package eu.kidf.diversicon.core.exceptions;

import javax.annotation.Nullable;

import eu.kidf.diversicon.core.DivXmlValidator;
import eu.kidf.diversicon.core.ImportConfig;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivException;

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
    @Nullable
    private DivXmlValidator validator;
    
    
    
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
            @Nullable
            ImportConfig config,
            String fileUrl,
            @Nullable
            LexResPackage pack,
            @Nullable
            DivXmlValidator validator
            ) {
        super(msg, ex);
        this.config = config;
        this.fileUrl = fileUrl;
        this.pack = pack;
        this.validator = validator;
    }
    
    /**
     * Creates the exception 
     * 
     * @since 0.1.0
     */
    public InvalidImportException(
            String msg, 
            @Nullable
            ImportConfig config,
            @Nullable
            String fileUrl,
            @Nullable
            LexResPackage pack,
            @Nullable
            DivXmlValidator validator) {
        super(msg);
        this.config = config;
        this.fileUrl = fileUrl;
        this.pack = pack;
        this.validator = validator;
    }
    
    
    /**
     * Creates the exception using the provided message
     * 
     * @since 0.1.0
     */    
    public InvalidImportException(String msg) {
        super(msg);
    }
    
    /**
     * @since 0.1.0
     */
    @Nullable
    public ImportConfig getConfig() {
        return config;
    }
    
    /**
     * @since 0.1.0
     */
    @Nullable
    public String getFileUrl() {
        return fileUrl;
    }
    
    /**
     * @since 0.1.0
     */
    @Nullable
    public LexResPackage getPack() {
        return pack;
    }

    /**
     * @since 0.1.0
     */    
    @Nullable
    public DivXmlValidator getValidator() {
        return validator;
    }
    
    
    
}