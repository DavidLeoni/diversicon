package it.unitn.disi.diversicon.exceptions;

import it.disi.unitn.diversicon.exceptions.DivException;

/**
 * A runtime exception raised to signal an import request is erroneaus,
 * before any changes to database have been done.
 * 
 * @see InterruptedImportException
 * 
 * @since 0.1.0
 */
public class InvalidImportException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private InvalidImportException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.1.0
     */
    public InvalidImportException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.1.0
     */
    public InvalidImportException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */
    public InvalidImportException(String msg) {
        super(msg);
    }
}