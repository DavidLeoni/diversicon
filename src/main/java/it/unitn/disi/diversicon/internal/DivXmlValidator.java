package it.unitn.disi.diversicon.internal;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.DivXmlHandler;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.LexResPackage;

// from here
// http://www.java2s.com/Tutorials/Java/XML/SAX/Output_line_number_for_SAX_parser_event_handler_in_Java.htm
class DivXmlValidator extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DivXmlValidator.class);
    
    private LexResPackage pack;
    
    @Nullable
    private Locator locator;
    
    private DivXmlHandler errorHandler;
    

    /**
     * @since 0.1.0
     */
    private DivXmlValidator(LexResPackage pack, DivXmlHandler errorHandler) {
        super();
        checkNotNull(pack);
        checkNotNull(errorHandler);
        this.pack = pack;
        this.errorHandler = errorHandler;

    }

    /**
     * @since 0.1.0
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
       
        String n = qName;

        if ("LexicalResource".equals(n)) {
            
            pack.setLabel(attrs.getValue("name"));
            pack.setId(attrs.getValue("id"));
            pack.setPrefix(attrs.getValue("prefix"));                        

            for (int i = 0; i < attrs.getLength(); i++) {
                
                String qname = attrs.getQName(i);                
                
                if (qname.startsWith("xmlns:")) {
                    
                    pack.putNamespace(qname.split(":")[1], attrs.getValue(i));
                }
            }

        } else {

            // Would have liked to get it from class, but if we
            // assume xml schema pass was done before it should still be ok

            @Nullable
            String id = attrs.getValue("id");
            if (id != null) {                                
                if (!id.startsWith(pack.getPrefix() + Diversicons.NAMESPACE_SEPARATOR)) {
                    errorHandler.error(new SAXParseException("Found " + n + " id " + id
                            + " not starting with LexicalResource prefix '" + pack.getPrefix() + "'", 
                            locator));
                }
            }

        }
    }

    public static void validate(File file, DivXmlHandler errorHandler) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
            parser.getXMLReader()
                  .setErrorHandler(errorHandler);
            DivXmlValidator handler = new DivXmlValidator(new LexResPackage(), errorHandler);
            // parser.parse("xmlFileName.xml", handler);

            InputSource is = new InputSource(new FileInputStream(file));

            parser.parse(is, handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            throw new DivException("Something went wrong!", e);
        }

    }

}
