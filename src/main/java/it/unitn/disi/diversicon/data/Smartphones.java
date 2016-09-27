package it.unitn.disi.diversicon.data;

import javax.annotation.Nullable;

import it.unitn.disi.diversicon.BuildInfo;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.LexResPackage;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * Singleton holding references to sample {@code handheld-devices} files packaged for Diversicon
 * 
 * @since 0.1.0
 *
 */
public class Smartphones extends LexResPackage {

    /**
     * @since 0.1.0
     */    
    public static final String SHORT_NAME = "smartphones";
    
    /**
     * @since 0.1.0
     */
    public static final String NAME = "div-" + SHORT_NAME;
    
    /**
     * @since 0.1.0
     */
    public static final String LABEL = "Smartphones";
    
    /**
     * @since 0.1.0
     */
    public static final String PREFIX = "sm";

    /**
     * @since 0.1.0
     */
    private static final String CLASSPATH = "classpath:/" + SHORT_NAME + ".lmf";
    
    /**
     * @since 0.1.0
     */
    public static final String SQL_URI = CLASSPATH + ".sql";
    
    /**
     * @since 0.1.0
     */
    public static final String H2DB_URI = CLASSPATH + ".h2.db";
    
    /**
     * @since 0.1.0
     */
    public static final String XML_URI = CLASSPATH + ".xml";    
    
    /**
     * @since 0.1.0
     */
    @Nullable
    private static BuildInfo buildInfo;
        
    /**
     * @since 0.1.0
     */
    private static final Smartphones INSTANCE = new Smartphones();   
    
    static {
        
        
        
        INSTANCE.setName(NAME);
        INSTANCE.setLabel(LABEL);
        INSTANCE.setPrefix(PREFIX);
        INSTANCE.setH2DbUri(CLASSPATH + ".h2.db");
        INSTANCE.setSqlUri(CLASSPATH + ".sql");
        INSTANCE.setXmlUri(CLASSPATH + ".xml");
        
        INSTANCE.setSampleXmlUri(CLASSPATH + ".xml"); // sample of itself
        if (BuildInfo.hasProperties(Smartphones.class)){
            BuildInfo buildInfo = BuildInfo.of(Smartphones.class);            
            INSTANCE.setVersion(buildInfo.getVersion());
            INSTANCE.putNamespace(PREFIX, buildInfo.sourceAtVersion(SHORT_NAME + ".lmf.xml") );  
            INSTANCE.putNamespace(DivWn31.PREFIX, DivWn31.of().namespace());
        } else {            
            throw new IllegalStateException("Couldn't find properties file " + BuildInfo.BUILD_PROPERTIES_PATH + " for class " + Smartphones.class.getCanonicalName());
        }

        
    }
       
    /**
     * @since 0.1.0
     */
    public static Smartphones of(){
        return INSTANCE;
    }
   
}



