package it.unitn.disi.diversicon.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
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
import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.disi.unitn.diversicon.exceptions.DivIoException;
import it.disi.unitn.diversicon.exceptions.DivNotFoundException;
import it.unitn.disi.diversicon.BuildInfo;
import it.unitn.disi.diversicon.DivXmlErrorHandler;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.LexResPackage;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.data.DivWn31;
import it.unitn.disi.diversicon.data.Examplicon;
import it.unitn.disi.diversicon.exceptions.InvalidXmlException;
import it.unitn.disi.diversicon.internal.ExtractedStream;
import it.unitn.disi.diversicon.internal.Internals;



public class DivUtilsTest {
       
        
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;       
        
    @Nullable
    private String savedKeepTempFiles ;
    
    @Before
    public void beforeMethod(){
         savedKeepTempFiles = System.getProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES);
         dbConfig = DivTester.createNewDbConfig();             
    }
    
    @After
    public void afterMethod(){
        if (savedKeepTempFiles != null){
            System.setProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES, savedKeepTempFiles);    
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


    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDiversiconResource() {
        
        LexResPackage dr = Diversicons.readResource(Examplicon.XML_URI);               
        
        assertEquals(Examplicon.NAME, dr.getName());
        
        assertEquals(Examplicon.PREFIX, dr.getPrefix());
        
        Map<String, String> ns = Diversicons.readResource(Examplicon.XML_URI).getNamespaces();
        
        assertEquals(3, ns.size());
        assertTrue(ns.containsKey(Examplicon.PREFIX));
        assertTrue(ns.containsKey(DivWn31.PREFIX));
        
        assertTrue(ns.get(Examplicon.PREFIX).contains(Examplicon.ID + ".lmf.xml"));
        assertTrue(ns.get(DivWn31.PREFIX).contains(DivWn31.ID + ".lmf.xml"));
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
        
        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        div.getSession().close();
        
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
        System.setProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(true));        
        Internals.createTempDivDir("will-survive-");
        System.setProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(false));
        Internals.createTempDivDir("wont-survive-");               
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXml(){
        File f = Internals.readData(Examplicon.XML_URI).toTempFile();
        
        Diversicons.validateXml(f, LOG);
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlLogLimitZero(){
        File f = Internals.readData(Examplicon.XML_URI).toTempFile();
        
        Diversicons.validateXml(f, LOG, 0);
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlLogLimitOne(){
        File f = Internals.readData(Examplicon.XML_URI).toTempFile();
        
        Diversicons.validateXml(f, LOG, 1);
    }
    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlFatalIllFormed() throws IOException{
        
        File f = DivTester.writeXml("666");
        
        try {
            Diversicons.validateXml(f, LOG);            
            Assert.fail("Shouldn't arrive here!");            
        } catch (InvalidXmlException ex){
            LOG.debug("Catched exception:", ex);
            assertFatal(ex);
        }
    }
    
    /**
     * Asserts one fatal error occurred
     * 
     * @since 0.1.0
     */
    private static void assertFatal(InvalidXmlException ex) {
        DivXmlErrorHandler errorHandler = ex.getErrorHandler();
        assertTrue(errorHandler.isFatal());
        assertEquals(1, errorHandler.issuesCount());
        assertTrue(errorHandler.fatalError() != null);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlFatalUnclosedTag() throws IOException{
                
        File f = DivTester.writeXml("<bla>");
        
        try {
            Diversicons.validateXml(f, LOG);            
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidXmlException ex){
            LOG.debug("Catched exception:", ex);
            assertFatal(ex);
        }
    }
    

    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDataJar(){
        File f = new File("src/test/resources/test.jar!/a.txt");
        ExtractedStream es = Internals.readData("jar:file://" + f.getAbsolutePath(), false);
        LOG.debug("Extracted stream = " + es.toString());
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testReadCompressedDataJar(){
        File f = new File("src/test/resources/test.jar!/b.txt.xz");
        ExtractedStream es = Internals.readData("jar:file://" + f.getAbsolutePath(), true);
        LOG.debug("Extracted stream = " + es.toString());
    }
    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testNamespacePrefix(){
        Pattern p = Diversicons.NAMESPACE_PREFIX_PATTERN;
        assertFalse(p.matcher("").matches());
        assertFalse(p.matcher("-").matches());
        assertFalse(p.matcher("_").matches());
        assertFalse(p.matcher("2").matches());
        assertTrue(p.matcher("a").matches());
        assertFalse(p.matcher(".").matches());
        assertTrue(p.matcher("a.b").matches());
        assertFalse(p.matcher("a:b").matches());
        assertFalse(p.matcher("a:").matches());
        assertTrue(p.matcher("a-").matches());
    }
    
}