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
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.data.Examplicon;
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
    public void testReadLexicalResourceName() throws SAXException, IOException{
        File outFile = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM);
        String name = Diversicons.readLexicalResourceName(outFile.getAbsolutePath());        
        assertEquals(DivTester.GRAPH_1_HYPERNYM.getName(), name);
    }

    /**
     * @since 0.1.0
     */    
    @Test
    public void testReadLexicalResourceNameNotFound() throws SAXException, IOException{

        File outFile = Internals.createTempFile("diversicon-test", "xml").toFile();
        FileUtils.writeStringToFile(outFile, "<LexicalResource></LexicalResource>");        
        try {
            Diversicons.readLexicalResourceName(outFile.getAbsolutePath());
            Assert.fail("Shouldn't arive here!");
        } catch (DivNotFoundException ex){
            
        }        
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testReadLexicalResourceNamespaces() throws SAXException, IOException{
        
        File outFile = new File(""); //DivTester.writeXml();
        Map<String, String> namespaces = Diversicons.readLexicalResourceNamespaces(outFile.getAbsolutePath());
        assertEquals(0, namespaces.size());
        throw new UnsupportedOperationException("todo implemenet me!");
    }

    /**
     * @since 0.1.0
     */    
    @Test
    public void testReadLexicalResourceNamespacesNotFound() throws SAXException, IOException{

        File outFile = Internals.createTempFile("diversicon-test", "xml").toFile();
        FileUtils.writeStringToFile(outFile, "<LexicalResource></LexicalResource>");        
        
        Map<String, String> namespaces = Diversicons.readLexicalResourceNamespaces(outFile.getAbsolutePath());
        assertEquals(0, namespaces.size());
                
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
        System.setProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(true));        
        Internals.createTempDivDir("will-survive-");
        System.setProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(false));
        Internals.createTempDivDir("wont-survive-");               
    }

    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidate(){
        File f = Internals.readData(Examplicon.XML_URI).toTempFile();
        
        Diversicons.validateXml(f);
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