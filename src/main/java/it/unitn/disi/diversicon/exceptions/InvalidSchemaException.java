package it.unitn.disi.diversicon.exceptions;

import it.disi.unitn.diversicon.exceptions.DivException;

/**
 * A runtime exception to raise Database schema is found to be invalid.
 * 
 * @since 0.1.0
 */
public class InvalidSchemaException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private InvalidSchemaException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     */
    public InvalidSchemaException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     */
    public InvalidSchemaException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     */
    public InvalidSchemaException(String msg) {
        super(msg);
    }
}