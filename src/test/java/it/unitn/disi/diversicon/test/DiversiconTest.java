package it.unitn.disi.diversicon.test;

import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.morphology.FormRepresentation;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DivException;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import static it.unitn.disi.diversicon.test.UtilsTest.checkDb;
import static it.unitn.disi.diversicon.test.UtilsTest.createDbConfig;

public class DiversiconTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiversiconTest.class);

    /**
     * 2 verteces and 1 hypernym edge
     */
    private static LexicalResource GRAPH_1_HYPERNYM = lmf().lexicon()
                                                           .synset()
                                                           .synset()
                                                           .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                           .build();

    /**
     * 4 verteces, last one is connected to others by respectively hypernym
     * edge, holonym and 'hello' edge
     */
    private static LexicalResource GRAPH_4_HYP_HOL_HELLO = lmf().lexicon()
                                                                .synset()
                                                                .synset()
                                                                .synset()
                                                                .synset()
                                                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                                .synsetRelation(ERelNameSemantics.HOLONYM, 2)
                                                                .synsetRelation("hello", 3)
                                                                .build();

    /**
     * A full DAG, 3 verteces and 3 hypernyms
     */
    private static final LexicalResource DAG_3_HYPERNYM = lmf().lexicon()
                                                               .synset()
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .depth(2)
                                                               .build();

    /**
     * 2 verteces, second connected to first with two relations.
     */
    private static final LexicalResource DAG_2_MULTI_REL = lmf().lexicon()
                                                                .synset()
                                                                .synset()
                                                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                                .synsetRelation(ERelNameSemantics.HOLONYM, 1)
                                                                .build();

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = createDbConfig();
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

        diversicon.importResource(lexicalResource, "lexical resource 1");

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

        diversicon.importResource(lexicalResource, "lexical resource 1");

        diversicon.augmentGraph();

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
    public void testGetTransitiveSynsets_Graph_1_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);

        diversicon.importResource(GRAPH_1_HYPERNYM, "lexical resource 1");

        assertFalse(diversicon.getTransitiveSynsets(
                "synset 2",
                0,
                ERelNameSemantics.HYPERNYM)
                              .hasNext());
        
        assertFalse(diversicon.getTransitiveSynsets(
                "synset 2",
                -1).hasNext());

        checkContainsAll(diversicon.getTransitiveSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(diversicon.getTransitiveSynsets(
                "synset 2",
                -1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(diversicon.getTransitiveSynsets(
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
    public void testIsReachable_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(DAG_3_HYPERNYM, "lexical resource 1");

        assertTrue(div.isReachable(
                div.getSynsetById("synset 1"),
                div.getSynsetById("synset 1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isReachable(
                div.getSynsetById("synset 1"),
                div.getSynsetById("synset 1"),
                0, new ArrayList()));
        
        
        assertFalse(div.isReachable(
                div.getSynsetById("synset 2"),
                div.getSynsetById("synset 1"),
                0,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        
        assertTrue(div.isReachable(
                div.getSynsetById("synset 2"),
                div.getSynsetById("synset 1"),
                1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isReachable(
                div.getSynsetById("synset 2"),
                div.getSynsetById("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));
        
        assertTrue(div.isReachable(
                div.getSynsetById("synset 1"),
                div.getSynsetById("synset 3"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));
        
        assertFalse(div.isReachable(
                div.getSynsetById("synset 3"),
                div.getSynsetById("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPONYM)));
        
        assertFalse(div.isReachable(
                div.getSynsetById("synset 1"),
                div.getSynsetById("synset 2"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));

        assertTrue(div.isReachable(
                div.getSynsetById("synset 1"),
                div.getSynsetById("synset 1"),
                -1,
                Arrays.asList(ERelNameSemantics.HYPERNYM)));
        
                   
        div.getSession().close();        

    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetTransitiveSynsets_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(DAG_3_HYPERNYM, "lexical resource 1");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 1",
                1,
                ERelNameSemantics.HYPONYM),
                "synset 2");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 3",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 2");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPONYM),
                "synset 3");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 3",
                2,
                ERelNameSemantics.HYPERNYM),
                "synset 1", "synset 2");

        checkContainsAll(div.getTransitiveSynsets(
                "synset 1",
                2,
                ERelNameSemantics.HYPONYM),
                "synset 2", "synset 3");


        div.getSession().close();

    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetTransitiveSynsetsMultiRelNames() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);
        diversicon.importResource(GRAPH_4_HYP_HOL_HELLO, "lexical resource 1");
        checkContainsAll(diversicon.getTransitiveSynsets(
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
    public void testGetTransitiveSynsetsNoDups() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicon diversicon = Diversicon.create(dbConfig);

        diversicon.importResource(DAG_2_MULTI_REL, "lexical resource 1");

        checkContainsAll(diversicon.getTransitiveSynsets(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                "synset 1");

        diversicon.getSession()
                  .close();

    }

}
