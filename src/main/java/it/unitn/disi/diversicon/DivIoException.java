package it.unitn.disi.diversicon;

/**
 * A runtime exception to raise when something is not found.
 * 
 * @since 0.1
 */
public class DivIoException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private DivIoException(){
        super();
    }
    
    /**
     * Creates the exception using the provided throwable
     */
    public DivIoException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the exception using the provided message and throwable
     */
    public DivIoException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the exception using the provided message
     */
    public DivIoException(String msg) {
        super(msg);
    }
}