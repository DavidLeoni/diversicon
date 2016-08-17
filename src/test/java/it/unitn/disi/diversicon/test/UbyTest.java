package it.unitn.disi.diversicon.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;
import static it.unitn.disi.diversicon.test.DivTester.GRAPH_1_HYPERNYM;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests to show Uby corner cases or bugs to report.
 * 
 * @since 0.1.0
 *
 */
public class UbyTest {

    private static final Logger LOG = LoggerFactory.getLogger(UbyTest.class);

    private DBConfig dbConfig;

    @Rule 
    public TemporaryFolder folder= new TemporaryFolder();

    
    @Before
    public void beforeMethod() {
        dbConfig = DivTester.createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }

    /**
     * Uby bug to report. Shows getSynsetById as of Uby 0.7 throws
     * IndexOutOfBoundsException
     * 
     * See https://github.com/DavidLeoni/diversicon/issues/7
     * 
     * @see #testGetSynsetIterator()
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetByNonExistingId() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig);
        Uby uby = new Uby(dbConfig);

        try {
            uby.getSynsetById("666");
            Assert.fail("Shouldn't arrive here!");
        } catch (IndexOutOfBoundsException ex) {

        }

        uby.getSession()
           .close();
    }

    /**
     * Seems like calling Uby.getSynsetIterator(null).next()  always throws NullPointerException:
     * 
     * See https://github.com/DavidLeoni/diversicon/issues/16
     * 
     * @see #testGetSynsetByNonExistingId()
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetIterator() throws FileNotFoundException  {

        LMFDBUtils.createTables(dbConfig);
                
        LexicalResource lexRes = lmf().lexicon()
                .synset()
                .build();
       
        importIntoUby(lexRes);
                
        Uby uby = new Uby(dbConfig);
        try {
            Iterator<Synset> iter = uby.getSynsetIterator(null);
            iter.next();
            Assert.fail("Seems bug was solved!");

        } catch (NullPointerException ex) {

        }
        

        uby.getSession()
           .close();
    }
    
    
    /**    
     * Transforms res into an XML and then imports it into UBY
     * @since 0.1.0
     */
    private File importIntoUby(LexicalResource lexRes) {
        File xml;
        try {
             xml = DivTester.writeXml(lexRes);

            new XMLToDBTransformer(dbConfig).transform(xml, null);
            return xml;
        } catch (Exception ex) {
            throw new DivException("Error while import file into Uby!", ex);
        }
    }

    /**
     * 
     * Demonstrates you can't import twice a lexicon with the same id with
     * uby 0.7.0 xml transformer, even if you explicitly select an existing
     * lexical resource, see https://github.com/DavidLeoni/diversicon/issues/6
     * 
     * See also {@link DiversiconTest#testCantMergeSameLexicon()}
     * 
     * @since 0.1.0
     */
    @Test
    public void testCantMergeSameLexicon() throws FileNotFoundException, IllegalArgumentException, DocumentException {

        LMFDBUtils.createTables(dbConfig);
        
        File xml = importIntoUby(GRAPH_1_HYPERNYM);       

        try {
            new XMLToDBTransformer(dbConfig).transform(xml, GRAPH_1_HYPERNYM.getName());
        } catch (ConstraintViolationException ex) {
            assertTrue(ex.getMessage()
                         .contains(
                                 "Unique index or primary key violation: \"PRIMARY_KEY_2 ON PUBLIC.LEXICON(LEXICONID)"));
        }

    }

    /**
     * 
     * Shows that if you import twice a synset, the second one gets skipped,
     * showing on error to {@code System.err}. Notice the error is generated by
     * Hibernate,
     * *NOT* by XML validation, which is <b><a href=
     * "https://github.com/dkpro/dkpro-uby/blob/de.tudarmstadt.ukp.uby-0.7.0/de.tudarmstadt.ukp.uby.persistence.transform-asl/src/main/java/de/tudarmstadt/ukp/lmf/transform/XMLToDBTransformer.java#L88"
     * target="_blank">never performed </a> </b>
     * 
     * @throws SAXException
     * 
     * @since 0.1.0
     */
    @Test
    public void testSkipDuplicateSynsetId()
            throws IllegalArgumentException, DocumentException, IOException, SAXException {

        LMFDBUtils.createTables(dbConfig);

        LexicalResource lexRes = lmf().lexicon()
                                      .synset(1)
                                      .synset(1)
                                      .build();

        importIntoUby(lexRes);
        
    }

    /**
     * Tries to import xml with myAttr=666
     * 
     * @throws IOException
     * @throws DocumentException
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    @Test
    public void testInvalidXml() throws IOException, IllegalArgumentException, DocumentException {

        LMFDBUtils.createTables(dbConfig);

        final String INVALID = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<LexicalResource name=\"lexical resource 1\" myAttr=\"666\">   \n"
                + "     <Lexicon id=\"lexicon 1\">       \n"
                + "         <Synset id=\"synset 1\"/>    \n"
                + "     </Lexicon>                       \n"
                + "</LexicalResource>";

        File xml = new File(DivTester.createTestDir()
                                     .toFile()
                + "test.xml");
        FileUtils.writeStringToFile(xml, INVALID, "UTF-8");
        new XMLToDBTransformer(dbConfig).transform(xml, null);

    }
/**
 * @since 0.1.0 
 */
    @Test
    public void testOwaToUby() throws DocumentException, TransformerException, IOException {

        SAXReader reader = new SAXReader(false);
        Document document = reader.read("src/test/resources/owa-test.xml");
        String stylesheet = "src/main/resources/owa-to-uby.xslt";

        // load the transformer using JAXP
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(
                new StreamSource(stylesheet));

        // now lets style the given document
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);

        // return the transformed document
        Document transformedDoc = result.getDocument();

        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter sw = new StringWriter();
        XMLWriter writer = new XMLWriter(sw, format);
        writer.write(transformedDoc);

        LOG.debug("\n" + sw.toString());

    }

    /**
     * Inputs:
     * 
     * <pre>
     *      e FILEPATH : Extracts relType from SynsetRelation rows of OWA XML at FILEPATH
     * </pre>
     *
     * @since 0.1.0
     */
    public static void main(String[] args) {

        switch (args[0]) {
        case "e":
            try {
                File file = new File(args[1]);

                Pattern p = Pattern.compile("(.*)<SynsetRelation(.*)relType='(.*)'/>");

                HashSet<String> set = new HashSet();

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher m = p.matcher(line);
                        if (m.matches()) {
                            set.add(m.group(3));
                        }
                    }
                }

                for (String s : set) {
                    System.out.println(s);
                }
            } catch (Exception ex) {
                throw new DivException(ex);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown command: " + args[0]);
        }

    }
    
    /**
     * Shows you can import a synset relation pointing to a non-existing synset, and 
     * such synset is not automatically created in the db. Still when 
     * in the API there is Synset to return, Hibernate  creates a 
     * Java object on the fly.
     *
     * @since 0.1.0
     */
    @Test
    public void testSynsetRelationNonExistingTarget() throws FileNotFoundException {

        Diversicons.createTables(dbConfig); // Diversicons, otherwise
                                            // JavaTransformer
                                            // complains about missing
                                            // DivSynsetRelation column

        LexicalResource lexRes = lmf().lexicon()
                                      .synset()
                                      .build();
        
        Synset syn = lexRes.getLexicons()
                           .get(0)
                           .getSynsets()
                           .get(0);

        SynsetRelation sr = new SynsetRelation();
        sr.setRelName("a");
        sr.setRelType(ERelTypeSemantics.label);
        sr.setSource(syn);

        Synset syn2 = new Synset();
        syn2.setId("synset 2");

        sr.setTarget(syn2);

        syn.setSynsetRelations(Internals.newArrayList(sr));

        importIntoUby(lexRes);        
        
        Uby uby = new Uby(dbConfig);
               
        Synset retSyn1 = uby.getSynsetById("synset 1");                
        
        // second synset is not created !
        assertEquals(1, uby.getLexicons().get(0).getSynsets().size());
        
        List<SynsetRelation> synRels = retSyn1.getSynsetRelations();
        checkNotNull(synRels);        
        assertEquals(1, synRels.size());
        SynsetRelation retSynRel = synRels.get(0); 
        Synset retSyn2 = retSynRel.getTarget();
        // shows Hibernate here does create the synset object 
        assertEquals("synset 2", retSyn2.getId());

    }


    
  
}
