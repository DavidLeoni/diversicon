package it.unitn.disi.diversicon.exceptions;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.DivXmlErrorHandler;

/**
 * A runtime exception raised when Xml validation fails. 
 * 
 * @since 0.1.0
 */
public class InvalidXmlException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(InvalidXmlException.class);
    
    private DivXmlErrorHandler errorHandler;
    
    /**
     * @since 0.1.0
     */
    public InvalidXmlException(DivXmlErrorHandler errorHandler){
        super();
        setErrorHandler(errorHandler);
    }
    
    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.1.0
     */
    public InvalidXmlException(DivXmlErrorHandler errorHandler, String msg, Throwable tr) {
        super(msg, tr);
        setErrorHandler(errorHandler);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */
    public InvalidXmlException(DivXmlErrorHandler errorHandler, String msg) {
        super(msg);
        setErrorHandler(errorHandler);        
    }
    
    
    /**
     * @since 0.1.0
     */
    @Nullable
    public DivXmlErrorHandler getErrorHandler() {
        return errorHandler;
    }


    /**
     * 
     */
    private void setErrorHandler(@Nullable DivXmlErrorHandler errorHandler) {
        if (errorHandler == null){
            LOG.error("Found null error handler!!!");
        }
        this.errorHandler = errorHandler;
    }

}