package it.unitn.disi.diversicon.test;

import static it.unitn.disi.diversicon.test.DivTester.*;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static it.unitn.disi.diversicon.test.DivTester.DEFAULT_TEST_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.xerces.impl.dtd.DTDGrammar;
import org.apache.xerces.impl.dtd.XMLContentSpec;
import org.apache.xerces.impl.dtd.XMLElementDecl;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.disi.unitn.diversicon.exceptions.DivIoException;
import it.disi.unitn.diversicon.exceptions.DivNotFoundException;
import it.unitn.disi.diversicon.DivXmlHandler;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.LexResPackage;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.data.DivWn31;
import it.unitn.disi.diversicon.data.Examplicon;
import it.unitn.disi.diversicon.exceptions.InvalidXmlException;
import it.unitn.disi.diversicon.internal.ExtractedStream;
import it.unitn.disi.diversicon.internal.Internals;


/**
 * @since 0.1.0
 *
 */
public class DivUtilsTest {
       
        
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;       
        
    @Nullable
    private String savedKeepTempFiles ;
    
    @Before
    public void beforeMethod(){
         savedKeepTempFiles = System.getProperty(Diversicons.PROPERTY_DEBUG_KEEP_TEMP_FILES);
         dbConfig = DivTester.createNewDbConfig();             
    }
    
    @After
    public void afterMethod(){
        if (savedKeepTempFiles != null){
            System.setProperty(Diversicons.PROPERTY_DEBUG_KEEP_TEMP_FILES, savedKeepTempFiles);    
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
                
        LexicalResource lexRes1 = LmfBuilder.lmf()
                .lexicon()
                .synset()  
                .definition("cool")
                .lexicalEntry("a")
                .synset()
                .lexicalEntry("b")
                .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                .build();

        assertEquals(1, lexRes1.getLexicons().size());
        assertEquals(2, lexRes1.getLexicons().get(0).getSynsets().size());
        assertEquals(1, lexRes1.getLexicons().get(0).getSynsets().get(1).getSynsetRelations().size());
        
        LexicalResource lexRes2 = LmfBuilder.lmf()
                .lexicon()
                .synset()
                .lexicalEntry()                
                .definition("uncool")
                .build();                
        
        Diversicons.dropCreateTables(dbConfig);

        Diversicon div = Diversicon.connectToDb(dbConfig);               
        
        DivTester.importResource(div,lexRes1,  true);
        
        DivTester.checkDb(lexRes1, div);
      
        try {
            DivTester.checkDb(lexRes2, div);
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
        
        LexResPackage dr = Diversicons.readPackageFromLexRes(Examplicon.XML_URI);               
        
        assertEquals(Examplicon.LABEL, dr.getLabel());
        
        assertEquals(Examplicon.PREFIX, dr.getPrefix());
        
        Map<String, String> ns = Diversicons.readPackageFromLexRes(Examplicon.XML_URI).getNamespaces();
        
        assertEquals(3, ns.size());
        assertTrue(ns.containsKey(Examplicon.PREFIX));
        assertTrue(ns.containsKey(DivWn31.PREFIX));
        
        assertTrue(ns.get(Examplicon.PREFIX).contains(Examplicon.SHORT_NAME + ".lmf.xml"));
        assertTrue(ns.get(DivWn31.PREFIX).contains(DivWn31.NAME + ".lmf.xml"));
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
        System.setProperty(Diversicons.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(true));        
        Internals.createTempDivDir("will-survive-");
        System.setProperty(Diversicons.PROPERTY_DEBUG_KEEP_TEMP_FILES, Boolean.toString(false));
        Internals.createTempDivDir("wont-survive-");               
    }
        
    
    
    /**
     * @since 0.1.0
     */
    @Test    
    public void testValidateExamplicon(){
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
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlUndeclaredPrefix() throws IOException{
        
        LexicalResource lexRes = 
                lmf()
                .lexicon()
                .synset()
                .lexicalEntry()
                .synset()
                .build();
        
        lexRes.getLexicons().get(0).getSynsets().get(1).setId("prefix666_synset-2");                
        File xml = DivTester.writeXml(lexRes);

        try {
            Diversicons.validateXml(xml, LOG);
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidXmlException ex){
            
        }                
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlWrongSynsetRelationInternalTarget() throws IOException{
        
        LexicalResource lexRes = 
                lmf()
                .lexicon()
                .synset()
                .lexicalEntry()
                .synset()
                .synsetRelation("a", 3)
                .build();
                               
        File xml = DivTester.writeXml(lexRes);

        LOG.debug("\n" + FileUtils.readFileToString(xml));        
        
        try {
            Diversicons.validateXml(xml, LOG);
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidXmlException ex){
            
        }                
    }    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testValidateXmlWrongSenseInternalSynset() throws IOException{
        
        LexicalResource lexRes = LmfBuilder.simpleLexicalResource();
        
        Synset syn = new Synset();
        syn.setId(tid("syn666"));
        lexRes.getLexicons().get(0).getLexicalEntries().get(0).getSenses().get(0).setSynset(syn);
                               
        File xml = DivTester.writeXml(lexRes);

        LOG.debug("\n" + FileUtils.readFileToString(xml));        
        
        try {
            Diversicons.validateXml(xml, LOG);
            Assert.fail("Shouldn't arrive here!");
        } catch (InvalidXmlException ex){
            
        }                
    }    
    
    /**
     * Asserts one fatal error occurred
     * 
     * @since 0.1.0
     */
    private static void assertFatal(InvalidXmlException ex) {
        DivXmlHandler errorHandler = ex.getErrorHandler();
        assertTrue(errorHandler.isFatal());
        assertTrue(errorHandler.issuesCount() >= 1);
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
        assertFalse(p.matcher(" a").matches());
        assertFalse(p.matcher("\t").matches());
        assertFalse(p.matcher("-").matches());
        assertFalse(p.matcher("_").matches());
        assertFalse(p.matcher("2").matches());
        assertTrue(p.matcher("a").matches());
        assertFalse(p.matcher(".").matches());
        assertTrue(p.matcher("a.b").matches());
        assertFalse(p.matcher("a:b").matches());
        assertFalse(p.matcher("a:").matches());
        assertTrue(p.matcher("a-").matches());
        assertTrue(p.matcher("æ").matches()); // unicode *letters* are allowed
        assertFalse(p.matcher("€").matches()); // unicode *symbols* are *not* allowed
    }
 
    /**
     * Shows we are much more permissive with id names than with prefixes.
     * 
     * @since 0.1.0
     */
    @Test
    public void testIdPattern(){
        Pattern p = Diversicons.ID_PATTERN;
        
        assertFalse(p.matcher("t").matches());        
        assertFalse(p.matcher(pid("t"," a")).matches());
        assertTrue(p.matcher(pid("t","a")).matches());
        assertFalse(p.matcher(pid("t","\t")).matches());
        assertFalse(p.matcher(pid("t","-")).matches());
        assertFalse(p.matcher(pid("t","_")).matches());
        assertFalse(p.matcher(pid("t","2")).matches());
        assertTrue(p.matcher(pid("t","a2")).matches());
        assertFalse(p.matcher(pid("t",".")).matches());
        assertTrue(p.matcher(pid("t","a.b")).matches());
        assertFalse(p.matcher(pid("t","a:b")).matches());
        assertFalse(p.matcher(pid("t","a:")).matches());
        assertTrue(p.matcher(pid("t","a-")).matches());
        assertTrue(p.matcher(pid("a","æ")).matches()); // unicode *letters* are allowed
        assertTrue(p.matcher(pid("æ","æ")).matches()); 
        assertFalse(p.matcher(pid("a","€")).matches()); // unicode *symbols* are *not* allowed

        // 
        assertFalse(p.matcher("a").matches()); // currently we don't support non-prefixed ids.
    }
 
    /**
     * For comments, see  https://github.com/DavidLeoni/diversicon/issues/20
     * 
     * @since 0.1.0
     */
    @Test
    public void testWriteLexResToXml() throws IOException, DocumentException {

        // 3 dag already augmented
        LexicalResource res = lmf().lexicon()
        .synset()
        .lexicalEntry()
        .synset()
        .synsetRelation(ERelNameSemantics.HYPONYM, 1)        
        .synsetRelation(ERelNameSemantics.HYPERNYM, 1, 2)
        .provenance(Diversicon.getProvenanceId())  // included even if provenance = "div" because canonical 
        .synset()
        .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
        .synsetRelation(ERelNameSemantics.HYPERNYM, 1) // skipped, provenance = "div" and depth > 1
        .depth(2)
        .provenance(Diversicon.getProvenanceId())
        .build();
        
        File xml = DivTester.writeXml(res);
        
        String str = FileUtils.readFileToString(xml, "UTF-8");
        LOG.debug("\n" +str);
        
        assertTrue(!str.contains("DivSynsetRelation"));

        SAXReader reader = new SAXReader();
        Document document = reader.read(xml);

        assertTrue(!str.contains(ERelNameSemantics.HYPONYM));
        assertTrue(str.contains(ERelNameSemantics.HYPERNYM));
        
        // this should be filtered, we don't want edges from the transitive closure
        assertEquals(0, document
                .selectNodes("//Synset[@id='test:synset 3']/SynsetRelation[@target='test:synset 1']" )
                 .size());
    }
    
    /**
     * 
     * @since 0.1.0
     */
    @Test
    public void testWriteLexResToXmlWithNamespaces() throws IOException, DocumentException {
                
        String pref2 = DEFAULT_TEST_PREFIX + "-2";
        
        LexResPackage pack = new LexResPackage();
        
        pack.setName(DEFAULT_TEST_PREFIX);
        pack.setPrefix(DEFAULT_TEST_PREFIX);
        pack.setLabel(DivTester.GRAPH_1_HYPERNYM.getName());
        pack.setNamespaces(Internals.newMap(
                DEFAULT_TEST_PREFIX, "url-1",
                pref2, "url-2"));
        
        File xml = DivTester.writeXml(DivTester.GRAPH_1_HYPERNYM, pack);
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(xml);
                
        String str = FileUtils.readFileToString(xml, "UTF-8");
        LOG.debug(str);
        
        assertEquals(1, document
                    .selectNodes("//LexicalResource[namespace::*[.='url-1'] "
                                 + " and namespace::*[.='url-2'] ]" )
                     .size());
        
        // using just string matching because can't select xmlns stuff: https://www.oxygenxml.com/forum/topic4845.html 
        assertTrue(str.contains("xmlns:test=\"url-1\""));
        assertTrue(str.contains("xmlns:test-2=\"url-2\""));
        assertTrue(str.contains("xmlns:xsi="));
        assertTrue(str.contains("xsi:schemaLocation"));
    } 
    

    
    /**
     * @deprecated 
     * @throws IOException
     * 
     * @since 0.1.0
     */
    @Test
    public void testParseDtd() {
        
        String dtd = Internals.readData(Diversicons.DIVERSICON_DTD_1_0_CLASSPATH_URL)
                    .streamToString();
        DTDGrammar g = Internals.parseDtd(dtd);
        g.printElements();
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        LOG.debug("Diversicon DTD:\n ");
        
        while (g.getElementDecl(elementDeclIndex++, elementDecl)) {            
            LOG.debug("element decl: "+elementDecl.name+
                               ", "+ elementDecl.name.rawname  );
            LOG.debug(g.getContentSpecAsString(elementDeclIndex-1));
        }
        
        int contentDeclIndex = 0;
        XMLContentSpec contentSpec = new XMLContentSpec();
        
        
/*        while (g.getContentSpecAsString(elementDeclIndex)(contentDeclIndex++, contentSpec)) {            
            LOG.debug("content spec: "+contentSpec.type);
        }

        while (g.getNotationDecl(notationDeclIndex, notationDecl)ContentSpec(contentDeclIndex++, contentSpec)) {            
            LOG.debug("content spec: "+contentSpec.type);
        }*/
        
        
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testGenerateXmlSchema() throws IOException {
        
        File xsd = new File("target","diversicon-1.0-SNAPSHOT.xsd");
        
        // extra security check
        if (xsd.getAbsolutePath().endsWith("target/diversicon-1.0-SNAPSHOT.xsd")){
            FileUtils.deleteQuietly(xsd);
            LOG.info("cleaned " + xsd.getAbsolutePath());
        }
        
        Internals.generateXmlSchemaFromDtd(xsd);
        
        LOG.debug("GENERATED SCHEMA IS:\n" + FileUtils.readFileToString(xsd));
        
        File f = Internals.readData(Examplicon.XML_URI).toTempFile();
                
        Internals.validateXml(f, xsd, LOG, -1);        
        
    }
    
    
    /** 
     * @since 0.1.0
     */
    @Test
    public void testTransformXml() throws IOException{
        
        String xquery = Diversicons.XQUERY_UPDATE_PROLOGUE
                + "  for $a in $root//a"
                + "  return replace value of node $a with 'b'"
                + Diversicons.XQUERY_UPDATE_END;
        
        File dir = DivTester.createTestDir().toFile();
        File inXml = new File(dir, "in.xml");
        File outXml = new File(dir, "out.xml");
        
        FileUtils.writeStringToFile(inXml, "<a></a>");        
        
        Diversicons.transformXml(xquery, inXml, outXml);
        
        assertTrue(outXml.exists());
        
        String output = FileUtils.readFileToString(outXml);
        assertEquals("<a>b</a>", output);
    }
   
}