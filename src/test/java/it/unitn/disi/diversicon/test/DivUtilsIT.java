package it.unitn.disi.diversicon.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;
import it.unitn.disi.diversicon.internal.Internals.ExtractedStream;

public class DivUtilsIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;
        
    
    @Before
    public void beforeMethod(){
         dbConfig = DivTester.createNewDbConfig();                
    }
    
    @After
    public void afterMethod(){
        dbConfig = null;       
    }

    @Test
    public void testRestoreH2Dump(){
        Diversicons.restoreH2Dump(Diversicons.WORDNET_DIV_DB_RESOURCE_URI, dbConfig);
        
        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        
        div.getSession().close();
    }
    
    @Test
    public void testReadDataWordnetDb(){
        ExtractedStream es = Internals.readData(Diversicons.WORDNET_DIV_DB_RESOURCE_URI, true);
        assertTrue(es.isExtracted());
        assertEquals("script.sql", es.getFilepath());
        assertEquals(Diversicons.WORDNET_DIV_DB_RESOURCE_URI, es.getSourceUrl());
        File f = es.toFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        
    }
    
    @Test
    public void testReadDataWordnetXml(){
        ExtractedStream es = Internals.readData(Diversicons.WORDNET_UBY_XML_RESOURCE_URI, true);
        assertTrue(es.isExtracted());
        assertEquals("uby-wn30.xml", es.getFilepath());
        assertEquals(Diversicons.WORDNET_UBY_XML_RESOURCE_URI, es.getSourceUrl());
        File f = es.toFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);                
    }
    

}
