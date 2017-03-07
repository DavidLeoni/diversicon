package eu.kidf.diversicon.core.exceptions;

import eu.kidf.diversicon.core.exceptions.DivException;

/**
 * A runtime exception to raise when the import of a LexicalResource is invalid. 
 * 
 * @since 0.1.0
 */
public class DivValidationException extends DivException {
    
    private static final long serialVersionUID = 1L;

    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.1.0
     */
    public DivValidationException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.1.0
     */
    public DivValidationException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */
    public DivValidationException(String msg) {
        super(msg);
    }
}