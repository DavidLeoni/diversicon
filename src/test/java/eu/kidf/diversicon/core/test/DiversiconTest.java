package eu.kidf.diversicon.core.test;

import static eu.kidf.diversicon.core.test.DivTester.*;
import static eu.kidf.diversicon.core.test.LmfBuilder.lmf;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import org.dom4j.DocumentException;
import org.hibernate.exception.GenericJDBCException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.Sense;
import de.tudarmstadt.ukp.lmf.model.enums.ELabelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.meta.SemanticLabel;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import eu.kidf.diversicon.core.DbInfo;
import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.ImportConfig;
import eu.kidf.diversicon.core.ImportJob;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivIoException;
import eu.kidf.diversicon.core.exceptions.DivNotFoundException;
import eu.kidf.diversicon.core.exceptions.InterruptedImportException;
import eu.kidf.diversicon.core.exceptions.InvalidImportException;
import eu.kidf.diversicon.core.exceptions.InvalidSchemaException;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.data.DivWn31;
import eu.kidf.diversicon.data.Smartphones;

/**
 * @since 0.1.0
 *
 */
public class DiversiconTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiversiconTest.class);

    /**
     * @since 0.1.0
     */
    private static final LexicalResource WORDFORMS_LEX_RES = lmf()
            .lexicon()
            .synset()
            .lexicalEntry("a")
            .wordform("x")
            .lexicalEntry("b")
            .wordform("y")                                 
            .build();

    
    private DivConfig divConfig;
    
    @Before
    public void beforeMethod() {
        divConfig = createNewDivConfig();
    }

    @After
    public void afterMethod() {
        divConfig = null;
    }

    /**
     * Tests db tables are not automatically created.
     * 
     * @since 0.1.0
     */
    @Test
    public void testConnectToDbDontAutoCreate() {
        
        try {
            Diversicon uby = Diversicon.connectToDb(divConfig);
            Assert.fail("Shouldn't be able to connect to non-existing db!");
        } catch (InvalidSchemaException ex){
            
        }
    }

    /**
     * Checks our extended model of uby with is actually returned by Hibernate
     * 
     * @since 0.1.0
     */
    @Test
    public void testHibernateExtraAttributes() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        String lexResName = "lexical-resource-1";
        String lexiconName = "lexicon-1";
        
        LexicalResource lexRes = new LexicalResource();
        lexRes.setName(lexResName);
        Lexicon lexicon = new Lexicon();
        lexRes.addLexicon(lexicon);
        lexicon.setId(lexiconName);
        LexicalEntry lexicalEntry = new LexicalEntry();
        lexicon.addLexicalEntry(lexicalEntry);
        lexicalEntry.setId("lexical-entry-1");
        Synset synset = new Synset();
        lexicon.getSynsets()
               .add(synset);
        synset.setId("synset-1");
        DivSynsetRelation synsetRelation = new DivSynsetRelation();
        synsetRelation.setRelType(ERelTypeSemantics.taxonomic);
        synsetRelation.setRelName(ERelNameSemantics.HYPERNYM);
        synsetRelation.setDepth(3);
        synsetRelation.setProvenance("a");
        synsetRelation.setSource(synset);
        synsetRelation.setTarget(synset);
        synset.getSynsetRelations()
              .add(synsetRelation);

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, lexRes, true);

        assertNotNull(div.getLexicalResource(lexResName));
        assertEquals(2, div.getLexicons()
                                  .size());

        Lexicon rlexicon = div.getLexiconById(lexiconName);

        List<Synset> rsynsets = rlexicon.getSynsets();

        assertEquals(1, rsynsets.size());

        List<SynsetRelation> synRels = rsynsets.get(0)
                                               .getSynsetRelations();
        assertEquals(1, synRels.size());
        SynsetRelation rel = synRels.get(0);
        assertNotNull(rel);

        LOG.info("Asserting rel is instance of " + DivSynsetRelation.class);
        if (!(rel instanceof DivSynsetRelation)) {
            throw new RuntimeException(
                    "relation is not of type " + DivSynsetRelation.class + " found instead " + rel.getClass());
        }

        DivSynsetRelation WbRel = (DivSynsetRelation) rel;

        assertEquals(3, WbRel.getDepth());
        assertEquals("a", WbRel.getProvenance());

        div.getSession()
                  .close();
    };

    /**
     * Saves provided {@code lexicalResource} to database, normalizes and
     * augments database
     * with transitive closure, and tests database actually matches
     * {@code expectedLexicalResource}
     * 
     * @param lexicalResource
     * @param expectedLexicalResource
     * @since 0.1.0
     */
    public void assertAugmentation(
            LexicalResource lexicalResource,
            LexicalResource expectedLexicalResource) {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, lexicalResource, true);

        div.processGraph();

        checkDb(expectedLexicalResource, div, Internals.newHashSet(DivTester.Flags.UNORDERED_SYNSET_RELATIONS));

        div.getSession()
                  .close();

    };

    /**
     * @since 0.1.0
     */
    @Test
    public void testNormalizeNonCanonicalEdge() {

        assertAugmentation(

                lmf().lexicon()
                     .synset()
                     .lexicalEntry()
                     .synset()
                     .synsetRelation(ERelNameSemantics.HYPONYM, 1)
                     .build(),

                lmf().lexicon()
                     .synset()
                     .lexicalEntry()
                     .synset()
                     .synsetRelation(ERelNameSemantics.HYPONYM, 1)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 1, 2)
                     .build());
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testNormalizeCanonicalEdge() {
        assertAugmentation(GRAPH_1_HYPERNYM, GRAPH_1_HYPERNYM);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testNormalizeUnknownEdge() {

        LexicalResource lexicalResource = lmf().lexicon()
                                               .synset()
                                               .lexicalEntry()
                                               .synset()
                                               .synsetRelation("hello", 1)
                                               .build();

        assertAugmentation(lexicalResource, lexicalResource);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testTransitiveClosureDepth_2() {

        assertAugmentation(GRAPH_3_HYPERNYM,
                DAG_3_HYPERNYM);

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testTransitiveClosureDepth_3() {

        assertAugmentation(lmf().lexicon()
                                .synset()
                                .lexicalEntry()
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 3)

                                .build(),

                lmf().lexicon()
                     .synset()
                     .lexicalEntry()
                     .synset()
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                     .synset()                                          
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 1)                     
                     .depth(2)
                     .synset()                     
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 3)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                     .depth(2)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                     .depth(3)
                     .build());
    }

    /**
     * @since 0.1.0
     * 
     * Could still have problems, see https://github.com/diversicon-kb/diversicon/issues/14
     */
    @Test
    public void testTransitiveClosureNoDuplicates() {
        assertNoAugmentation(DAG_3_HYPERNYM);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testTransitiveClosureIgnoreNonCanonical() {

        assertNoAugmentation(lmf().lexicon()
                                  .synset()
                                  .lexicalEntry()
                                  .synset()
                                  .synsetRelation("a", 1)
                                  .synset()
                                  .synsetRelation("a", 2)
                                  .build());
    }

    /**
     * Asserts the provided lexical resource doesn't provoke any augmentation in
     * the database.
     * 
     * @since 0.1.0
     */
    private void assertNoAugmentation(LexicalResource lr) {
        assertAugmentation(lr, lr);
    }

    /**
     * Checks sequence indicated by provided iterator contains all the
     * synsets of given ids.
     * 
     * @throws DivNotFoundException
     */
    private static void checkContainsAll(Iterator<Synset> iter, String firstId, String... ids) {
        
               
        Set<String> synsetIds = new HashSet<>();
        while (iter.hasNext()) {
            synsetIds.add(iter.next()
                              .getId());
        }

        HashSet<String> setIds = new HashSet<>();
        setIds.add(firstId);
        for (String id : ids) {
            setIds.add(id);
        }

        assertEquals(setIds, synsetIds);
    }

    
    /**
     * Test on simple graph
     * 
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsets_Graph_1_Hypernym() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, GRAPH_1_HYPERNYM, true);

        assertFalse(div.getConnectedSynsets(
                tid("synset-2"),
                0,
                ERelNameSemantics.HYPERNYM)
                              .hasNext());

        assertFalse(div.getConnectedSynsets(
                tid("synset-2"),
                -1)
                              .hasNext());

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-2"),
                1,
                ERelNameSemantics.HYPERNYM),
                tid("synset-1"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-2"),
                -1,
                ERelNameSemantics.HYPERNYM),
                tid("synset-1"));

        assertFalse(div.getConnectedSynsets(
                tid("synset-2"),
                1,
                "hello").hasNext());

        div.getSession()
                  .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testIsConnected() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, DAG_3_HYPERNYM, true);

        assertTrue(div.isConnected(
                tid("synset-1"),
                tid("synset-1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                tid("synset-1"),
                tid("synset-1"),
                0, new ArrayList<String>()));

        assertFalse(div.isConnected(
                tid("synset-2"),
                tid("synset-1"),
                0,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                tid("synset-2"),
                tid("synset-1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                tid("synset-2"),
                tid("synset-1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                tid("synset-1"),
                tid("synset-3"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                tid("synset-3"),
                tid("synset-1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                tid("synset-1"),
                tid("synset-2"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                tid("synset-1"),
                tid("synset-1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        try {
            div.isConnected(
                    tid("synset-1"),
                    tid("synset-1"),
                    -2,
                    Arrays.asList(ERelNameSemantics.HYPERNYM));
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    "",
                    tid("synset-1"),
                    -1,
                    new ArrayList<String>());
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    tid("synset-1"),
                    tid(""),
                    -1,
                    new ArrayList<String>());
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        div.getSession()
           .close();

    }

    /**
     * @since 0.1.0
     */
    // todo improve, not so clear how lemmas work.
    @Test
    public void testGetLemmaByWrittenForm() {
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        LexicalResource res = lmf().lexicon()
                                   .synset()
                                   .lexicalEntry("a")
                                   .lexicalEntry("ab")
                                   .synset()
                                   .lexicalEntry("c")
                                   .build();

        DivTester.importResource(div, res, true);

        assertEquals(Internals.newArrayList("a"), div.getLemmaStringsByWrittenForm("a", null, null));
        assertEquals(Internals.newArrayList("c"), div.getLemmaStringsByWrittenForm("c", null, null));
        assertEquals(Internals.newArrayList(), div.getLemmaStringsByWrittenForm("666", null, null));

        div.getSession()
           .close();
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsets_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, DAG_3_HYPERNYM, true);

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-2"),
                1,
                ERelNameSemantics.HYPERNYM),
                tid("synset-1"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-1"),
                1,
                ERelNameSemantics.HYPONYM),
                tid("synset-2"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-3"),
                1,
                ERelNameSemantics.HYPERNYM),
                tid("synset-2"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-2"),
                1,
                ERelNameSemantics.HYPONYM),
                tid("synset-3"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-3"),
                2,
                ERelNameSemantics.HYPERNYM),
                tid("synset-1"), tid("synset-2"));

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-1"),
                2,
                ERelNameSemantics.HYPONYM),
                tid("synset-2"), tid("synset-3"));

        div.getSession()
           .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsetsMultiRelNames() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);
        DivTester.importResource(div, GRAPH_4_HYP_HOL_HELLO, true);
        checkContainsAll(div.getConnectedSynsets(
                tid("synset-4"),
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                tid("synset-1"),
                tid("synset-2"));

        div.getSession()
                  .close();
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsetsNoDups() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, DAG_2_MULTI_REL, true);

        checkContainsAll(div.getConnectedSynsets(
                tid("synset-2"),
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                tid("synset-1"));

        div.getSession()
                  .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetDbInfo() {
        
        LexicalResource g = GRAPH_4_HYP_HOL_HELLO;

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DbInfo dbInfo1 = div.getDbInfo();

        assertNotNull(dbInfo1);
        assertEquals(1, div.getImportJobs()
                           .size());
        assertEquals(null, dbInfo1.getCurrentImportJob());

        assertEquals(Diversicons.SCHEMA_VERSION_1_0, dbInfo1.getSchemaVersion());

        DivTester.importResource(div,g, true);

        assertEquals(2, div.getImportJobs()
                           .size());
        
        assertEquals(null, div.getDbInfo()
                              .getCurrentImportJob());

        ImportJob job = div.getImportJobs()
                           .get(1);
        assertEquals(Diversicons.DEFAULT_AUTHOR, job.getAuthor());

        // assertNotEquals(-1, job.getId());
        assertNotNull(job.getStartDate());
        assertNotNull(job.getEndDate());
        assertTrue(job.getEndDate()
                      .getTime() >= job.getStartDate()
                                       .getTime());
        assertEquals(g.getName(), job.getLexResPackage().getName());
        assertEquals(g.getGlobalInformation().getLabel(), job.getLexResPackage().getLabel());
        assertTrue(job.getFileUrl()
                      .startsWith(Diversicons.MEMORY_PROTOCOL + ":"));
        /* todo why the hell is commented?
         * LexicalResource res2 = lmf().lexicon()
         * .synset()
         * .synset()
         * .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
         * .build();
         * res2.setName("lexical-resource-1");
         * 
         * div.importResource(res2, true);
         * assertEquals(2, div.getImportJobs()
         * .size());
         * ImportJob job2 = div.getImportJobs()
         * .get(1);
         * assertNotEquals(-1, job2.getId());
         */
    }

    /**
     * Shows we don't allow self loops for canonical transitive relations
     * 
     * @since 0.1.0
     * @see #testSelfLoopNonCanonical()
     */    
    @Test
    public void testSelfLoopCanonical(){
        LexicalResource lr = lmf().lexicon()
             .synset()           
             .lexicalEntry()
             .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
             .build();
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);
        
        try {
            DivTester.importResource(div, lr, false);
            Assert.fail("Shouldn't arrive here!");
        } catch (InterruptedImportException ex){
            
        }
    }

    /**
     * Shows we allow self-loops for non canonical transitive relations,
     * because they won't be considered by Diversicon algorithms
     * 
     * @since 0.1.0
     * @see #testSelfLoopCanonical()
     */
    @Test
    public void testSelfLoopNonCanonical(){        
        LexicalResource lr = lmf().lexicon()
             .synset()      
             .lexicalEntry()
             .synsetRelation(ERelNameSemantics.HYPONYM, 1)
             .build();
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);
                
        DivTester.importResource(div, lr, false);
        
        DivTester.checkDb(lr, div);
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testGetNamespaces(){
                
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);       
        
        ImportJob job = DivTester.importResource(div, GRAPH_1_HYPERNYM, true);        
        LexResPackage pack1 = job.getLexResPackage();        

        assertEquals(DEFAULT_TEST_PREFIX, pack1.getPrefix());
        
        String nsUrl1 = pack1.getNamespaces().get(pack1.getPrefix()); 
        Internals.checkNotBlank(nsUrl1, "Invalid url!");
        
        assertEquals(nsUrl1, div.getNamespaces().get(DEFAULT_TEST_PREFIX));

        String prefix2 = "test2";
        
        /**
         * 2 verteces and 1 hypernym edge
         */
        LexicalResource lexRes2 = lmf(prefix2).lexicon()
                                              .synset()
                                              .lexicalEntry()
                                              .synset()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                              .build();
        
        ImportJob job2 = div.importResource(lexRes2,
                createLexResPackage(lexRes2, prefix2), 
                true);

        assertEquals(5, div.getNamespaces().size());
        
        LexResPackage pack2 = job2.getLexResPackage();
        assertEquals(prefix2, pack2.getPrefix());
        String nsUrl2 = pack2.getNamespaces().get(pack2.getPrefix());
        assertEquals(nsUrl2, div.getNamespaces().get(pack2.getPrefix()));
        
    }
    
    /**
     * Shows we can calculate transitive closure using relations
     * pointing to missing synsets
     *  
     * @since 0.1.0
     */
    @Test
    public void testTransitiveClosureMissingSynsets(){
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);               
        String upp = "upp";
        
        /**
         * 2 verteces and 1 hypernym edge, plus one hypernym to upper ontology synset
         */
        LexicalResource lexRes = lmf().lexicon()
                                              .synset()
                                              .lexicalEntry()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, upp +":ss-1")
                                              .synset()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                              .build();
        LexResPackage pack = DivTester.createLexResPackage(lexRes);
        pack.putNamespace(upp,
                           "http://upp");
               
        div.importResource( lexRes, pack, false);
        assertEquals(3, div.getSynsetRelationsCount());                                       
        
    }
    
    /**
     * 
     * Imports two resources. Second one assigns to the first prefix a different url.
     * 
     * Expected result: exception is thrown.
     * 
     * @since 0.1.0
     * 
     */
    @Test
    public void testImportWithClashingDependencies() throws IOException {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);       
        
        ImportJob job1 = DivTester.importResource(div, GRAPH_1_HYPERNYM, true);
        job1.getLexResPackage();

        String prefix2 = "test2";
        /**
         * 2 verteces and 1 hypernym edge
         */
        LexicalResource lexRes2 = lmf(prefix2).lexicon()
                                              .synset()
                                              .lexicalEntry()
                                              .synset()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                              .build();
        LexResPackage pack2 = DivTester.createLexResPackage(lexRes2, prefix2);
        pack2.putNamespace(DEFAULT_TEST_PREFIX,
                           "http://666");
        
        try {
            div.importResource( lexRes2, pack2, true);
                                   
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidImportException ex){
            LOG.debug("Caught exception:", ex);
        }        
    }
    
    /**
     * Was giving absurd problems for null discriminator in
     * DivSynsetRelation.hbm.xml 
     * 
     * @since 0.1.0
     * 
     */
    @Test
    public void testImportXml() throws IOException {

        File xml = DivTester.writeXml(GRAPH_4_HYP_HOL_HELLO);
        
        LOG.debug("\n" + FileUtils.readFileToString(xml, "UTF-8"));
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        assertFalse(div.formatImportJobs(true)
                       .isEmpty());
        
        div.importXml(xml.getAbsolutePath());
        
        checkDb(GRAPH_4_HYP_HOL_HELLO, div);                
        
        div.getSession().close();

    }

    /**
     * We should be able to import Smartphones and compute transitive closure 
     * even without Wordnet loaded. 
     * 
     * @see DivUtilsIT#testImportSmartPhonesXmlWithWordnet()
     * 
     * @since 0.1.0
     * 
     */
    @Test
    public void testImportSmartPhonesXmlWithoutWordnet() {        
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);
        
        ImportJob job = div.importXml(Smartphones.of().getXmlUri());        
                
        LexicalResource lr = div.getLexicalResource(Smartphones.NAME);
        
        job.getLexResPackage().getNamespaces().get(Smartphones.PREFIX).equals(
                Smartphones.of().namespace());
        job.getLexResPackage().getNamespaces().get(DivWn31.PREFIX).equals(DivWn31.of().namespace());
        
        List<MetaData> metadatas = lr.getMetaData();
        assertEquals(1, metadatas.size());
        MetaData metadata = metadatas.get(0);
        assertNotNull(metadata);
        assertEquals("sm_md", metadata.getId());
        
        assertEquals(5 + 2 + 2 + 2, div.getSynsetRelationsCount());
        
    }    
    
    /** 
     * @since 0.1.0
     */
    @Test
    public void testExportToXml() throws IOException {
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);
        
        DivTester.importResource(div, GRAPH_1_HYPERNYM, false);
        
        File dir = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING).toFile();
        
        File xml = new File(dir, "test.xml");
        
        div.exportToXml(xml.toString(), 
                        GRAPH_1_HYPERNYM.getName(),
                        false);
        
        String str = FileUtils.readFileToString(xml, "UTF-8");
        assertTrue(!str.contains("DivSynsetRelation"));        
    }
   
    
    /**
     * todo...
     * @since 0.1.0
     */
    @Test
    @Ignore
    public void testTransformWithNamespaces() throws IOException, DocumentException {
               
        Assert.fail();
        
/**     Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        div.importResource(GRAPH_1_HYPERNYM, false);
        
        File dir = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING).toFile();
        
        File xml = new File(dir, "test.xml");
        
        div.exportToXml(xml.toString(), 
                        GRAPH_1_HYPERNYM.getName(),
                        false);
        
        String str = FileUtils.readFileToString(xml, "UTF-8");
        
        
        File xml = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM,
                Internals.newMap("prefix-1", "url-1",
                                 "prefix-2", "url-2"));
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        div.importResource(GRAPH_1_HYPERNYM, false);
        
        File dir = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING).toFile();
        
        File xml = new File(dir, "test.xml");
        
        div.exportToXml(xml.toString(), 
                        GRAPH_1_HYPERNYM.getName(),
                        false,
                        Internals.newMap("prefix-1", "url-1",
                                "prefix-2", "url-2"));
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(xml);
                
        String str = FileUtils.readFileToString(xml, "UTF-8");
        LOG.debug(str);
        
        assertEquals(1, document
                    .selectNodes("//LexicalResource[namespace::*[.='url-1'] "
                                 + " and namespace::*[.='url-2'] ]" )
                     .size());
        // using just string matching because can't select xmlns stuff: https://www.oxygenxml.com/forum/topic4845.html 
        assertTrue(str.contains("xmlns:prefix-1=\"url-1\""));
        assertTrue(str.contains("xmlns:prefix-2=\"url-2\""));
   **/             
    }
    
    
    /**
     * Watch out for long texts when importing
     * 
     * @since 0.1.0
     * 
     */
    @Test
    public void testFormatImports() {
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        assertFalse(div.formatImportJobs(true)
                       .isEmpty());

        File xml1 = DivTester.writeXml(GRAPH_1_HYPERNYM);
        String prefix2 = "test2";
        File xml2 = DivTester.writeXml(lmf(prefix2).lexicon()
                                                  .synset()
                                                  .lexicalEntry()
                                                  .synset()
                                                  .synset()
                                                  .synset()
                                                  .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                  .synsetRelation(ERelNameSemantics.HOLONYM, 2)
                                                  .synsetRelation("hello", 3)
                                                  .build(),
                                         prefix2);

        ImportConfig config = new ImportConfig();

        config.addLexicalResource(xml1.getAbsolutePath());
        config.addLexicalResource(xml2.getAbsolutePath());

        config.setAuthor("Someone With A Very Long Name");
        config.setDescription(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        config.setSkipAugment(true);

        ImportJob job1 = div.importFiles(config)
                            .get(0);

        String output1 = div.formatImportJob(job1, false);

        LOG.debug("\n\n" + output1);

        assertTrue(output1
                          .contains(Long.toString(job1.getId())));

        assertTrue(output1
                          .contains(GRAPH_1_HYPERNYM.getName()));

        String output2 = div.formatImportJobs(false);

        LOG.debug("\n\n-----------------\n\n" + output2);

        assertTrue(output2.contains(Long.toString(job1.getId())));
        assertTrue(output2.contains(GRAPH_1_HYPERNYM.getName()));
        assertTrue(output2.contains(prefix2));

    }

    /**
     * Test for https://github.com/diversicon-kb/diversicon/issues/8
     * See also {@link UbyTest#testCantMergeSameLexicon()}
     * 
     * @since 0.1.0
     */
    @Test
    public void testImportCantMergeSameLexicon() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        DivTester.importResource(div, GRAPH_1_HYPERNYM, true);

        try {
            DivTester.importResource(div, GRAPH_1_HYPERNYM, true);
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidImportException ex){
            
        } finally {
            div.getSession()
            .close();    
        }


        
    }


    /**
     * @since 0.1.0
     */
    @Test
    public void testImportTwoConnectedLexicalResources() {

        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);       


        DivTester.importResource(div, GRAPH_1_HYPERNYM, false);

        String prefix2 = "test2";
        
        /**
         * 2 verteces and 1 hypernym edge
         */
        LexicalResource lexRes2 = lmf(prefix2).lexicon()
                                              .synset()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, "test_synset-2")                                              
                                              .lexicalEntry()
                                              .build();
        LexResPackage pack2 = DivTester.createLexResPackage(lexRes2, prefix2);
        div.importResource( lexRes2, pack2, false);

        DivTester.checkDb(GRAPH_1_HYPERNYM, div);        

        assertEquals(3, div.getImportJobs()
                           .size());

        ImportJob import1 = div.getImportJobs()
                               .get(1);
        ImportJob import2 = div.getImportJobs()
                               .get(2);

        assertEquals("test lexical resource", import1.getLexResPackage().getLabel());
        assertNotEquals(-1, import1.getId());

        assertEquals(prefix2 + " lexical resource", import2.getLexResPackage().getLabel());
        assertNotEquals(-1, import2.getId());
                        
        List<Synset> synsets = Internals.newArrayList(div.getConnectedSynsets("test2_synset-1", -1, ERelNameSemantics.HYPERNYM));
        
        assertEquals(2, synsets.size());
        assertEquals("test_synset-2", synsets.get(0).getId());
        assertEquals("test_synset-1", synsets.get(1).getId());
        
        div.getSession()
           .close();

    }

    
    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlRestore() throws IOException {
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        
        Diversicon div = Diversicon.connectToDb(divConfig);
        DivTester.importResource(div, GRAPH_1_HYPERNYM, true);
        
        Path dir = DivTester.createTestDir();
        File zip = new File(dir.toString() + "/output.sql.zip");
        div.exportToSql(zip.getAbsolutePath(), true);
        
        assertTrue(zip.exists());
        assertTrue(zip.length() > 0);
        
        DivConfig divConfig2 = DivTester.createNewDivConfig();
        //Diversicons.dropCreateTables(dbConfig2);
        Diversicons.h2RestoreSql("file://" + zip.getAbsolutePath(), divConfig2);
        Diversicon div2 = Diversicon.connectToDb(divConfig2);
        checkDb(GRAPH_1_HYPERNYM, div2);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlToAlreadyExistingFile() throws IOException{
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);
        DivTester.importResource(div, GRAPH_1_HYPERNYM, true);
        
        Path dir = DivTester.createTestDir();
        File zip = new File(dir.toString() + "/output.sql.zip");
        div.exportToSql(zip.getAbsolutePath(), true);
        
        try {
            div.exportToSql(zip.getAbsolutePath(), true);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivIoException ex){
            
        }        
        
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlWrongFile() throws IOException{
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);
        DivTester.importResource(div, GRAPH_1_HYPERNYM, true);
        
        Path dir = DivTester.createTestDir();
        File zip = new File(dir.toString() + "/output.sql.zip");
        
        try {
            div.exportToSql("", true);
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex){
            
        }
        
        try {
            div.exportToSql(" ", true);
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex){
            
        }                

        
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlInNonExistingDir() throws IOException{
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);
        importResource(div, GRAPH_1_HYPERNYM, true);
        
        Path dir = DivTester.createTestDir();
        File zip = new File(dir.toString() + "123/output.sql.zip");
        
        
        div.exportToSql(zip.getAbsolutePath(), true);
        
        File newDir = new File(dir.toString() + "123");
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
        assertTrue(zip.exists());
    }

    
    /**
     * This is currently unsupported in H2 1.3.x we're using
     * 
     * See https://github.com/diversicon-kb/diversicon/issues/11
     * 
     * @since 0.1.0
     */
    @Test  
    public void testConnectToDbH2InMemoryCompressed(){
        try {
            DBConfig dbConfig = Diversicons.h2MakeDefaultInMemoryDbConfig("trial-" + UUID.randomUUID(), true);
            
            
            Assert.fail("Shouldn't arrive here!");

            Diversicons.dropCreateTables(dbConfig);
            Diversicon div = Diversicon.connectToDb(DivConfig.of(dbConfig));
            int mem = div.memoryUsed();
            LOG.debug("Memory used for empty H2 compressed in-memory db is " + Internals.humanByteCount(mem * 1024));
            
            div.getSession().close();
            
        }
        catch (UnsupportedOperationException ex){
            
        }
        
    }

    /**
     * @since 0.1.0
     */      
    @Test
    public void testMemoryUsed(){
        DBConfig dbConfig = Diversicons.h2MakeDefaultInMemoryDbConfig("trial-" + UUID.randomUUID()
                                    , false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(DivConfig.of(dbConfig));
        int mem = div.memoryUsed();
        LOG.debug("Memory used for empty uncompressed H2 in-memory db is " + Internals.humanByteCount(mem * 1024));
        div.getSession().close();
    }
    
    /**
     * Attempts to connect with wrong password.
     * 
     * Test for https://github.com/diversicon-kb/diversicon/issues/13
     * 
     * @since 0.1.0
     */
    @Test
    public void testConnectToDbTimeout(){
        
        Date start = new Date();
        
        DBConfig dbConfig = Diversicons.h2MakeDefaultInMemoryDbConfig("trial-" + UUID.randomUUID()
        , false);
        Diversicons.dropCreateTables(dbConfig);
        dbConfig.setPassword("666");
        
        try {
            Diversicon div = Diversicon.connectToDb(DivConfig.of(dbConfig));
            Assert.fail("Shouldn't arrive here!");
        } catch (GenericJDBCException ex){
                                   
            Date end = new Date();
            LOG.info("Elapsed time: " + Internals.formatInterval(start, end));
            
            long delay = end.getTime() - start.getTime();
            assertTrue( "Exceeded max delay of " 
                    + MAX_TEST_DELAY + "ms for connection problems. Got " + delay + "ms instead." , delay < MAX_TEST_DELAY);
                                                  
        }
        
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetRelationsCount(){
        Diversicons.dropCreateTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);       

        DivTester.importResource(div, GRAPH_1_HYPERNYM, false);

        assertEquals(1, div.getSynsetRelationsCount());

        div.getSession()
           .close();        
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testGetLexicalEntriesByWordForm(){
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);        
        DivTester.importResource(div, WORDFORMS_LEX_RES, true);
        
        List<LexicalEntry> les = div.getLexicalEntriesByWordForm("x", null, null);
        
        assertEquals(1, les.size());
        assertEquals(les.get(0).getLemmaForm(), "a");        

    }
    

    
    /**
     * @since 0.1.0
     */
    @Test
    public void testGetLemmaByWordForm(){
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);        
        DivTester.importResource(div, WORDFORMS_LEX_RES, true);
        
        assertEquals(Internals.newArrayList("a"),
                    div.getLemmaStringsByWordForm("x", null, null));

    }    

    
    /**
     * @since 0.1.0
     */
    @Test
    public void testGetDomains(){
        
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);                
        DivTester.importResource(div, DivTester.GRAPH_DOMAINS, false);

        Synset syn2 = div.getSynsetById(tid("synset-2"));
        Synset syn4 = div.getSynsetById(tid("synset-4"));
        List<String> ids = Internals.getIds(div.getDomains(null));
        assertEquals(2, ids.size());
        assertTrue(ids.contains(syn2.getId()));
        assertTrue(ids.contains(syn4.getId()));
    }
    
    
    @Test
    public void testIsDomain(){
        Diversicons.dropCreateTables(divConfig.getDbConfig());
        Diversicon div = Diversicon.connectToDb(divConfig);                
        DivTester.importResource(div, DivTester.GRAPH_DOMAINS, false);
        
        
        Synset syn1 = div.getSynsetById(tid("synset-1"));        
        assertFalse(div.isDomain(syn1));
        Synset syn2 = div.getSynsetById(tid("synset-2"));        
        assertTrue(div.isDomain(syn2));
        Synset syn4 = div.getSynsetById(tid("synset-4"));        
        assertTrue(div.isDomain(syn4));
    }
    
    
}
