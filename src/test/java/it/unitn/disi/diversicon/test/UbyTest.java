package it.unitn.disi.diversicon.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;

import static it.unitn.disi.diversicon.test.DivTester.GRAPH_1_HYPERNYM;
import static it.unitn.disi.diversicon.test.DivTester.checkDb;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertNotNull;
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
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetById() throws FileNotFoundException {

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
     * 
     * @param lexicalResourceName
     *            the name of an existing lexical resource into which merge.
     *            If it doesn't exist or null, a new lexical resource is created
     *            using name attribute found
     *            in the XML (thus provided {@code lexicaResourceName} will be
     *            ignored).
     * 
     * @since 0.1.0
     */
    private void importIntoUby(LexicalResource res, @Nullable String lexicalResourceName) {

        try {
            File xml = DivTester.writeXml(res);

            new XMLToDBTransformer(dbConfig).transform(xml, null);
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

        File xml = DivTester.writeXml(GRAPH_1_HYPERNYM);

        new XMLToDBTransformer(dbConfig).transform(xml, null);

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

        File xml = new File(DivTester.createTestDir()
                                     .toFile()
                + "test.xml");

        LMFXmlWriter writer = new LMFXmlWriter(xml.getAbsolutePath(), null);

        writer.writeElement(lexRes);
        writer.writeEndDocument();

        LOG.debug("\n" + FileUtils.readFileToString(xml));

        new XMLToDBTransformer(dbConfig).transform(xml, null);

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
     * 'e' FILEPATH : Extracts relType from SynsetRelation rows of OWA XML
     * files.
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

}
