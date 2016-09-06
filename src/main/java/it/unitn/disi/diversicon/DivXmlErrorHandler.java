package it.unitn.disi.diversicon;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * Redirects Xml validation errors to a provided log, and keeps counts for errors and warnings. 
 * 
 * @since 0.1.0
 *
 */
public class DivXmlErrorHandler implements ErrorHandler {
    
    
       
    private Logger log;
    
    private long warningCount;
    private long errorCount;
    private boolean fatal;
    private String fatalReason;

    /**
     * @since 0.1.0
     */
    private DivXmlErrorHandler(){        
        this.warningCount = 0;
        this.errorCount = 0;
        this.fatal = false;
        this.fatalReason = "";
        this.log = LoggerFactory.getLogger(DivXmlErrorHandler.class);
    }
    
    /**
     * @since 0.1.0
     */    
    public DivXmlErrorHandler(@Nullable Logger log){
        this();
        
        if (log == null){
            this.log.error("Found null log, using default one!");            
        } else {
            this.log = log;
        }
    }

    /** 
     * @since 0.1.0
     */    
    public Logger getLog() {
        return log;
    }


    /**
     * Returns the number of warnings occurred during the parse.
     * 
     * @since 0.1.0
     */
    public long getWarningCount() {
        return warningCount;
    }

    /**
     * Returns true if the parse ended with a fatal error. 
     * 
     * @since 0.1.0
     */
    public boolean isFatal(){
        return fatal;
    }

    /**
     * If no fatal error occurred returns the empty string.
     * 
     * @since 0.1.0
     */
    public String getFatalReason(){
        return fatalReason;
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
    public void warning(SAXParseException ex) {

        this.warningCount += 1;
        
        log.warn(ex.getMessage());    

        
    }

    /**
     * {@inheritDoc}
     *      
     * @since 0.1.0
     */    
    public void error(SAXParseException ex) {
        this.errorCount += 1;
        
        log.error(ex.getSystemId());
        log.error("Line: " + ex.getLineNumber());
        log.error("Column: "+ex.getColumnNumber());
        log.error(ex.getMessage());            
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        this.fatal = true;

        log.error("FATAL ERROR! " + ex.getMessage());            
        throw ex;
    }

    /**
     * Returns true if there were validation problems.
     * 
     * @since 0.1.0
     */
    public boolean invalid() {
        return this.warningCount > 0 || this.errorCount > 0;
    }

    /**
     * @since 0.1.0
     */
    @Override
    public String toString(){
        String fat = fatal ? "Fatal error: " + fatalReason + " - ": ""; 
        return fat + "Found " + errorCount + " errors and " + warningCount + " warnings";  
    }
}