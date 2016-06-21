package it.unitn.disi.diversicon.test;


import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.xml.sax.SAXException;

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
import de.tudarmstadt.ukp.lmf.transform.DBToXMLTransformer;
import it.unitn.disi.diversicon.DivException;
import it.unitn.disi.diversicon.DivIoException;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;
import it.unitn.disi.diversicon.internal.Internals.ExtractedStream;



public class DivUtilsTest {
   
    
        
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
     * @since 0.1
     */
    @Test
    public void testRestoreWrongDump() throws IOException{
        Path dir = Files.createTempDirectory("diversicon-test");
        try {
            Diversicons.restoreH2Dump("file:"+ dir.toString() +"/666" , dbConfig);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivIoException ex){
            
        }
        
        try {
            Diversicons.restoreH2Dump("classpath:/666" , dbConfig);
            Assert.fail("Shouldn't arrive here!");
        } catch (DivIoException ex){
            
        }
    }
}