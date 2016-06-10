package it.unitn.disi.diversicon.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DbInfo;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.ImportConfig;
import it.unitn.disi.diversicon.ImportJob;
import it.unitn.disi.diversicon.internal.Internals;

import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static it.unitn.disi.diversicon.test.DivTester.checkDb;
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
     * Tests db tables are automatically created.
     * 
     * @since 0.1
     */
    @Test
    public void testAutoCreate() {
        Diversicon uby = Diversicon.create(dbConfig);
        uby.getSession()
           .close();
    }

    /**
     * Checks our extended model of uby with is actually returned by Hibernate
     * 
     * @since 0.1
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

        Diversicon diversicon = Diversicon.create(dbConfig);

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
     * @since 0.1
     */
    public void assertAugmentation(
            LexicalResource lexicalResource,
            LexicalResource expectedLexicalResource) {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);

        diversicon.importResource(lexicalResource, true);

        diversicon.processGraph();

        checkDb(expectedLexicalResource, diversicon);

        diversicon.getSession()
                  .close();

    };

    /**
     * @since 0.1
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
     * @since 0.1
     */
    @Test
    public void testNormalizeCanonicalEdge() {
        assertAugmentation(GRAPH_1_HYPERNYM, GRAPH_1_HYPERNYM);
    }

    /**
     * @since 0.1
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
     * @since 0.1
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
     * @since 0.1
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
     * @since 0.1
     */
    @Test
    public void testTransitiveClosureNoDuplicates() {
        assertNoAugmentation(DAG_3_HYPERNYM);
    }

    /**
     * @since 0.1
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
     * @since 0.1
     */
    private void assertNoAugmentation(LexicalResource lr) {
        assertAugmentation(lr, lr);
    }

    /**
     * Checks sequence indicated by provided provided iterator contains all the
     * synsets of given ids.
     * 
     * @throws DivNotFoundException
     */
    private static void checkContainsAll(Iterator<Synset> iter, String... ids) {

        Set<String> synsetIds = new HashSet();
        while (iter.hasNext()) {
            synsetIds.add(iter.next()
                              .getId());
        }

        HashSet<String> listIds = new HashSet();
        for (String id : ids) {
            listIds.add(id);
        }

        assertEquals(listIds, synsetIds);
    }

    /**
     * Test on simple graph
     * 
     * @since 0.1
     */
    @Test
    public void testGetConnectedSynsets_Graph_1_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);

        diversicon.importResource(GRAPH_1_HYPERNYM, true);

        assertFalse(diversicon.getConnectedSynsets(
                "synset 2",
                0,
                ERelNameSemantics.HYPERNYM)
                              .hasNext());

        assertFalse(diversicon.getConnectedSynsets(
                "synset 2",
                -1)
                              .hasNext());

        checkContainsAll(diversicon.getConnectedSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(diversicon.getConnectedSynsets(
                "synset 2",
                -1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(diversicon.getConnectedSynsets(
                "synset 2",
                1,
                "hello"));

        diversicon.getSession()
                  .close();

    }

    /**
     * @since 0.1
     */
    @Test
    public void testIsReachable() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(DAG_3_HYPERNYM, true);

        assertTrue(div.isConnected(
                "synset 1",
                "synset 1",
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                "synset 1",
                "synset 1",
                0, new ArrayList()));

        assertFalse(div.isConnected(
                "synset 2",
                "synset 1",
                0,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                "synset 2",
                "synset 1",
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                "synset 2",
                "synset 1",
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                "synset 1",
                "synset 3",
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                "synset 3",
                "synset 1",
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));

        assertFalse(div.isConnected(
                "synset 1",
                "synset 2",
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isConnected(
                "synset 1",
                "synset 1",
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        try {
            div.isConnected(
                    "synset 1",
                    "synset 1",
                    -2,
                    Arrays.asList(ERelNameSemantics.HYPERNYM));
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    "",
                    "synset 1",
                    -1,
                    new ArrayList());
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        try {
            div.isConnected(
                    "synset 1",
                    "",
                    -1,
                    new ArrayList());
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex) {

        }

        div.getSession()
           .close();

    }

    // todo improve, not so clear how lemmas work.
    @Test
    public void testGetLemmaByWrittenForm() {
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

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
     * @since 0.1
     */
    @Test
    public void testGetConnectedSynsets_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(DAG_3_HYPERNYM, true);

        checkContainsAll(div.getConnectedSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(div.getConnectedSynsets(
                "synset 1",
                1,
                ERelNameSemantics.HYPONYM),
                "synset 2");

        checkContainsAll(div.getConnectedSynsets(
                "synset 3",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 2");

        checkContainsAll(div.getConnectedSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPONYM),
                "synset 3");

        checkContainsAll(div.getConnectedSynsets(
                "synset 3",
                2,
                ERelNameSemantics.HYPERNYM),
                "synset 1", "synset 2");

        checkContainsAll(div.getConnectedSynsets(
                "synset 1",
                2,
                ERelNameSemantics.HYPONYM),
                "synset 2", "synset 3");

        div.getSession()
           .close();

    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetConnectedSynsetsMultiRelNames() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);
        diversicon.importResource(GRAPH_4_HYP_HOL_HELLO, true);
        checkContainsAll(diversicon.getConnectedSynsets(
                "synset 4",
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                "synset 1",
                "synset 2");

        diversicon.getSession()
                  .close();
    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetConnectedSynsetsNoDups() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);

        diversicon.importResource(DAG_2_MULTI_REL, true);

        checkContainsAll(diversicon.getConnectedSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                "synset 1");

        diversicon.getSession()
                  .close();

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGetDbInfo() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

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
        assertEquals("lexical resource 1", job.getLexicalResourceName());
        assertTrue(job.getFileUrl()
                      .startsWith(Diversicon.MEMORY_PROTOCOL + ":"));
/*
        LexicalResource res2 = lmf().lexicon()
                                    .synset()
                                    .synset()
                                    .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                    .build();
        res2.setName("lexical resource 1");

        div.importResource(res2, true);
        assertEquals(2, div.getImportJobs()
                           .size());
        ImportJob job2 = div.getImportJobs()
                            .get(1);
        assertNotEquals(-1, job2.getId());
*/        
    }

    /**
     * @since 0.1.0
     * 
     */
    @Test
    public void testFormatImports() {
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        assertFalse(div.formatImportLog()
                       .isEmpty());
        div.importResource(GRAPH_4_HYP_HOL_HELLO, true);

        LOG.debug(div.formatImportLog());

        assertTrue(div.formatImportLog()
                      .contains(GRAPH_4_HYP_HOL_HELLO.getName()));

    }
    
    /**
     * Test for https://github.com/DavidLeoni/diversicon/issues/8
     * See also {@link UbyTest#testCantMergeSameLexicon()}
     * 
     * @since 0.1
     */
    @Test
    @Ignore
    public void testCantMergeSameLexicon(){
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(GRAPH_1_HYPERNYM, true);

        div.importResource(GRAPH_1_HYPERNYM, true);
        
        DivTester.checkDb(GRAPH_1_HYPERNYM, div);
        
        div.getSession().close();
    }
    
}
