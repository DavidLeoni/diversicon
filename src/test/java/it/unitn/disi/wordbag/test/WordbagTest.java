package it.unitn.disi.wordbag.test;

import static org.junit.Assert.assertEquals;
import static it.unitn.disi.wordbag.test.LmfBuilder.lmf;
import static org.junit.Assert.assertNotNull;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import it.unitn.disi.wordbag.WbSynsetRelation;
import it.unitn.disi.wordbag.Wordbag;
import it.unitn.disi.wordbag.Wordbags;

public class WordbagTest {

    private static final Logger log = LoggerFactory.getLogger(WordbagTest.class);

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
     * Tests simple saving with Hibernate
     * 
     * @since 0.1
     */
    @Test
    public void testHibernateSave() {
        /*
         * try {
         * Wordbags.createTables(dbConfig);
         * } catch (FileNotFoundException e) {
         * throw new RuntimeException("Couldn't create tables in database " +
         * dbConfig.getJdbc_url() + "!", e); // todo
         * }
         * 
         * WbLinguisticOracle oracle = new WbLinguisticOracle(dbConfig, null);
         * 
         * WbUby uby = oracle.getUby();
         * 
         * uby.getSession().save(arg0)
         */
    }

    /**
     * todo this seems a uby bug, it always return null !
     */
    @Test
    @Ignore
    public void todo() {
        // Synset syn = uby.getSynsetIterator(null).next();
    }

    /**
     * Checks our extended model of uby with is actually returned by Hibernate
     * 
     * @since 0.1
     */
    @Test
    public void testHibernateExtraAttributes() {

        Wordbags.createTables(dbConfig);

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

        Wordbag uby = new Wordbag(dbConfig);

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

        log.info("Asserting rel is instance of " + WbSynsetRelation.class);
        if (!(rel instanceof WbSynsetRelation)) {
            throw new RuntimeException(
                    "relation is not of type " + WbSynsetRelation.class + " found instead " + rel.getClass());
        }

        WbSynsetRelation WbRel = (WbSynsetRelation) rel;

        assertEquals(3, WbRel.getDepth());
        assertEquals("a", WbRel.getProvenance());
    };
    
    /**
     * Saves provided {@code lexicalResource} to database, normalizes and augments database 
     * with transitive closure, and tests database actually matches {@code expectedLexicalResource}   
     * @param lexicalResource
     * @param expectedLexicalResource
     */    
    public void assertAugmentation(
                LexicalResource lexicalResource,
                LexicalResource expectedLexicalResource                
            ){
        
        Wordbags.createTables(dbConfig);


        Wordbags.saveLexicalResourceToDb(dbConfig, lexicalResource, "lexical resource 1");
        
        Wordbag uby = new Wordbag(dbConfig);

        uby.augmentGraph();
           
        WordbagTester.checkDb(expectedLexicalResource, uby);
                
    };

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

    @Test
    public void testNormalizeCanonicalEdge() {              

        LexicalResource lexicalResource = lmf().lexicon()
                                               .synset()
                                               .synset()
                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                               .build();
        
        assertAugmentation(lexicalResource, lexicalResource);               
    }
    
    @Test
    public void testNormalizeUnknownEdge() {              

        LexicalResource lexicalResource = lmf().lexicon()
                                               .synset()
                                               .synset()
                                               .synsetRelation("hello", 1)
                                               .build();
        
        assertAugmentation(lexicalResource, lexicalResource);               
    }
    
    
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
     */
    private void assertNoAugmentation(LexicalResource lr) {
        assertAugmentation(lr, lr);
    }

    

}
