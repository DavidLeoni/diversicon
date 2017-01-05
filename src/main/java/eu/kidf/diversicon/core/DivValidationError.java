package eu.kidf.diversicon.core;

/**
 * @since 0.1.0
 */
public enum DivValidationError {

    /**
     * @since 0.1.0
     */    
    INVALID_INTERNAL_ID,    
    
    /**
     * @since 0.1.0
     */
    MISSING_INTERNAL_ID,

    /**
     * @since 0.1.0
     */    
    MISSING_EXTERNAL_ID,
    
    /**
     * @since 0.1.0
     */
    INVALID_PREFIX,
    
    /**
     * @since 0.1.0
     */
    UNDECLARED_NAMESPACE,
    
    /**
     * @since 0.1.0
     */    
    INVALID_DIVUPPER_NAMESPACE,

    /**
     * @since 0.1.0
     */    
    INVALID_NAMESPACE, 
    
    /**
     * @since 0.1.0
     */
    NAMESPACE_CLASH;        
    
    /**
     * @since 0.1.0
     */
    public String toString(){
        return "DIV-" + ordinal() ;
    }

}
