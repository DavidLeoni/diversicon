package it.unitn.disi.diversicon.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DivIoException;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;



public class DivUtilsTest {
       
        
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;
        
    @Nullable
    private String savedKeepTempFiles ;
    
    @Before
    public void beforeMethod(){
         savedKeepTempFiles = System.getProperty(Diversicon.DEBUG_KEEP_TEMP_FILES);
         dbConfig = DivTester.createNewDbConfig();                
    }
    
    @After
    public void afterMethod(){
        if (savedKeepTempFiles != null){
            System.setProperty(Diversicon.DEBUG_KEEP_TEMP_FILES, savedKeepTempFiles);    
        }
        
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
        
        assertFalse(Diversicons.isSchemaValid(dbConfig));
        
        Diversicons.dropCreateTables(dbConfig);
        assertTrue(Diversicons.isSchemaValid(dbConfig));
           
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
                
        LexicalResource lexicalResource1 = LmfBuilder.lmf()
                .lexicon()
                .synset()  
                .definition("cool")
                .lexicalEntry("a")
                .synset()
                .lexicalEntry("b")
                .build();

        LexicalResource lexicalResource2 = LmfBuilder.lmf()
                .lexicon()
                .synset()
                .definition("uncool")
                .build();                
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);               
        
        div.importResource(lexicalResource1,  true);
        
        DivTester.checkDb(lexicalResource1, div);
      
        try {
            DivTester.checkDb(lexicalResource2, div);
            Assert.fail("Shouldn't arrive here!");
        } catch (Exception ex){
            
        }
        
        div.getSession().close();
    
    }
    
    @Test
    public void testGetLexicalResourceName() throws SAXException, IOException{

        File outFile = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM);
        String name = Diversicons.extractNameFromLexicalResource(outFile.getAbsolutePath());        
        assertEquals(DivTester.GRAPH_1_HYPERNYM.getName(), name);

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testRestoreWrongDump() throws IOException{
        Path dir = DivTester.createTestDir();
        try {
            Diversicons.restoreH2Sql("file:"+ dir.toString() +"/666" , dbConfig);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivIoException ex){
            
        }
        
        try {
            Diversicons.restoreH2Sql("classpath:/666" , dbConfig);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivIoException ex){
            
        }
    }


    /**
     * Tricky for logging!
     * 
     * @since 0.1.0
     */
    @Test
    public void testDropCreateTables(){        
        Diversicons.dropCreateTables(dbConfig);
        
        // todo improve test
    }
    
    /**
     * Tricky for logging!
     * 
     * @since 0.1.0
     */
    @Test
    public void testCreateTables(){        
        Diversicons.createTables(dbConfig);
        // todo improve test
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testTempFilesDeletion(){        
        System.setProperty(Diversicon.DEBUG_KEEP_TEMP_FILES, Boolean.toString(true));        
        Internals.createTempDivDir("will-survive-");
        System.setProperty(Diversicon.DEBUG_KEEP_TEMP_FILES, Boolean.toString(false));
        Internals.createTempDivDir("wont-survive-");               
    }

}