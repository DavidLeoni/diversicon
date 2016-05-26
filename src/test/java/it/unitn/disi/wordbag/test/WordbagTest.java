package it.unitn.disi.wordbag.test;

import static org.junit.Assert.assertEquals;
import static it.unitn.disi.wordbag.test.LmfBuilder.lmf;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.wordbag.WbException;
import it.unitn.disi.wordbag.WbNotFoundException;
import it.unitn.disi.wordbag.WbSynsetRelation;
import it.unitn.disi.wordbag.Wordbag;
import it.unitn.disi.wordbag.Wordbags;
import it.unitn.disi.wordbag.internal.Internals;

public class WordbagTest {

    private static final Logger LOG = LoggerFactory.getLogger(WordbagTest.class);

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = new DBConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver",
                UBYH2Dialect.class.getName(), "root", "pass", true);
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }


    

    /**
     * todo this seems a uby bug, it always return null !
     * @since 0.1
     */
    @Test
    @Ignore
    public void todo() {
        // Synset syn = uby.getSynsetIterator(null).next();
    }

    /**
     * Tests db tables are automatically created.
     * 
     * @since 0.1
     */
    @Test
    public void testAutoCreate() {
        Wordbag uby = Wordbag.create(dbConfig);
        uby.getSession().close();
    }
    
    
    /**
     * Checks our extended model of uby with is actually returned by Hibernate
     * 
     * @since 0.1
     */
    @Test
    public void testHibernateExtraAttributes() {

        Wordbags.dropCreateTables(dbConfig);

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
        WbSynsetRelation synsetRelation = new WbSynsetRelation();
        synsetRelation.setRelType(ERelTypeSemantics.taxonomic);
        synsetRelation.setRelName(ERelNameSemantics.HYPERNYM);
        synsetRelation.setDepth(3);
        synsetRelation.setProvenance("a");
        synsetRelation.setSource(synset);
        synsetRelation.setTarget(synset);
        synset.getSynsetRelations()
              .add(synsetRelation);

        Wordbags.saveLexicalResourceToDb(dbConfig, lexicalResource, "lexical resource 1");       

        Wordbag uby = Wordbag.create(dbConfig);

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

        LOG.info("Asserting rel is instance of " + WbSynsetRelation.class);
        if (!(rel instanceof WbSynsetRelation)) {
            throw new RuntimeException(
                    "relation is not of type " + WbSynsetRelation.class + " found instead " + rel.getClass());
        }

        WbSynsetRelation WbRel = (WbSynsetRelation) rel;

        assertEquals(3, WbRel.getDepth());
        assertEquals("a", WbRel.getProvenance());
        
        uby.getSession().close();
    };
    
    /**
     * Saves provided {@code lexicalResource} to database, normalizes and augments database 
     * with transitive closure, and tests database actually matches {@code expectedLexicalResource}   
     * @param lexicalResource
     * @param expectedLexicalResource
     * @since 0.1
     */    
    public void assertAugmentation(
                LexicalResource lexicalResource,
                LexicalResource expectedLexicalResource                
            ){
        
        Wordbags.dropCreateTables(dbConfig);


        Wordbags.saveLexicalResourceToDb(dbConfig, lexicalResource, "lexical resource 1");
        
        Wordbag uby = Wordbag.create(dbConfig);

        uby.augmentGraph();
           
        checkDb(expectedLexicalResource, uby);
        
        uby.getSession().close();
                
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
                .synsetRelation(ERelNameSemantics.HYPERNYM, 1,2)
                .build());        
    }

    /**
     * @since 0.1
     */
    @Test
    public void testNormalizeCanonicalEdge() {              

        LexicalResource lexicalResource = lmf().lexicon()
                                               .synset()
                                               .synset()
                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                               .build();
        
        assertAugmentation(lexicalResource, lexicalResource);               
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
                               
                               lmf().lexicon()
                                .synset()
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                .synset()
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                .build());              
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
        
        assertNoAugmentation(lmf().lexicon()
                .synset()
                .synset()
                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                .synset()
                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                .synsetRelation(ERelNameSemantics.HYPERNYM, 2)                              
                .build());              
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
     * Asserts the provided lexical resource doesn't provoke any augmentation in the database.
     * @since 0.1
     */
    private void assertNoAugmentation(LexicalResource lr) {
        assertAugmentation(lr, lr);
    }

    /**
     * 
     * Retrieves the synset with id 'synset ' + {@code idNum}
     * 
     * @param idNum
     *            index starts from 1
     * @throws WbNotFoundException
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
        throw new WbNotFoundException("Couldn't find synset with id 'synset " + idNum);
    }

    /**
     * 
     * Checks provided lexical resource corresponds to current db.
     * 
     * Checks only for elements we care about in S-Match Uby, and only for the
     * ones which are not {@code null} in provided model.
     */
    public static void checkDb(LexicalResource lr, Uby uby) {
        Internals.checkNotNull(lr);

        LexicalResource ulr = uby.getLexicalResource(lr.getName());

        assertEquals(lr.getName(), ulr.getName());

        for (Lexicon lex : lr.getLexicons()) {

            try {
                Lexicon uLex = uby.getLexiconById(lex.getId());
                assertEquals(lex.getId(), uLex.getId());
                assertEquals(lex.getSynsets()
                                .size(),
                        uLex.getSynsets()
                            .size());

                for (Synset syn : lex.getSynsets()) {
                    try {
                        Synset uSyn = uby.getSynsetById(syn.getId());
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

                                if (sr instanceof WbSynsetRelation) {
                                    WbSynsetRelation Wbsr = (WbSynsetRelation) sr;
                                    WbSynsetRelation Wbusr = (WbSynsetRelation) usr;

                                    assertEquals(Wbsr.getDepth(), Wbusr.getDepth());

                                    if (Wbsr.getProvenance() != null) {
                                        assertEquals(Wbsr.getProvenance(), Wbusr.getProvenance());
                                    }
                                }
                            } catch (Error ex) {
                                throw new WbException("Error while checking synset relation: " + Wordbags.synsetRelationToString(sr),
                                        ex);
                            }

                        }
                    } catch (Error ex) {
                        String synId = syn == null ? "null" : syn.getId();
                        throw new WbException("Error while checking synset " + synId, ex);
                    }
                }
            } catch (Error ex) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new WbException("Error while checking lexicon " + lexId, ex);

            }
        }
    }

}
