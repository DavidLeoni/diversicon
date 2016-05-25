package it.unitn.disi.wordbag;

/**
 * A runtime exception to raise when something is not found.
 * 
 * @author David Leoni <david.leoni@unitn.it>
 * @since 0.1
 */
public class WbNotFoundException extends WbException {
    
    private static final long serialVersionUID = 1L;

    private WbNotFoundException(){
        super();
    }
    
    /**
     * Creates the NotFoundException using the provided throwable
     */
    public WbNotFoundException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the NotFoundException using the provided message and throwable
     */
    public WbNotFoundException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the NotFoundException using the provided message
     */
    public WbNotFoundException(String msg) {
        super(msg);
    }
}