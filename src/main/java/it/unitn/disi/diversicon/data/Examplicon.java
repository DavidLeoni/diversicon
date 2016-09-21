package it.unitn.disi.diversicon.data;

import javax.annotation.Nullable;

import it.unitn.disi.diversicon.BuildInfo;
import it.unitn.disi.diversicon.LexResPackage;

/**
 * Example terminology used for testing XML validation.   
 * 
 * @since 0.1.0
 *
 */
public class Examplicon extends LexResPackage {

    /**
     * @since 0.1.0
     */
    public static final String ID = "examplicon";
    
    /**
     * 
     * @since 0.1.0
     */
    public static final String LABEL = "The Examplicon";    
    
    /**
     * @since 0.1.0
     */
    public static final String PREFIX = "ex";

    /**
     * @since 0.1.0
     */
    private static final String CLASSPATH = "classpath:/" + ID + ".lmf";
    
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
    private static final Examplicon INSTANCE = new Examplicon();   
    
    static {
        INSTANCE.setName(ID);
        INSTANCE.setPrefix(PREFIX);
        INSTANCE.setLabel(LABEL);
        INSTANCE.setH2DbUri(CLASSPATH + ".h2.db");
        INSTANCE.setSqlUri(CLASSPATH + ".sql");
        INSTANCE.setXmlUri(CLASSPATH + ".xml");
        INSTANCE.setSampleXmlUri(CLASSPATH + ".xml"); // sample of itself
        if (BuildInfo.hasProperties(Examplicon.class)){
            BuildInfo buildInfo = BuildInfo.of(Examplicon.class);
            INSTANCE.setVersion(buildInfo.getVersion());
        } else {            
            throw new IllegalStateException("Couldn't find properties file " + BuildInfo.BUILD_PROPERTIES_PATH + " for class " + Examplicon.class.getCanonicalName());
        }
        
        
    }
       
    /**
     * @since 0.1.0
     */
    public static Examplicon of(){
        return INSTANCE;
    }

   
}



