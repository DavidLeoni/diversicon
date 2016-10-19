package it.unitn.disi.diversicon.exceptions;

import it.unitn.disi.diversicon.exceptions.DivException;

/**
 * A runtime exception raised when trying to perform
 * an operation while database is not in the correct state.  
 * 
 * 
 * @since 0.1.0
 */
public class InvalidStateException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private InvalidStateException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     * 
     * @since 0.1.0
     */
    public InvalidStateException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     * 
     * @since 0.1.0
     */
    public InvalidStateException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */
    public InvalidStateException(String msg) {
        super(msg);
    }
}