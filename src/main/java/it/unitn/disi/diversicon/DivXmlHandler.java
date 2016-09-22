package it.unitn.disi.diversicon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.xml.serialize.OutputFormat.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import it.disi.unitn.diversicon.exceptions.DivException;

/**
 * 
 * Redirects LMF XML validation errors to provided log, while keeping counts for
 * errors and warnings. For convenience, also caches first {@value #MAX_FIRST_ISSUES} encountered  
 * issue .
 * 
 * @since 0.1.0
 *
 */
public class DivXmlHandler implements ErrorHandler, ErrorListener {

    public static final int MAX_FIRST_ISSUES = 10;

    private Logger log;
    
    private long logLimit;

    private List<Exception> firstWarnings;
    private List<Exception> firstErrors;
    private long warningCount;
    private long errorCount;
    @Nullable
    private Exception fatalError;
    private String fatalReason;
    
    private String defaultSystemId;

       
    /**
     * @since 0.1.0
     */
    private DivXmlHandler() {
        this.log = LoggerFactory.getLogger(DivXmlHandler.class);
        this.warningCount = 0;
        this.errorCount = 0;
        this.firstErrors = new ArrayList<>();
        this.firstWarnings = new ArrayList<>();
        this.fatalError = null;
        this.logLimit = -1;
        this.defaultSystemId = "";

    }


    /**
     * 
     * @param defaultSystemId The main document upon which xml validation is performed, won't be shown in logs.
     * @param logLimit if -1 all logs are outputted.
     * @since 0.1.0
     */
    public DivXmlHandler(
            @Nullable Logger log, 
            long logLimit, 
            String defaultSystemId) {
        
        this();

        if (log == null) {
            this.log.error("Found null log, using default one!");
        } else {
            this.log = log;
        }
        
        if (logLimit < -1){
            this.log.error("Found logLimit < -1, setting it to -1");
            this.logLimit = -1;
        } else {
            this.logLimit = logLimit;
        }
        
        if (defaultSystemId == null){
            this.log.error("Found defaultSystemId, setting it to empty string.");
            this.defaultSystemId = "";
        } else {
            this.defaultSystemId = defaultSystemId;
        }
        
    }

    /**
     * Returns the amount of logs which will be outputted. If -1 all 
     * log messages will be emitted.
     * 
     * @since 0.1.0
     */
    public long getLogLimit() {
        return logLimit;
    }
    
    /**
     * 
     * The main document upon which xml validation is performed, won't be shown in logs.
     * 
     * @since 0.1.0
     */
    public String getDefaultSystemId() {
        return defaultSystemId;
    }
    
    
    /**
     * @since 0.1.0
     */
    public Logger getLog() {
        return log;
    }

    /**
     * A returns a list of the first {@value #MAX_FIRST_ISSUES} warnings
     * 
     * @since 0.1.0
     */
    public List<Exception> getFirstWarnings() {
        return Collections.unmodifiableList(firstWarnings);
    }

    /**
     * A returns a list of the first {@value #MAX_FIRST_ISSUES} warnings
     * 
     * @since 0.1.0
     */
    public List<Exception> getFirstErrors() {
        return Collections.unmodifiableList(firstErrors);
    }

    /**
     * Returns the total number of warnings occurred during the parse.
     * 
     * @since 0.1.0
     */
    public long getWarningCount() {
        return warningCount;
    }

    /**
     * Returns the excpetion for the fatal error, or fails if no fatal error
     * occurred
     * 
     * @throws DivException
     *             if no fatal error was encountered.
     * 
     * @see #isFatal()
     */
    public Exception fatalError() {
        if (!isFatal()) {
            throw new DivException("No fatal error was raised!");
        }
        return fatalError;
    }

    /**
     * Returns true if the parse ended with a fatal error.
     * 
     * @see #fatalError()
     * 
     * @since 0.1.0
     */
    public boolean isFatal() {
        return fatalError != null;
    }

    /**
     * Returns the number of errors occurred during the parse.
     * 
     * @since 0.1.0
     */
    public long getErrorsCount() {
        return errorCount;
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void warning(SAXParseException ex) {        
        processWarning(ex);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */    
    @Override
    public void warning(TransformerException ex) throws TransformerException {
        processWarning(ex);
    }

    /**
     * 
     * @since 0.1.0
     */
    private void processWarning(Exception ex){
        if (firstWarnings.size() < MAX_FIRST_ISSUES){
            firstWarnings.add(ex);
        }

        if (issuesCount() < logLimit || logLimit == -1){
            log.warn(exceptionToString(ex, defaultSystemId));
        }
        
        this.warningCount += 1;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void error(SAXParseException ex) {                             
        processError(ex);
    }
    
    @Override
    public void error(TransformerException ex) throws TransformerException {
        processError(ex);
    }   
    
    /**
     * @since 0.1.0
     */
    private void processError(Exception ex){
        
        String msg = ex.getLocalizedMessage();
        
          if (msg != null 
                && msg.contains("every $prefixed-id")
                && msg.contains("for element 'LexicalResource'")){            
            log.debug("Skipping xerces id prefix assertion because line reporting is imprecise!");
            log.debug("Xerces message was -> " + msg);
        } else {
            if (firstErrors.size() < MAX_FIRST_ISSUES){
                firstErrors.add(ex);
            }
            
            if (issuesCount() < logLimit || logLimit == -1){
                log.error(exceptionToString(ex, defaultSystemId));            
            }

            this.errorCount += 1;
            
        }
    }

    /**
     * Returns the total number of issues found so far.
     * 
     * @since 0.1.0
     */
    public long issuesCount() {        
        return this.warningCount + this.errorCount + (isFatal() ? 1 : 0);
    }

    /**
     * DIVERSICON NOTICE: check also {@link #fatalError(TransformerException)}
     * 
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        this.fatalError = ex;
                
        if (ex.getSystemId().contains(Diversicons.DIVERSICON_DTD_1_0_FILENAME)){
            log.info("TODO: skipping DTD validation ...");
            return;
        } else {
            log.error("FATAL!  " + exceptionToString(ex, defaultSystemId));
            throw ex;
        }
        
    }      
    
    /**
     * DIVERSICON NOTICE: check also {@link #fatalError(SAXParseException)}
     * 
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void fatalError(TransformerException ex) throws TransformerException {
        this.fatalError = ex;
        log.error("FATAL!  " + exceptionToString(ex, defaultSystemId));
        throw ex;
    }
 

    /**
     * Returns true if there were validation problems.
     * 
     * @since 0.1.0
     */
    public boolean invalid() {
        return issuesCount() > 0;
    }

    /**
     * @since 0.1.0
     */    
    public String summary() {
        String fat = isFatal() ? "Fatal error: " + fatalReason + " - " : "";
        return fat + "Found " + errorCount + " errors and " + warningCount + " warnings";
    }
    
    /**
     * Outputs exception {@code ex} in a format suitable for a log.
     * 
     * @param defaultSystemId  won't be shown in the output (See {@link #getDefaultSystemId() getter})
     * 
     * @since 0.1.0
     */
    // todo think about public id
    private static String exceptionToString(Exception ex, String defaultSystemId){
        
        if (ex == null){
            return "#ERROR IN EXCEPTION REPORTING, FOUND NULL EXCEPTION#";
        }
        
        
        String systemId = null;
        long lineNumber = -1;
        long colNumber = -1;
        
        if (ex instanceof SAXParseException){
            SAXParseException spe = (SAXParseException) ex;
            systemId = ((SAXParseException) ex).getSystemId();
            lineNumber = spe.getLineNumber();
            colNumber = spe.getColumnNumber();
        } else if (ex instanceof TransformerException){
            systemId = null;
        }
        
        StringBuilder buf = new StringBuilder();
        String message = ex.getLocalizedMessage();
        //if (publicId!=null)    buf.append("publicId: ").append(publicId);
                
        if (systemId!=null
                && !systemId.equals(defaultSystemId)){
            buf.append("systemId: ").append(systemId);
            buf.append(";");
        }                     
        
        buf.append("").append(lineNumber);
        buf.append("," + colNumber).append(": ");
        
        //append the exception message at the end
        if (message==null){
            buf.append("unknown problem.");
        } else {
            String fixedMessage;

            if (message.contains("Content is not allowed in prolog")){  
                fixedMessage = "Unparseable XML!";
            } else {
                fixedMessage = message;
            }
            buf.append(fixedMessage);            
        }
        
        
        return buf.toString();        
    }

    /**
     * Return the first found issue as a string. If no issue was found 
     * returns the empty string.
     * 
     * @since 0.1.0
     */
    public String firstIssueAsString() {
        String prefix;
        
        if (isFatal()){            
            return "Fatal error was: " + exceptionToString(fatalError, defaultSystemId);
        }
        if (!getFirstErrors().isEmpty()){
            
            if (getFirstErrors().size() == 1){
                prefix = "Error was: ";                               
            } else {
                prefix = "First error was: ";
                
            }
            return  prefix + exceptionToString(getFirstErrors().get(0), defaultSystemId);
        } else if (!getFirstWarnings().isEmpty()){
            if (getFirstWarnings().size() == 1){
                prefix = "Wrror was: ";                               
            } else {
                prefix = "First warning was: ";                
            }            
            return prefix + exceptionToString(getFirstWarnings().get(0), defaultSystemId);
        } else {
            return "";
        }
    }





    
}