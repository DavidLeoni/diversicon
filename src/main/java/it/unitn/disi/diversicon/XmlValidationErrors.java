package it.unitn.disi.diversicon;

/**
 * @since 0.1.0
 */
public enum XmlValidationErrors {

    /**
     * @since 0.1.0
     */    
    SYNSET_RELATION_MISSING_INTERNAL_TARGET,
    
    /**
     * @since 0.1.0
     */
    SENSE_MISSING_INTERNAL_SYNSET;

    /**
     * @since 0.1.0
     */
    public String toString(){
        return "DIV-" + ordinal() ;
    }

}
