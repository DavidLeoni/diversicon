package it.unitn.disi.diversicon.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unitn.disi.diversicon.internal.Internals;

/**
 * @since 0.1.0
 *
 */
public class DivXmlWriterTest {

    private static final Logger LOG = LoggerFactory.getLogger(DivXmlWriterTest.class);
    
    /**
     * 
     * @since 0.1.0
     */
    @Test
    public void testTransform() throws IOException{
                        
        File xml = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM);
        
        String str = FileUtils.readFileToString(xml, "UTF-8");
        LOG.debug(str);
        
        assertTrue(!str.contains("DivSynsetRelation"));
        
    }
    
    /**
     * 
     * @since 0.1.0
     */
    @Test
    public void testTransformWithNamespaces() throws IOException, DocumentException {
                        
        File xml = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM,
                Internals.newMap("prefix-1", "url-1",
                                 "prefix-2", "url-2"));
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(xml);
                
        String str = FileUtils.readFileToString(xml, "UTF-8");
        LOG.debug(str);
        
        assertEquals(1, document
                    .selectNodes("//LexicalResource[namespace::*[.='url-1'] "
                                 + " and namespace::*[.='url-2'] ]" )
                     .size());
        // using just string matching because can't select xmlns stuff: https://www.oxygenxml.com/forum/topic4845.html 
        assertTrue(str.contains("xmlns:prefix-1=\"url-1\""));
        assertTrue(str.contains("xmlns:prefix-2=\"url-2\""));
                
    }
    
}
