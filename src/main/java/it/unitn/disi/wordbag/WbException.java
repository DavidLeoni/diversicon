package it.unitn.disi.wordbag;


/**
 * A generic runtime exception. 
 * 
 * @author David Leoni <david.leoni@unitn.it>
 * @since 0.1
 */
public class WbException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    protected WbException(){
        super();
    }
    
    public WbException(Throwable tr) {
        super(tr);
    }

    public WbException(String msg, Throwable tr) {
        super(msg, tr);
    }

    public WbException(String msg) {
        super(msg);
    }
}