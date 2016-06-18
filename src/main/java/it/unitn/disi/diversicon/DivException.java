package it.unitn.disi.diversicon;


/**
 * A generic runtime exception. 
 * 
 * @since 0.1
 */
public class DivException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    protected DivException(){
        super();
    }
    
    public DivException(Throwable tr) {
        super(tr);
    }

    public DivException(String msg, Throwable tr) {
        super(msg, tr);
    }

    public DivException(String msg) {
        super(msg);
    }
}