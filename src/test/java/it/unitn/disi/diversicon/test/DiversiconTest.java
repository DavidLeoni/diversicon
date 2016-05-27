package it.unitn.disi.diversicon.test;

import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
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
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DivException;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;

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
     * 4 verteces, last one is connected to others by respectively hypernym edge, holonym and 'hello' edge
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
                                                               .build();

    /**
     *  2 verteces, second connected to first with two relations.  
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

        Diversicons.saveLexicalResourceToDb(dbConfig, lexicalResource, "lexical resource 1");

        Diversicon uby = Diversicon.create(dbConfig);

        assertNotNull(uby.getLexicalResource("lexicalResource 1"));
        assertEquals(1, uby.getLexicons()
                           .size());

        Lexicon rlexicon = uby.getLexicons()
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

        uby.getSession()
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

        Diversicons.saveLexicalResourceToDb(dbConfig, lexicalResource, "lexical resource 1");

        Diversicon uby = Diversicon.create(dbConfig);

        uby.augmentGraph();

        checkDb(expectedLexicalResource, uby);

        uby.getSession()
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
                     .synset()
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 3)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                     .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
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

        List<String> synsetIds = new ArrayList();
        while (iter.hasNext()) {
            synsetIds.add(iter.next().getId());
        }
                
        List<String> listIds = new ArrayList();
        
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
    public void testGetSynsetParents_Graph_1_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicons.saveLexicalResourceToDb(dbConfig, GRAPH_1_HYPERNYM, "lexical resource 1");

        Diversicon wb = Diversicon.create(dbConfig);

        assertFalse(wb.getSynsetParents(
                "synset 2",
                0,
                ERelNameSemantics.HYPERNYM)
                      .hasNext());

        checkContainsAll(wb.getSynsetParents(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(wb.getSynsetParents(
                "synset 2",
                1,
                "hello"));

        wb.getSession()
          .close();

    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetSynsetParents_Dag_3_Hypernym() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicons.saveLexicalResourceToDb(dbConfig, DAG_3_HYPERNYM, "lexical resource 1");

        Diversicon wb = Diversicon.create(dbConfig);

        checkContainsAll(wb.getSynsetParents(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 1");

        checkContainsAll(wb.getSynsetParents(
                "synset 3",
                1,
                ERelNameSemantics.HYPERNYM),
                "synset 2");

        checkContainsAll(wb.getSynsetParents(
                "synset 3",
                2,
                ERelNameSemantics.HYPERNYM),
                "synset 1", "synset 2");

        wb.getSession()
          .close();

    }
    
    /**
     * @since 0.1
     */
    @Test
    public void testGetSynsetParentsMultiRelNames() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicons.saveLexicalResourceToDb(dbConfig, GRAPH_4_HYP_HOL_HELLO, "lexical resource 1");

        Diversicon wb = Diversicon.create(dbConfig);

        checkContainsAll(wb.getSynsetParents(
                "synset 4",
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                "synset 1",
                "synset 2");

        wb.getSession()
          .close();

    }

    /**
     * @since 0.1
     */
    @Test
    public void testGetSynsetParentsNoDups() {

        Diversicons.dropCreateTables(dbConfig);

        Diversicons.saveLexicalResourceToDb(dbConfig, DAG_2_MULTI_REL, "lexical resource 1");

        Diversicon wb = Diversicon.create(dbConfig);

        checkContainsAll(wb.getSynsetParents(
                "synset 2",
                1,
                ERelNameSemantics.HYPERNYM,
                ERelNameSemantics.HOLONYM),
                "synset 1");

        wb.getSession()
          .close();

    }

    
    
    /**
     * 
     * Retrieves the synset with id 'synset ' + {@code idNum}
     * 
     * @param idNum
     *            index starts from 1
     * @throws DivNotFoundException
     */
    public static Synset getSynset(LexicalResource lr, int idNum) {
        Internals.checkArgument(idNum >= 1, "idNum must be positive, found instead " + idNum);

        for (Lexicon lexicon : lr.getLexicons()) {
            for (Synset synset : lexicon.getSynsets()) {
                if (synset.getId()
                          .equals("synset " + idNum)) {
                    return synset;
                }
            }
        }
        throw new DivNotFoundException("Couldn't find synset with id 'synset " + idNum);
    }

    /**
     * 
     * Checks provided lexical resource corresponds to current db.
     * 
     * Checks only for elements we care about in Diversicon, and only for the
     * ones which are not {@code null} in provided model.
     */
    public static void checkDb(LexicalResource lr, Diversicon diversicon) {
        Internals.checkNotNull(lr);

        LexicalResource ulr = diversicon.getLexicalResource(lr.getName());

        assertEquals(lr.getName(), ulr.getName());

        for (Lexicon lex : lr.getLexicons()) {

            try {
                Lexicon uLex = diversicon.getLexiconById(lex.getId());
                assertEquals(lex.getId(), uLex.getId());
                assertEquals(lex.getSynsets()
                                .size(),
                        uLex.getSynsets()
                            .size());

                for (Synset syn : lex.getSynsets()) {
                    try {
                        Synset uSyn = diversicon.getSynsetById(syn.getId());
                        assertEquals(syn.getId(), uSyn.getId());

                        assertEquals(syn.getSynsetRelations()
                                        .size(),
                                uSyn.getSynsetRelations()
                                    .size());

                        Iterator<SynsetRelation> iter = uSyn.getSynsetRelations()
                                                            .iterator();

                        for (SynsetRelation sr : syn.getSynsetRelations()) {

                            try {
                                SynsetRelation usr = iter.next();

                                if (sr.getRelName() != null) {
                                    assertEquals(sr.getRelName(), usr.getRelName());
                                }

                                if (sr.getRelType() != null) {
                                    assertEquals(sr.getRelType(), usr.getRelType());
                                }

                                if (sr.getSource() != null) {
                                    assertEquals(sr.getSource()
                                                   .getId(),
                                            usr.getSource()
                                               .getId());
                                }

                                if (sr.getTarget() != null) {
                                    assertEquals(sr.getTarget()
                                                   .getId(),
                                            usr.getTarget()
                                               .getId());
                                }

                                if (sr instanceof DivSynsetRelation) {
                                    DivSynsetRelation Wbsr = (DivSynsetRelation) sr;
                                    DivSynsetRelation Wbusr = (DivSynsetRelation) usr;

                                    assertEquals(Wbsr.getDepth(), Wbusr.getDepth());

                                    if (Wbsr.getProvenance() != null) {
                                        assertEquals(Wbsr.getProvenance(), Wbusr.getProvenance());
                                    }
                                }
                            } catch (Error ex) {
                                throw new DivException("Error while checking synset relation: "
                                        + Diversicons.synsetRelationToString(sr),
                                        ex);
                            }

                        }
                    } catch (Error ex) {
                        String synId = syn == null ? "null" : syn.getId();
                        throw new DivException("Error while checking synset " + synId, ex);
                    }
                }
            } catch (Error ex) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new DivException("Error while checking lexicon " + lexId, ex);

            }
        }
    }

    public static DBConfig createDbConfig() {
        return new DBConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver",
                UBYH2Dialect.class.getName(), "root", "pass", true);
    }
}
