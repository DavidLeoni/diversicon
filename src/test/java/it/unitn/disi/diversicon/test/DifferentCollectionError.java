package it.unitn.disi.diversicon.test;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * @since 0.1.0
 *
 */
public class DifferentCollectionError extends Error {
    
    @Nullable 
    private Collection col1;
    @Nullable 
    private Collection col2;
        
    public DifferentCollectionError(String msg, @Nullable Collection col1, @Nullable Collection col2) {
        super(msg);
        this.col1 = col1;
        this.col2 = col2;
    }

    @Nullable 
    public Collection getCol1() {
        return col1;
    }
    
    @Nullable 
    public Collection getCol2() {
        return col2;
    }
    
    
}

