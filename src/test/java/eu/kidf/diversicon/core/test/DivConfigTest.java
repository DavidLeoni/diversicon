package eu.kidf.diversicon.core.test;

import static org.junit.Assert.*;

import org.junit.Test;
import eu.kidf.diversicon.core.DivConfig;

public class DivConfigTest {   

    @Test
    public void testBuilder(){
                
        assertEquals(null, DivConfig.of().getDbConfig());
        
        DivConfig.builder().setDbConfig(null).build();
        
        DivConfig.Builder twiceBuilder = DivConfig.builder();
        twiceBuilder.build();
        try {                        
            twiceBuilder.build();
        } catch (IllegalStateException ex){
            
        }
    }

}
