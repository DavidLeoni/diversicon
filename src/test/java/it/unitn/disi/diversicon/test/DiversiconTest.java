package it.unitn.disi.diversicon.test;

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
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.disi.unitn.diversicon.exceptions.DivIoException;
import it.disi.unitn.diversicon.exceptions.DivNotFoundException;
import it.unitn.disi.diversicon.DbInfo;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.DivValidationException;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.ImportConfig;
import it.unitn.disi.diversicon.ImportJob;
import it.unitn.disi.diversicon.InvalidSchemaException;
import it.unitn.disi.diversicon.data.Smartphones;
import it.unitn.disi.diversicon.internal.Internals;

import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static it.unitn.disi.diversicon.test.DivTester.*;

public class DiversiconTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiversiconTest.class);

    private DBConfig dbConfig;
    
    @Before
    public void beforeMethod() {
        dbConfig = createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }

    /**
     * Tests db tables are not automatically created.
     * 
     * @since 0.1.0
     */
    @Test
    public void testDontAutoCreate() {
        
        try {
            Diversicon uby = Diversicon.connectToDb(dbConfig);
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

        Diversicons.dropCreateTables(dbConfig);

        LexicalResource lexicalResource = new LexicalResource();
        lexicalResource.setName("lexicalResource 1");
        Lexicon lexicon = new Lexicon();
        lexicalResource.addLexicon(lexicon);
        lexicon.setId("lexicon 1");
        LexicalEntry lexicalEntry = new LexicalEntry();
        lexicon.addLexicalEntry(lexicalEntry);
        lexicalEntry.setId("lexicalEntry 1");
        Synset synset = new Synset();
        lexicon.getSynsets()
               .add(synset);
        synset.setId("synset 1");
        DivSynsetRelation synsetRelation = new DivSynsetRelation();
        synsetRelation.setRelType(ERelTypeSemantics.taxonomic);
        synsetRelation.setRelName(ERelNameSemantics.HYPERNYM);
        synsetRelation.setDepth(3);
        synsetRelation.setProvenance("a");
        synsetRelation.setSource(synset);
        synsetRelation.setTarget(synset);
        synset.getSynsetRelations()
              .add(synsetRelation);

        Diversicon diversicon = Diversicon.connectToDb(dbConfig);

        diversicon.importResource(lexicalResource, true);

        assertNotNull(diversicon.getLexicalResource("lexicalResource 1"));
        assertEquals(1, diversicon.getLexicons()
                                  .size());

        Lexicon rlexicon = diversicon.getLexicons()
                                     .get(0);

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

        diversicon.getSession()
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

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.connectToDb(dbConfig);

        diversicon.importResource(lexicalResource, true);

        diversicon.processGraph();

        checkDb(expectedLexicalResource, diversicon, Internals.newHashSet(DivTester.Flags.UNORDERED_SYNSET_RELATIONS));

        diversicon.getSession()
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
                     .synset()
                     .synsetRelation(ERelNameSemantics.HYPONYM, 1)
                     .build(),

                lmf().lexicon()
                     .synset()
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

        assertAugmentation(lmf().lexicon()
                                .synset()
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                .build(),
                DAG_3_HYPERNYM);

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testTransitiveClosureDepth_3() {

        assertAugmentation(lmf().lexicon()
                                .synset()
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 3)

                                .build(),

                lmf().lexicon()
                     .synset()
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
     * Could still have problems, see https://github.com/DavidLeoni/diversicon/issues/14
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
     * @since 0.1.0
     */
    private static String id(String name){        
        return LmfBuilder.DEFAULT_PREFIX + ":" + name;
    }
    /**
     * Test on simple graph
     * 
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsets_Graph_1_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.connectToDb(dbConfig);

        diversicon.importResource(GRAPH_1_HYPERNYM, true);

        assertFalse(diversicon.getConnectedSynsets(
                id("synset 2"),
                0,
                ERelNameSemantics.HYPERNYM)
                              .hasNext());

        assertFalse(diversicon.getConnectedSynsets(
                id("synset 2"),
                -1)
                              .hasNext());

        checkContainsAll(diversicon.getConnectedSynsets(
                id("synset 2"),
                1,
                ERelNameSemantics.HYPERNYM),
                id("synset 1"));

        checkContainsAll(diversicon.getConnectedSynsets(
                id("synset 2"),
                -1,
                ERelNameSemantics.HYPERNYM),
                id("synset 1"));

        assertFalse(diversicon.getConnectedSynsets(
                id("synset 2"),
                1,
                "hello").hasNext());

        diversicon.getSession()
                  .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testIsConnected() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        div.importResource(DAG_3_HYPERNYM, true);

        assertTrue(div.isConnected(
                id("synset 1"),
                id("synset 1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                id("synset 1"),
                id("synset 1"),
                0, new ArrayList<String>()));

        assertFalse(div.isConnected(
                id("synset 2"),
                id("synset 1"),
                0,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                id("synset 2"),
                id("synset 1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                id("synset 2"),
                id("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                id("synset 1"),
                id("synset 3"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                id("synset 3"),
                id("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                id("synset 1"),
                id("synset 2"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                id("synset 1"),
                id("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        try {
            div.isConnected(
                    id("synset 1"),
                    id("synset 1"),
                    -2,
                    Arrays.asList(ERelNameSemantics.HYPERNYM));
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    "",
                    id("synset 1"),
                    -1,
                    new ArrayList());
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    id("synset 1"),
                    id(""),
                    -1,
                    new ArrayList());
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
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        LexicalResource res = lmf().lexicon()
                                   .synset()
                                   .lexicalEntry("a")
                                   .lexicalEntry("ab")
                                   .synset()
                                   .lexicalEntry("c")
                                   .build();

        div.importResource(res, true);

        assertEquals(Internals.newArrayList("a"), div.getLemmaStringsByWrittenForm("a"));
        assertEquals(Internals.newArrayList("c"), div.getLemmaStringsByWrittenForm("c"));
        assertEquals(Internals.newArrayList(), div.getLemmaStringsByWrittenForm("666"));

        div.getSession()
           .close();
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsets_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        div.importResource(DAG_3_HYPERNYM, true);

        checkContainsAll(div.getConnectedSynsets(
                id("synset 2"),
                1,
                ERelNameSemantics.HYPERNYM),
                id("synset 1"));

        checkContainsAll(div.getConnectedSynsets(
                id("synset 1"),
                1,
                ERelNameSemantics.HYPONYM),
                id("synset 2"));

        checkContainsAll(div.getConnectedSynsets(
                id("synset 3"),
                1,
                ERelNameSemantics.HYPERNYM),
                id("synset 2"));

        checkContainsAll(div.getConnectedSynsets(
                id("synset 2"),
                1,
                ERelNameSemantics.HYPONYM),
                id("synset 3"));

        checkContainsAll(div.getConnectedSynsets(
                id("synset 3"),
                2,
                ERelNameSemantics.HYPERNYM),
                id("synset 1"), id("synset 2"));

        checkContainsAll(div.getConnectedSynsets(
                id("synset 1"),
                2,
                ERelNameSemantics.HYPONYM),
                id("synset 2"), id("synset 3"));

        div.getSession()
           .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsetsMultiRelNames() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.connectToDb(dbConfig);
        diversicon.importResource(GRAPH_4_HYP_HOL_HELLO, true);
        checkContainsAll(diversicon.getConnectedSynsets(
                id("synset 4"),
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                id("synset 1"),
                id("synset 2"));

        diversicon.getSession()
                  .close();
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetConnectedSynsetsNoDups() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.connectToDb(dbConfig);

        diversicon.importResource(DAG_2_MULTI_REL, true);

        checkContainsAll(diversicon.getConnectedSynsets(
                id("synset 2"),
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                id("synset 1"));

        diversicon.getSession()
                  .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetDbInfo() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        DbInfo dbInfo1 = div.getDbInfo();

        assertNotNull(dbInfo1);
        assertEquals(0, div.getImportJobs()
                           .size());
        assertEquals(null, dbInfo1.getCurrentImportJob());

        assertEquals(Diversicon.DIVERSICON_SCHEMA_VERSION, dbInfo1.getSchemaVersion());

        div.importResource(GRAPH_4_HYP_HOL_HELLO, true);

        assertEquals(1, div.getImportJobs()
                           .size());
        assertEquals(null, div.getDbInfo()
                              .getCurrentImportJob());

        ImportJob job = div.getImportJobs()
                           .get(0);
        assertEquals(Diversicon.DEFAULT_AUTHOR, job.getAuthor());

        // assertNotEquals(-1, job.getId());
        assertNotNull(job.getStartDate());
        assertNotNull(job.getEndDate());
        assertTrue(job.getEndDate()
                      .getTime() >= job.getStartDate()
                                       .getTime());
        assertEquals("lexical resource 1", job.getResourceDescriptor().getName());
        assertTrue(job.getFileUrl()
                      .startsWith(Diversicon.MEMORY_PROTOCOL + ":"));
        /*
         * LexicalResource res2 = lmf().lexicon()
         * .synset()
         * .synset()
         * .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
         * .build();
         * res2.setName("lexical resource 1");
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
             .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
             .build();
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        try {
            div.importResource(lr, false);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivValidationException ex){
            
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
             .synsetRelation(ERelNameSemantics.HYPONYM, 1)
             .build();
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
                
        div.importResource(lr, false);
        
        DivTester.checkDb(lr, div);
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
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

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
                
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        div.importXml(Smartphones.of().getXmlUri());        
                
        LexicalResource lr = div.getLexicalResourceById(Smartphones.ID);
        List<MetaData> metadatas = lr.getMetaData();
        assertEquals(1, metadatas.size());
        MetaData metadata = metadatas.get(0);
        assertNotNull(metadata);
        assertEquals("sm:md", metadata.getId());
        
    }    
    
    /** 
     * @since 0.1.0
     */
    @Test
    public void testExportToXml() throws IOException {
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        div.importResource(GRAPH_1_HYPERNYM, false);
        
        File dir = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING).toFile();
        
        File xml = new File(dir, "test.xml");
        
        div.exportToXml(xml.toString(), 
                        GRAPH_1_HYPERNYM.getName(),
                        false);
        
        String str = FileUtils.readFileToString(xml, "UTF-8");
        assertTrue(!str.contains("DivSynsetRelation"));        
    }
   
    
    /**
     * 
     * @since 0.1.0
     */
    @Test
    public void testTransformWithNamespaces() throws IOException, DocumentException {
               
        Assert.fail();
        
/**        Diversicons.dropCreateTables(dbConfig);

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
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        assertFalse(div.formatImportJobs(true)
                       .isEmpty());

        File xml1 = DivTester.writeXml(GRAPH_1_HYPERNYM);
        File xml2 = DivTester.writeXml(lmf("2nd-").lexicon()
                                                  .synset()
                                                  .synset()
                                                  .synset()
                                                  .synset()
                                                  .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                  .synsetRelation(ERelNameSemantics.HOLONYM, 2)
                                                  .synsetRelation("hello", 3)
                                                  .build());

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
        assertTrue(output2.contains("2nd"));

    }

    /**
     * Test for https://github.com/DavidLeoni/diversicon/issues/8
     * See also {@link UbyTest#testCantMergeSameLexicon()}
     * 
     * @since 0.1.0
     */
    @Test
    @Ignore
    public void testCantMergeSameLexicon() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        div.importResource(GRAPH_1_HYPERNYM, true);

        div.importResource(GRAPH_1_HYPERNYM, true);

        DivTester.checkDb(GRAPH_1_HYPERNYM, div);

        div.getSession()
           .close();
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testMergeTwoSeparateLexicalResources() {

        String prefix2 = "2nd-";

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);

        div.importResource(GRAPH_1_HYPERNYM, true);

        /**
         * 2 verteces and 1 hypernym edge
         */
        LexicalResource lexRes2 = lmf(prefix2).lexicon()
                                              .synset()
                                              .synset()
                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                              .build();

        div.importResource(lexRes2, true);

        DivTester.checkDb(GRAPH_1_HYPERNYM, div);
        DivTester.checkDb(lexRes2, div);

        assertEquals(2, div.getImportJobs()
                           .size());

        ImportJob import0 = div.getImportJobs()
                               .get(0);
        ImportJob import1 = div.getImportJobs()
                               .get(1);

        assertEquals("lexical resource 1", import0.getResourceDescriptor().getName());
        assertNotEquals(-1, import0.getId());

        assertEquals(prefix2 + "lexical resource 1", import1.getResourceDescriptor().getName());
        assertNotEquals(-1, import1.getId());

        div.getSession()
           .close();

    }
   
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlRestore() throws IOException {
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importResource(GRAPH_1_HYPERNYM, true);
        
        Path dir = DivTester.createTestDir();
        File zip = new File(dir.toString() + "/output.sql.zip");
        div.exportToSql(zip.getAbsolutePath(), true);
        
        assertTrue(zip.exists());
        assertTrue(zip.length() > 0);
        
        DBConfig dbConfig2 = DivTester.createNewDbConfig();
        //Diversicons.dropCreateTables(dbConfig2);
        Diversicons.restoreH2Sql("file://" + zip.getAbsolutePath(), dbConfig2);
        Diversicon div2 = Diversicon.connectToDb(dbConfig2);
        checkDb(GRAPH_1_HYPERNYM, div2);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testExportToSqlToAlreadyExistingFile() throws IOException{
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importResource(GRAPH_1_HYPERNYM, true);
        
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
        
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importResource(GRAPH_1_HYPERNYM, true);
        
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
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importResource(GRAPH_1_HYPERNYM, true);
        
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
     * See https://github.com/DavidLeoni/diversicon/issues/11
     * 
     * @since 0.1.0
     */
    @Test  
    public void testInMemoryCompressedH2(){
        try {
            DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("trial-" + UUID.randomUUID(), true);
            Assert.fail("Shouldn't arrive here!");

            Diversicons.dropCreateTables(dbConfig);
            Diversicon div = Diversicon.connectToDb(dbConfig);
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
        DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("trial-" + UUID.randomUUID()
                                    , false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        int mem = div.memoryUsed();
        LOG.debug("Memory used for empty uncompressed H2 in-memory db is " + Internals.humanByteCount(mem * 1024));
        div.getSession().close();
    }
    
    /**
     * Test for https://github.com/DavidLeoni/diversicon/issues/13
     * 
     * @since 0.1.0
     */
    @Test
    public void testConnectionTimeout(){
        
        Date start = new Date();
        
        DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("trial-" + UUID.randomUUID()
        , false);
        Diversicons.dropCreateTables(dbConfig);
        dbConfig.setPassword("666");
        
        try {
            Diversicon div = Diversicon.connectToDb(dbConfig);
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
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);       

        div.importResource(GRAPH_1_HYPERNYM, false);

        assertEquals(1, div.getSynsetRelationsCount());

        div.getSession()
           .close();        
    }
    
}
