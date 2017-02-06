package eu.kidf.diversicon.core.test.experiments;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import java.io.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import eu.kidf.diversicon.core.DivXmlHandler;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.XmlValidationConfig;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.exceptions.InvalidXmlException;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.core.test.DivTester;
import eu.kidf.diversicon.data.Examplicon;

import org.apache.commons.io.FileUtils;
import org.apache.xerces.impl.Constants;
import org.basex.core.*;
import org.basex.core.cmd.*;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;

/**
 * Mostly copied from native Basex API examples:
 * https://github.com/BaseXdb/basex/blob/master/basex-examples/src/main/java/org
 * /basex/examples/local/RunQueries.java
 * 
 * @since 0.1.0
 */
public class XmlExperimentsTest {

    /**
     * @since 0.1.0
     */
    // transform with wasn't working with doc() transform with
    @Test
    public void testCopyModify() throws IOException, QueryException {

        String query = FileUtils.readFileToString(new File(XML_DIR + "basex-copy-modify.xql"),
                "UTF-8");

        // Process the query by using the database command
        LOG.info("\n* Use the database command:");

        try {
            query(query);
        } catch (Exception ex) {
            LOG.info("error", ex);
        }
        // Iterate through all query results
        LOG.info("\n* Serialize each single result:");

        serialize(query);

    }

    private static final Logger LOG = LoggerFactory.getLogger(XmlExperimentsTest.class);

    /**
     * @since 0.1.0
     */
    private static final String XML_DIR = DivTester.EXPERIMENTS_DIR + "xml/";

    /** Database context. */
    static Context context = new Context();

    /**
     * Runs the example code.
     * 
     * @throws IOException
     *             if an error occurs while serializing the results
     * @throws QueryException
     *             if an error occurs while evaluating the query
     * @throws BaseXException
     *             if a database command fails
     */
    @Test
    public void testBasex() throws IOException, QueryException {
        LOG.info("=== RunQueries ===");

        // Evaluate the specified XQuery
        String query = "for $x in doc('" + XML_DIR + "basex-1.xml')//li return data($x)";

        // Process the query by using the database command
        LOG.info("\n* Use the database command:");

        query(query);

        // Directly use the query processor
        LOG.info("\n* Use the query processor:");

        process(query);

        // Iterate through all query results
        LOG.info("\n* Serialize each single result:");

        serialize(query);

        // Iterate through all query results
        LOG.info("\n* Convert each result to its Java representation:");

        iterate(query);

        // Uncomment this line to see how erroneous queries are handled
        // iterate("for error s$x in . return $x");

    }

    /**
     * This method evaluates a query by using the database command.
     * The results are automatically serialized and printed.
     * 
     * @param query
     *            query to be evaluated
     * @throws BaseXException
     *             if a database command fails
     */
    static void query(final String query) throws BaseXException {
        LOG.info(new XQuery(query).execute(context));
    }

    /**
     * This method uses the {@link QueryProcessor} to evaluate a query.
     * The resulting items are passed on to a serializer.
     * 
     * @param query
     *            query to be evaluated
     * @throws QueryException
     *             if an error occurs while evaluating the query
     */
    static void process(final String query) throws QueryException {
        // Create a query processor
        try (QueryProcessor proc = new QueryProcessor(query, context)) {
            // Execute the query
            Value result = proc.value();

            // Print result as string.
            LOG.info(String.valueOf(result));
        }
    }

    /**
     * This method uses the {@link QueryProcessor} to evaluate a query.
     * The results are iterated one by one and converted to their Java
     * representation, using {{@link Item#toJava()}. This variant is especially
     * efficient if large result sets are expected.
     * 
     * @param query
     *            query to be evaluated
     * @throws QueryException
     *             if an error occurs while evaluating the query
     */
    static void iterate(final String query) throws QueryException {
        // Create a query processor
        try (QueryProcessor proc = new QueryProcessor(query, context)) {
            // Store the pointer to the result in an iterator:
            Iter iter = proc.iter();

            // Iterate through all items and serialize
            for (Item item; (item = iter.next()) != null;) {
                LOG.info(String.valueOf(item.toJava()));
            }
        }
    }

    /**
     * This method uses the {@link QueryProcessor} to evaluate a query.
     * The results are iterated one by one and passed on to an serializer.
     * This variant is especially efficient if large result sets are expected.
     * 
     * @param query
     *            query to be evaluated
     * @throws QueryException
     *             if an error occurs while evaluating the query
     * @throws IOException
     *             if an error occurs while serializing the results
     */
    static void serialize(final String query) throws QueryException, IOException {
        // Create a query processor
        try (QueryProcessor proc = new QueryProcessor(query, context)) {

            // Store the pointer to the result in an iterator:
            Iter iter = proc.iter();

            File f = Internals.createTempFile(DivTester.DEFAULT_TEST_PREFIX, ".xml")
                              .toFile();
            FileOutputStream sw = new FileOutputStream(f);

            // Create a serializer instance
            try (Serializer ser = proc.getSerializer(sw)) {
                // Iterate through all items and serialize contents
                for (Item item; (item = iter.next()) != null;) {

                    ser.serialize(item);
                }
            }
            sw.close();

            LOG.info("\n Transformed file is:\n" + FileUtils.readFileToString(f));
        }
    }

    /**
     * This version uses the java.xml.validation.Validator
     * 
     * @since 0.1.0
     */
    @Test
    @Ignore
    public void testXmlValidationValidator() {
        File xmlFile = Diversicons.readData(Examplicon.XML_URI)
                                .toTempFile();

        File xsdFile = Diversicons.readData(Diversicons.SCHEMA_1_0_CLASSPATH_URL, false)
                                .toTempFile();

        // if editor can't find the constant probably default xerces is being
        // used
        // instead of the one supporting schema 1.1

        SchemaFactory factory = SchemaFactory.newInstance(Constants.W3C_XML_SCHEMA11_NS_URI);
        File schemaLocation = xsdFile;
        Schema schema;
        try {
            schema = factory.newSchema(schemaLocation);
        } catch (SAXException e) {
            throw new DivException("Error while parsing schema!", e);
        }

        Source source = new StreamSource(xmlFile);
        DivXmlHandler errorHandler = new DivXmlHandler(
                XmlValidationConfig.of(LOG),
                source.getSystemId());

        Validator validator = schema.newValidator();

        validator.setErrorHandler(errorHandler);

        try {
            validator.validate(source);
        } catch (SAXException | IOException e) {
            throw new InvalidXmlException(errorHandler, "Fatal error while validating " + xmlFile.getAbsolutePath(), e);
        }
    }

    /**
     * Apparently there's no way I can make the parser skip validating the dtd.
     * I hate it hate it hate it
     * 
     * This version using the parser
     * 
     * @since 0.1.0
     * 
     */
    // <!DOCTYPE LexicalResource PUBLIC "-//DavidLeoni//Diversicon DTD based
    // upon UBY 0.7.0 DTD//EN"
    // "https://github.com/diversicon-kb/diversicon/blob/master/src/main/resources/diversicon-1.0.dtd">
    // @Test
    @Ignore
    public void testDamnedSkipDtdValidation() {

        File xmlFile = Diversicons.readData(Examplicon.XML_URI)
                                .toTempFile();

        checkNotNull(xmlFile);

        File xsdFile = Diversicons.readData(Diversicons.SCHEMA_1_0_CLASSPATH_URL, false)
                                .toTempFile();

        SchemaFactory factory = SchemaFactory.newInstance(Constants.W3C_XML_SCHEMA11_NS_URI);
        File schemaLocation = xsdFile;
        Schema schema;
        try {
            schema = factory.newSchema(schemaLocation);
        } catch (SAXException e) {
            throw new DivException("Error while parsing schema!", e);
        }

        Source source = new StreamSource(xmlFile);
                
        DivXmlHandler errorHandler = new DivXmlHandler(XmlValidationConfig.builder()
                .setLog(LOG)                
                .build(), source.getSystemId());

        /** Setup SAX parser for schema validation. */
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setSchema(schema);
        spf.setValidating(false);
        SAXParser parser;
        try {
            parser = spf.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            // TODO Auto-generated catch block
            throw new DivException("Something went wrong!", e);
        }

        try {
            XMLReader reader = parser.getXMLReader();
            reader.setDTDHandler(null);

            reader.setEntityResolver(new EntityResolver() {

                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return null;
                }

            });
            reader.setErrorHandler(errorHandler);

            reader.parse(new InputSource(new FileInputStream(xmlFile)));
        } catch (IOException | SAXException e) {
            // TODO Auto-generated catch block
            throw new DivException("Something went wrong!", e);
        }

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testXsd11Validation() throws SAXException, IOException {

        File xsdFile = new File(XML_DIR + "assertions-test.xsd");

        // if editor can't find the constant probably default xerces is being
        // used
        // instead of the one supporting schema 1.1

        SchemaFactory factory = SchemaFactory.newInstance(Constants.W3C_XML_SCHEMA11_NS_URI);
        File schemaLocation = xsdFile;
        Schema schema = factory.newSchema(schemaLocation);

        Validator validator = schema.newValidator();
        Source sourcePass = new StreamSource(new File(XML_DIR + "test-pass.xml"));
        validator.validate(sourcePass);

        Source sourceFail = new StreamSource(new File(XML_DIR + "test-fail.xml"));

        try {
            validator.validate(sourceFail);
            Assert.fail("Shouldn't arrive here!");
        } catch (SAXException ex) {

        }

    }
}