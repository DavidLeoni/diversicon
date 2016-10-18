package it.unitn.disi.diversicon.internal;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import it.unitn.disi.diversicon.DivXmlHandler;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.LexResPackage;
import it.unitn.disi.diversicon.XmlValidationErrors;

/**
 * Made to overcome poor implementation of Xerces Xml Schema 1.1 assertions,
 * see https://github.com/DavidLeoni/diversicon/issues/21
 * 
 * It remembers the number of times it has been launched to
 * perform finer and finer validation.
 * 
 * @since 0.1.0
 *
 */
// from here
// http://www.java2s.com/Tutorials/Java/XML/SAX/Output_line_number_for_SAX_parser_event_handler_in_Java.htm
public class DivXmlValidator extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DivXmlValidator.class);

    /**
     * Validate structure, collect ids
     */
    private static final int FIRST_STEP = 1;

    /**
     * Verify internal ids
     */
    private static final int SECOND_STEP = 2;

    

    private LexResPackage pack;

    @Nullable
    private Locator locator;

    private DivXmlHandler errorHandler;

    private int step;

    Map<String, String> tagIds;

    /**
     * @since 0.1.0
     */
    public DivXmlValidator(LexResPackage pack, DivXmlHandler errorHandler) {
        super();
        checkNotNull(pack);
        checkNotNull(errorHandler);
        this.pack = pack;
        this.errorHandler = errorHandler;
        this.step = FIRST_STEP;
        this.tagIds = new HashMap<>();
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

        String tagName = qName;

        switch (step) {
        case FIRST_STEP:
            if ("LexicalResource".equals(tagName)) {

                String name = attrs.getValue("name");

                pack.setName(name);
                pack.setPrefix(attrs.getValue("prefix"));

                for (int i = 0; i < attrs.getLength(); i++) {

                    String qname = attrs.getQName(i);

                    if (qname.startsWith("xmlns:")) {

                        pack.putNamespace(qname.split(":")[1], attrs.getValue(i));
                    }
                }
                if (!tagIds.containsKey(name)){
                    tagIds.put(name, tagName);    
                }
                
            } else {

                // Would have liked to get it from class, but if we
                // assume xml schema pass was done before it should still be ok

                @Nullable
                String id = attrs.getValue("id");
                if (id != null) {
                    if (!id.startsWith(pack.getPrefix() + Diversicons.NAMESPACE_SEPARATOR)) {
                        errorHandler.error(new SAXParseException("Found " + tagName + " id " + id
                                + " not starting with LexicalResource prefix '" + pack.getPrefix() + "'",
                                locator));
                    }
                    if (!tagIds.containsKey(id)){
                        tagIds.put(id, tagName);
                    }
                }
            }

            break;
        case SECOND_STEP:
            if ("SynsetRelation".equals(tagName)) {
                String target = attrs.getValue("target");
                if (target != null
                        && target.startsWith(pack.getPrefix() + Diversicons.NAMESPACE_SEPARATOR)){
                    if (!"Synset".equals(tagIds.get(target))){
                        errorHandler.error(new SAXParseException(
                                XmlValidationErrors.SYNSET_RELATION_MISSING_INTERNAL_TARGET + ": SynsetRelation internal target '" + target + "' points "
                                + " to a non-existing Synset! ",
                                locator));
                    }                    
                }
            }
            if ("Sense".equals(tagName)) {
                String synset = attrs.getValue("synset");
                if (synset != null
                        && synset.startsWith(pack.getPrefix() + Diversicons.NAMESPACE_SEPARATOR)){
                    if (!"Synset".equals(tagIds.get(synset))){
                        errorHandler.error(new SAXParseException(
                                XmlValidationErrors.SENSE_MISSING_INTERNAL_SYNSET + ": Sense internal synset '" + synset + "' points "
                                + " to a non-existing Synset! ",
                                locator));
                    }
                }
            }
            break;
        default:
            throw new UnsupportedOperationException("Unsupported validation step: " + step + " !!");
        }
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        step += 1;
    }

}
