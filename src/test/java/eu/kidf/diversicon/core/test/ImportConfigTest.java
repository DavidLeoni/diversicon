package eu.kidf.diversicon.core.test;

import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.kidf.diversicon.core.ImportConfig;
import eu.kidf.diversicon.core.internal.Internals;

/**
 * @since 0.1.0
 */
public class ImportConfigTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImportConfigTest.class);
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testToStringEmpty(){
        ImportConfig config = new ImportConfig();        
        LOG.debug(config.toString());               
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testToString(){
                                     
        LOG.debug("\n" + makeConfig().toString());               
    }
    
    /**
     * @since 0.1.0
     */
    public static ImportConfig makeConfig(){
        ImportConfig config = new ImportConfig();
        
        config.setAuthor("Crazy Importer Guy");
        config.setDescription("Import made to create the most important lexicon in the human history, to be remembered in the eons to come.");
        List<String> fileUrls = Internals.newArrayList("http://some-url1", "http://some-url2");
        
        config.setFileUrls(fileUrls);
        config.setSkipAugment(true);
        
        return config;
    }
    
}
