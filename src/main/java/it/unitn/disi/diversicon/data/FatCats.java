package it.unitn.disi.diversicon.data;

import javax.annotation.Nullable;

import it.unitn.disi.diversicon.BuildInfo;
import it.unitn.disi.diversicon.DiversiconResource;

/**
 * Singleton holding references to sample {@code fat-cat} files packaged for Diversicon
 * 
 * @since 0.1.0
 *
 */
public class FatCats extends DiversiconResource {

    /**
     * @since 0.1.0
     */
    public static final String ID = "fat-cats-lmf";
    
    /**
     * @since 0.1.0
     */
    public static final String PREFIX = "fc01";

    /**
     * @since 0.1.0
     */
    private static final String CLASSPATH = "classpath:" + ID;
    
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
    private static final FatCats INSTANCE = new FatCats();
   

    
    static {
        INSTANCE.setId(ID);
        INSTANCE.setPrefix(PREFIX);
        INSTANCE.setH2DbUri(CLASSPATH + ".h2.db");
        INSTANCE.setSqlUri(CLASSPATH + ".sql");
        INSTANCE.setXmlUri(CLASSPATH + ".xml");
        if (BuildInfo.hasProperties(FatCats.class)){
            BuildInfo buildInfo = BuildInfo.of(FatCats.class);
            INSTANCE.setVersion(buildInfo.getVersion());
        } else {            
            throw new IllegalStateException("Couldn't find properties file " + BuildInfo.BUILD_PROPERTIES_PATH + " for class " + FatCats.class.getCanonicalName());
        }
        
        
    }
       
    /**
     * @since 0.1.0
     */
    public static FatCats of(){
        return INSTANCE;
    }
   
}



