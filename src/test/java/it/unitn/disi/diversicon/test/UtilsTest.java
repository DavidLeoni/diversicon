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
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DivNotFoundException;
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
}