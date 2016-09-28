package it.unitn.disi.diversicon.exceptions;

import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.ImportConfig;
import it.unitn.disi.diversicon.LexResPackage;

/**
 * A runtime exception raised to signal an import request is erroneous,
 * before any changes to database have been done.
 * 
 * @see InterruptedImportException
 * 
 * @since 0.1.0
 */
public class InvalidImportException extends DivException {
    
    private static final long serialVersionUID = 1L;

    
    private ImportConfig config;
    private String fileUrl;
    private LexResPackage pack;
    
    
    /**
     * @since 0.1.0
     */
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
     * Creates the exception 
     * 
     * @since 0.1.0
     */
    public InvalidImportException(
            String msg,
            Throwable ex,
            ImportConfig config,
            String fileUrl, 
            LexResPackage pack 
            ) {
        super(msg, ex);
        this.config = config;
        this.fileUrl = fileUrl;
        this.pack = pack;        
    }
    
    /**
     * Creates the exception 
     * 
     * @since 0.1.0
     */
    public InvalidImportException(
            String msg, 
            ImportConfig config,
            String fileUrl, 
            LexResPackage pack) {
        super(msg);
        this.config = config;
        this.fileUrl = fileUrl;
        this.pack = pack;
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