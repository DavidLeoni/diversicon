package it.unitn.disi.diversicon.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import it.unitn.disi.diversicon.internal.Internals;



public class UtilsTest {

		
	private static final Logger log = LoggerFactory.getLogger(UtilsTest.class);
	
	private DBConfig dbConfig;
		
	
	@Before
	public void beforeMethod(){
		 dbConfig = new DBConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver",
				UBYH2Dialect.class.getName(), "root", "pass", true);		 		
	}
	
	@After
	public void afterMethod(){
		dbConfig = null;
	}

	@Test
	public void testInverses(){
		assertTrue(Diversicons.isInverse(ERelNameSemantics.HYPERNYM, ERelNameSemantics.HYPONYM));
		assertTrue(Diversicons.isInverse(ERelNameSemantics.HYPONYM, ERelNameSemantics.HYPERNYM));
		
		assertFalse(Diversicons.isInverse("a", ERelNameSemantics.HYPERNYM));
		
		try {
			Diversicons.getInverse("a");
			Assert.fail("Shouldn't arrive here!");
		} catch (DivNotFoundException ex){
			
		}
	}	
	
	
	@Test
	public void testExistsDb(){
	    
	    assertFalse(Diversicons.exists(dbConfig));
	    
	    Diversicons.dropCreateTables(dbConfig);
	    assertTrue(Diversicons.exists(dbConfig));
	       
	}
	
	
	
	@Test
	public void testNewMap(){
	    
	    HashMap<String, Integer> m1 = Internals.newMap("a", 1);	    
	    assertEquals(Integer.valueOf(1), m1.get("a"));
	    
        try {
            Internals.newMap("a", "b", 3, "f");
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex){
            
        }
        HashMap<String, Integer> m2 = Internals.newMap("a", 1, "b", 2);     
        assertEquals(Integer.valueOf(1),  m2.get("a"));
        assertEquals(Integer.valueOf(2),  m2.get("b"));
	    
	}
	
    @Test
    // todo make it more extensive
    public void testBuilder(){
                
        LexicalResource lexicalResource = LmfBuilder.lmf()
                .lexicon()
                .synset()                                            
                .lexicalEntry("abc", 1)
                .build();

        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.create(dbConfig);

        div.importResource(lexicalResource, "lexical resource 1");
        
        checkDb(lexicalResource, div);
        
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
                Lexicon dbLex = diversicon.getLexiconById(lex.getId());
                assertEquals(lex.getId(), dbLex.getId());
                assertEquals(lex.getSynsets()
                                .size(),
                        dbLex.getSynsets()
                             .size());

                for (Synset syn : lex.getSynsets()) {
                    try {
                        Synset dbSyn = diversicon.getSynsetById(syn.getId());
                        assertEquals(syn.getId(), dbSyn.getId());

                        assertEquals(syn.getSynsetRelations()
                                        .size(),
                                dbSyn.getSynsetRelations()
                                     .size());

                        Iterator<SynsetRelation> iter = dbSyn.getSynsetRelations()
                                                             .iterator();

                        for (SynsetRelation sr : syn.getSynsetRelations()) {

                            try {
                                SynsetRelation dbSr = iter.next();

                                if (sr.getRelName() != null) {
                                    assertEquals(sr.getRelName(), dbSr.getRelName());
                                }

                                if (sr.getRelType() != null) {
                                    assertEquals(sr.getRelType(), dbSr.getRelType());
                                }

                                if (sr.getSource() != null) {
                                    assertEquals(sr.getSource()
                                                   .getId(),
                                            dbSr.getSource()
                                                .getId());
                                }

                                if (sr.getTarget() != null) {
                                    assertEquals(sr.getTarget()
                                                   .getId(),
                                            dbSr.getTarget()
                                                .getId());
                                }

                                if (sr instanceof DivSynsetRelation) {
                                    DivSynsetRelation divSr = (DivSynsetRelation) sr;
                                    DivSynsetRelation divDbSr = (DivSynsetRelation) dbSr;

                                    assertEquals(divSr.getDepth(), divDbSr.getDepth());

                                    if (divSr.getProvenance() != null
                                            && !divSr.getProvenance()
                                                     .isEmpty()) {
                                        assertEquals(divSr.getProvenance(), divDbSr.getProvenance());
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

                    for (LexicalEntry le : lex.getLexicalEntries()) {
                        try {
                            LexicalEntry dbLe = diversicon.getLexicalEntryById(le.getId());

                            assertEquals(le.getId(), dbLe.getId());

                            assertEquals(le.getSenses()
                                           .size(),
                                    dbLe.getSenses()
                                        .size());

                            if (le.getLemma() != null) {
                                Lemma lemma = le.getLemma();

                                for (FormRepresentation fr : lemma.getFormRepresentations()) {
                                    assertEquals(fr.getWrittenForm(), dbLe.getLemmaForm());
                                    break;
                                }

                            }

                        } catch (Error err) {
                            String leId = le == null ? "null" : le.getId();
                            throw new DivException("Error while checking lexical entry " + leId, err);
                        }
                    }

                }
            } catch (Error err) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new DivException("Error while checking lexicon " + lexId, err);

            }
        }
    }

    public static DBConfig createDbConfig() {
        return new DBConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver",
                UBYH2Dialect.class.getName(), "root", "pass", true);
    }

}