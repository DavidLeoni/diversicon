package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.exceptions.InvalidImportException;
import eu.kidf.diversicon.core.exceptions.InvalidXmlException;
import eu.kidf.diversicon.data.DivUpper;

/**
 * 
 * Validates an XML.
 * 
 * <p>
 * It remembers the number of times it has been launched to
 * perform finer and finer validation :
 * 
 * <ol>
 * <li>Validate structure, collect ids
 * <li>Verify internal ids links, external ids syntax</li>
 * <li>Verify external ids against the db</li>
 * </ol>
 * </p>
 * 
 * 
 * @since 0.1.0
 *
 */
// Also made to overcome poor implementation of Xerces Xml Schema 1.1
// assertions,
// see https://github.com/diversicon-kb/diversicon/issues/21
// from here
// http://www.java2s.com/Tutorials/Java/XML/SAX/Output_line_number_for_SAX_parser_event_handler_in_Java.htm
public class DivXmlValidator extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DivXmlValidator.class);

    /**
     * @since 0.1.0
     */
    private static final int STEP_1 = 1;

    /**
     * @since 0.1.0
     */
    private static final int STEP_2 = 2;

    /**
     * @since 0.1.0
     */
    private static final int STEP_3 = 3;

    private LexResPackage pack;

    @Nullable
    private Locator locator;

    private DivXmlHandler errorHandler;

    private int step;

    private Map<String, String> tagIds;

    /**
     * Needed only in step 3
     */
    private @Nullable Diversicon diversicon;

    /**
     * Needed only in step 3
     */
    private @Nullable ImportConfig importConfig;
    
    
    /**
     * @param pack
     *            the package to fill with metadata extracted from the XML.
     * @param errorHandler
     *            the error handler to fill with reports
     * 
     * @since 0.1.0
     */
    public DivXmlValidator(
            LexResPackage pack,
            DivXmlHandler errorHandler) {
        super();
        checkNotNull(pack);
        checkNotNull(errorHandler);
        this.pack = pack;
        this.errorHandler = errorHandler;
        this.step = STEP_1;
        this.tagIds = new HashMap<>();
        this.diversicon = null;
        this.errorHandler = null;
    }

    public DivXmlValidator(
            LexResPackage pack,
            DivXmlHandler errorHandler,
            Diversicon diversicon,
            ImportConfig importConfig) {

        this(pack, errorHandler);

        checkNotNull(diversicon);
        checkNotNull(importConfig);

        this.diversicon = diversicon;
        this.importConfig = importConfig;
    }

    /**
     * Returns the error handler used for validating.
     * 
     * @since 0.1.0
     */
    public DivXmlHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * @since 0.1.0
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * 
     * See {@link #error(DivValidationError, String, Exception)}
     * 
     * @since 0.1.0
     */
    private void error(DivValidationError xmlValidationError,
            String msg) throws SAXException {

        error(xmlValidationError, msg, null);
    }

    /**
     * @since 0.1.0
     */
    private void error(DivValidationError xmlValidationError,
            String msg,
            @Nullable Exception ex) throws SAXException {

        String fmsg = String.valueOf(xmlValidationError) + ": " + msg;
        if (ex == null) {
            errorHandler.error(new SAXParseException(fmsg, locator));
        } else {
            errorHandler.error(new SAXParseException(fmsg, locator, ex));
        }
    }

    /**
     * See {@link #fatalError(DivValidationError, String, Exception)}
     * 
     * @since 0.1.0
     */
    private void fatalError(DivValidationError xmlValidationError,
            String msg) throws SAXException {
        fatalError(xmlValidationError, msg, null);
    }

    /**
     * @since 0.1.0
     */
    private void fatalError(DivValidationError xmlValidationError,
            String msg,
            @Nullable Exception ex) throws SAXException {

        String fmsg = String.valueOf(xmlValidationError) + ": " + msg;
        if (ex == null) {
            errorHandler.fatalError(new SAXParseException(fmsg, locator));
        } else {
            errorHandler.fatalError(new SAXParseException(fmsg, locator, ex));
        }
    }

    /**
     * @since 0.1.0
     */
    private void warning(
            DivValidationError xmlValidationError,
            String msg,
            @Nullable Exception ex) {

        String fmsg = String.valueOf(xmlValidationError) + ": " + msg;

        if (ex == null) {
            errorHandler.warning(new SAXParseException(fmsg, locator));
        } else {
            errorHandler.warning(new SAXParseException(fmsg, locator, ex));
        }
    }

    /**
     * See {@link #warning(DivValidationError, String, Exception)}
     * 
     * @since 0.1.0
     */
    private void warning(
            DivValidationError xmlValidationError,
            String msg) {

        warning(xmlValidationError, msg, null);
    }

    /**
     * @since 0.1.0
     */
    @Override
    public void startDocument() throws SAXException {

        if (this.step == STEP_3) {
            if (this.diversicon == null) {
                throw new IllegalStateException(
                        "Trying to do a third validation pass but no diversicon connection was specified!");
            }
            if (this.importConfig == null) {
                throw new IllegalStateException(
                        "Trying to do a third validation pass but no import config was specified!");
            }

            LOG.info("Validating import data against the db....");

            List<ImportJob> jobs = diversicon.getImportJobs();

            for (ImportJob job : jobs) {

                if (pack.getName()
                        .equals(job.getLexResPackage()
                                   .getName())) {
                    fatalError(DivValidationError.NAMESPACE_CLASH,
                            "Tried to import a lexical resource which has the same name of "
                                    + " an already imported lexical resource!");
                }

                if (Objects.equals(pack.getPrefix(), job.getLexResPackage()
                                                        .getPrefix())) {
                    fatalError(DivValidationError.NAMESPACE_CLASH,
                            "Tried to import a lexical resource which has the same"
                                    + " prefix of an already imported resource: " + job.getLexResPackage()
                                                                                       .getName());
                }
            }

            Map<String, String> dbNss = diversicon.getNamespaces();

            for (String prefix : pack.getNamespaces()
                                     .keySet()) {
                if (dbNss.containsKey(prefix)) {
                    String urlInDb = dbNss.get(prefix);
                    String urlToImport = pack.getNamespaces()
                                             .get(prefix);
                    if (!Objects.equals(urlInDb, urlToImport)) {
                        fatalError(DivValidationError.NAMESPACE_CLASH,
                                "Tried to import a prefix which is assigned to another resource url in the db!"
                                        + "\n  Prefix        = " + prefix
                                        + "\n  url to import = " + urlToImport
                                        + "\n  url in the db = " + urlInDb);
                    }
                }

                if (!dbNss.containsKey(prefix)) {
                    String msg = "Prefix '" + prefix + "' is not present in database prefixes!";
                    
                    warning(DivValidationError.INVALID_PREFIX, msg);
                    
                }
            }
        }
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

        final String sep = Diversicons.NAMESPACE_SEPARATOR;

        switch (step) {
        case STEP_1:
            if ("LexicalResource".equals(tagName)) {

                String name = attrs.getValue("name");

                pack.setName(name);
                pack.setPrefix(attrs.getValue("prefix"));

                for (int i = 0; i < attrs.getLength(); i++) {

                    String qname = attrs.getQName(i);

                    if (qname.startsWith("xmlns:")) {

                        String prefix = qname.split(":")[1];
                        String ns = attrs.getValue(i);

                        if (!Diversicons.NAMESPACE_PREFIX_PATTERN.matcher(prefix)
                                                                 .matches()) {
                            this.error(DivValidationError.INVALID_PREFIX, "Invalid prefix '"
                                    + prefix + "', it must match "
                                    + Diversicons.NAMESPACE_PREFIX_PATTERN.toString());
                        }

                        try {
                            Diversicons.checkNamespace(ns);
                        } catch (Exception ex) {
                            this.error(DivValidationError.INVALID_PREFIX, "Invalid namespace for prefix '"
                                    + prefix + "': '" + ns + '"');
                        }

                        if (DivUpper.of()
                                    .getPrefix()
                                    .equals(prefix)) {
                            if (!DivUpper.of()
                                         .namespace()
                                         .equals(ns)) {
                                error(DivValidationError.INVALID_DIVUPPER_NAMESPACE,
                                        "Invalid DivUpper namespace. Expected: \n" +
                                                DivUpper.of()
                                                        .namespace()
                                                + "\nFound instead: " + ns);
                            }
                        }

                        pack.putNamespace(prefix, ns);
                    }
                }
                if (!tagIds.containsKey(name)) {
                    tagIds.put(name, tagName);
                }

            } else {

                // Would have liked to get it from class, but if we
                // assume xml schema pass was done before it should still be ok

                @Nullable
                String id = attrs.getValue("id");
                if (id != null) {
                    if (!id.startsWith(pack.getPrefix() + sep)) {
                        errorHandler.error(new SAXParseException(DivValidationError.INVALID_INTERNAL_ID
                                + ": Found " + tagName + " id " + id
                                + " not starting with LexicalResource prefix '" + pack.getPrefix() + "'",
                                locator));
                    }
                    if (!tagIds.containsKey(id)) {
                        tagIds.put(id, tagName);
                    }
                }
            }

            break;
        case STEP_2:

            // todo crude, need to find smarter ways with reflection..
            if ("SynsetRelation".equals(tagName)) {
                validateRefAgainstXml(calcProv(tagName, "target"), attrs.getValue("target"), "Synset");
            }
            if ("Sense".equals(tagName)) {
                validateRefAgainstXml(calcProv(tagName, "synset"), attrs.getValue("synset"), "Synset");
            }

            // Commented as problematic for now, think about DivUpper SenseAxis
            // See https://github.com/diversicon-kb/diversicon-core/issues/36
            //
            // if ("SenseAxis".equals(tagName)) {
            // validateTargetId(tagName ,"senseOne", attrs.getValue("senseOne"),
            // "Sense");
            // validateTargetId(tagName ,"synsetOne",
            // attrs.getValue("synsetOne"), "Synset");
            // validateTargetId(tagName ,"lexiconOne",
            // attrs.getValue("lexiconOne"), "Lexicon");

            // validateTargetId(tagName ,"synsetTwo",
            // attrs.getValue("synsetTwo"), "Synset");
            // validateTargetId(tagName ,"senseTwo", attrs.getValue("senseTwo"),
            // "Sense");
            // validateTargetId(tagName ,"lexiconTwo",
            // attrs.getValue("lexiconTwo"), "Lexicon");
            // }

            break;
        case STEP_3: // checks against db

            if ("SynsetRelation".equals(tagName)) {
                validateRefAgainstDb(calcProv(tagName, "target"), attrs.getValue("target"), "Synset");
            }
            if ("Sense".equals(tagName)) {
                validateRefAgainstDb(calcProv(tagName, "synset"), attrs.getValue("synset"), "Synset");
            }

            break;

        default:
            LOG.warn("Tried to do an unsupported step: " + this.step);
        }
    }

    /**
     * Returns an informative provenance description
     * 
     * @since 0.1.0
     */
    private static String calcProv(String tagName, String attr) {
        return tagName + "." + attr;
    }

    /**
     * See
     * {@link #errorRef(String, DivValidationError, String, String, Exception)}
     * 
     * @since 0.1.0
     */
    private void errorRef(
            String id,
            DivValidationError validationCode,
            String prov,
            String details) throws SAXException {
        errorRef(id, validationCode, prov, details, null);
    }

    /**
     * See
     * {@link #warningRef(String, DivValidationError, String, String, Exception)}
     * 
     * @since 0.1.0
     */
    private void warningRef(
            String id,
            DivValidationError validationCode,
            String prov,
            String details) {
        warningRef(id, validationCode, prov, details, null);
    }

    /**
     * @param prov
     *            The informative provenance of the id, something like
     *            'SynsetRelation.target'
     * @since 0.1.0
     */
    private void errorRef(String id,
            DivValidationError valCode,
            String prov,
            String details,
            @Nullable Exception ex) throws SAXException {

        String msg = valCode + ": " + prov + " id " + id + " : " + details;

        error(valCode, msg, ex);

    }

    /**
     * @param prov
     *            The informative provenance of the id, something like
     *            'SynsetRelation.target'
     * @since 0.1.0
     */
    private void warningRef(String id,
            DivValidationError valCode,
            String prov,
            String details,
            @Nullable Exception ex) {
        String msg = valCode + ": " + prov + " id " + id + " : " + details;
        warning(valCode, msg, ex);
    }

    /**
     * @param msg
     * @param targetId
     * @throws SAXException
     * 
     * @since 0.1.0
     */
    private void validateRefAgainstDb(String prov, String targetId, String targetTag)
            throws SAXException {

        checkNotEmpty(prov, "Invalid prov!");
        checkNotEmpty(targetTag, "Invalid target tag!");

        if (this.step == STEP_3) {
            switch (targetTag) {
            case "Synset":
                try {
                    this.diversicon.getSynsetById(targetId);
                } catch (Exception ex) {
                    String details = "Db does not contain referenced synset!";
                    
                    warningRef(targetId,
                                DivValidationError.MISSING_EXTERNAL_ID,
                                prov,
                                details);                                           

                }
            case "Sense":
                try {
                    this.diversicon.getSenseById(targetId);
                } catch (Exception ex) {
                    String details = "Db does not contain referenced sense!";

                    warningRef(targetId,
                            DivValidationError.MISSING_EXTERNAL_ID,
                            prov,
                            details);

                }

            }

        }

    }

    /**
     * @param msg
     * @param targetId
     * @throws SAXException
     * 
     * @since 0.1.0
     */
    private void validateRefAgainstXml(String prov, String targetId, String targetTag)
            throws SAXException {

        String prefix = null;

        try {
            prefix = Diversicons.namespacePrefixFromId(targetId);

        } catch (IllegalArgumentException ex) {
            errorRef(targetId, DivValidationError.INVALID_PREFIX, prov, "Invalid prefix !", ex);
            return;
        }

        if (!(pack.getNamespaces()
                  .keySet()
                  .contains(prefix)
                || Diversicons.DEFAULT_NAMESPACES.keySet()
                                                 .contains(prefix))) {
            errorRef(targetId,
                    DivValidationError.UNDECLARED_NAMESPACE,
                    prov,
                    "Found undeclared prefix in id!");
        }

        if (prefix.equals(pack.getPrefix())) { // internal
            if (!targetTag.equals(tagIds.get(targetId))) {
                errorRef(targetId,
                        DivValidationError.MISSING_INTERNAL_ID,
                        prov,
                        "XML does not contain referenced synset! ");
            }
        }

    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        step += 1;
    }

    /**
     * Returns an unmodifiable map of found ids, in the form id -> xmltag
     * 
     * @since 0.1.0
     */
    public Map<String, String> getTagIds() {
        return Collections.unmodifiableMap(tagIds);
    }

    /**
     * @since 0.1.0
     */
    public LexResPackage getLexResPackage() {
        return pack;
    }

    /**
     * 
     * Sets required parameters for third pass.
     * 
     * @since 0.1.0
     */
    public void prepareThirdPass(Diversicon diversicon, ImportConfig importConfig) {
        checkNotNull(diversicon);
        checkNotNull(importConfig);
        this.diversicon = diversicon;
        this.importConfig = importConfig;
    }

    /**
     * If validation failed, throws InvalidXmlException
     * 
     * @throws InvalidXmlException
     * 
     * @since 0.1.0
     */
    public void checkPassed() {

        String sysId = errorHandler.getDefaultSystemId();

        if (errorHandler.isFatal() || errorHandler.getErrorsCount() > 0) {
            errorHandler.getConfig()
                        .getLog()
                        .error("Invalid xml! " + errorHandler.summary() + " in " + sysId);
            throw new InvalidXmlException(errorHandler,
                    "Invalid xml! " + errorHandler.summary() + " in " + sysId
                            + "\n" + errorHandler.firstIssueAsString());
        } else {
            if (errorHandler.getWarningCount() > 0) {
                if (errorHandler.getConfig()
                                .isStrict()) {
                    errorHandler.getConfig()
                                .getLog()
                                .error("Found warnings during strict validation! " + errorHandler.summary() + " in "
                                        + sysId);
                    throw new InvalidXmlException(errorHandler,
                            "Found warnings during strict validation! " + errorHandler.summary() + " in "
                                    + sysId
                                    + "\n" + errorHandler.firstIssueAsString());

                } else {
                    errorHandler.getConfig()
                                .getLog()
                                .warn(errorHandler.summary() + " in "
                                        + sysId);
                }
            }
        }
    }
}
