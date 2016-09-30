package it.unitn.disi.diversicon.exceptions;

import it.unitn.disi.diversicon.exceptions.DivException;

/**
 * A runtime exception raised if import got interrupted and 
 * as a result the database might be in an inconsistent state.
 * 
 * @see InvalidImportException 
 * 
 * @since 0.1.0
 */
public class InterruptedImportException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private InterruptedImportException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.1.0
     */
    public InterruptedImportException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.1.0
     */
    public InterruptedImportException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */
    public InterruptedImportException(String msg) {
        super(msg);
    }
}