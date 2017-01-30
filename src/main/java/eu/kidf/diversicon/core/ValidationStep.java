package eu.kidf.diversicon.core;

/**
 * A validation step.
 * 
 * @since 0.1.0
 *
 */
public enum ValidationStep {
    
    STEP_1_STRUCTURAL("Validate structure, collect ids"),
    STEP_2_INTERNAL("Verify internal ids links, external ids syntax"),
    STEP_3_EXTERNAL("Verify external ids against the db");    
        
    private String descr;

    /**
     * @since 0.1.0
     */    
    private ValidationStep(String descr){
        this.descr = descr;
    }
        
    /**
     * Returns a description of the step
     * @since 0.1.0
     */
    public String getDescr() {
        return descr;
    }

    /**
     * Returns something like 'ValidationStep 3'
     */
    @Override
    public String toString() {
        return "ValidationStep " + (ordinal() + 1) + ": " + name();
    }
    
}