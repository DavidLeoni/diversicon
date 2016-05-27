package it.unitn.disi.diversicon;

/**
 * A runtime exception to raise when something is not found.
 * 
 * @author David Leoni <david.leoni@unitn.it>
 * @since 0.1
 */
public class DivNotFoundException extends DivException {
    
    private static final long serialVersionUID = 1L;

    private DivNotFoundException(){
        super();
    }
    
    /**
     * Creates the NotFoundException using the provided throwable
     */
    public DivNotFoundException(Throwable tr) {
        super(tr);
    }

    /**
     * Creates the NotFoundException using the provided message and throwable
     */
    public DivNotFoundException(String msg, Throwable tr) {
        super(msg, tr);
    }

    /**
     * Creates the NotFoundException using the provided message
     */
    public DivNotFoundException(String msg) {
        super(msg);
    }
}