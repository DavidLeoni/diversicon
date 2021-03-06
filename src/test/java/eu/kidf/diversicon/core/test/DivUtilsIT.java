package eu.kidf.diversicon.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.h2.tools.Restore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.ExtractedStream;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.data.DivWn31;
import eu.kidf.diversicon.data.Examplicon;
import eu.kidf.diversicon.data.Smartphones;
import org.junit.Ignore;

/**
 * @since 0.1.0
 */
public class DivUtilsIT {

    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);

    private DivConfig divConfig;

    /**
     * @since 0.1.0
     */
    @Before
    public void beforeMethod() throws IOException {
        // needed for testing caching
        Path newHome = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING + "-home");
        System.setProperty("user.home", newHome.toString());

        divConfig = DivTester.createNewDivConfig();
    }

    /**
     * @since 0.1.0
     */
    @After
    public void afterMethod() {
        divConfig = null;
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testRestoreWordnetH2DbToFile() throws IOException {

        Path dir = DivTester.createTestDir();

        File target = new File(dir.toString() + "/test");

        Diversicons.h2RestoreDb(DivWn31.of()
                                       .getH2DbUri(),
                target.getAbsolutePath());

        DBConfig dbCfg = Diversicons.h2FileConfig(target.getAbsolutePath(), false);

        Diversicon div = Diversicon.connectToDb(DivConfig.of(dbCfg));

        div.getLexiconNames();

        div.getSession()
           .close();
    }

    /**
     * We should be able to import Smartphones and compute transitive closure
     * even without Wordnet loaded.
     * 
     * @since 0.1.0
     * 
     * @see DiversiconTest#testImportSmartPhonesXmlWithoutWordnet()
     */
    @Test
    public void testImportSmartPhonesXmlWithWordnet() {

        Path dir = DivTester.createTestDir();

        File target = new File(dir.toString() + "/test");
        
        Diversicons.h2RestoreDb(DivWn31.of().getH2DbUri(), target.getAbsolutePath());
        
        DBConfig dbCfg = Diversicons.h2FileConfig(target.getAbsolutePath(), false); 
        
        Diversicon div = Diversicon.connectToDb(DivConfig.of(dbCfg));               
        
        div.importXml(Smartphones.of().getXmlUri());    
    }

    /**
     * 
     * @since 0.1.0
     * 
     * @see DiversiconTest#testImportExampliconXmlWithoutWordnet()
     */
    @Test
    public void testImportExampliconXmlWithWordnet() {

        Path dir = DivTester.createTestDir();

        File target = new File(dir.toString() + "/test");
        
        Diversicons.h2RestoreDb(DivWn31.of().getH2DbUri(), target.getAbsolutePath());
        
        DBConfig dbCfg = Diversicons.h2FileConfig(target.getAbsolutePath(), false); 
        
        Diversicon div = Diversicon.connectToDb(DivConfig.of(dbCfg));               
        
        div.importXml(Examplicon.of().getXmlUri());    
    }

    /**
     * 
     * @since 0.1.0
     * 
     * @see DiversiconTest#testImportSmartphonesExampliconXmlWithoutWordnet()
     */
    @Test
    public void testImportSmartphonesExampliconXmlWithWordnet() {

        Path dir = DivTester.createTestDir();

        File target = new File(dir.toString() + "/test");
        
        Diversicons.h2RestoreDb(DivWn31.of().getH2DbUri(), target.getAbsolutePath());
        
        DBConfig dbCfg = Diversicons.h2FileConfig(target.getAbsolutePath(), false); 
        
        Diversicon div = Diversicon.connectToDb(DivConfig.of(dbCfg));               
        
        div.importXml(Smartphones.of().getXmlUri(), Examplicon.of().getXmlUri());    
    }
    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDataWordnetSql() {
        ExtractedStream es = Diversicons.readData(DivWn31.of()
                                                         .getSqlUri(),
                true);
        assertTrue(es.isExtracted());
        assertEquals("div-wn31.sql", es.getFilepath());
        assertEquals(DivWn31.of()
                            .getSqlUri(),
                es.getSourceUrl());
        File f = es.toTempFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDataWordnetXml() {
        ExtractedStream es = Diversicons.readData(DivWn31.of()
                                                         .getXmlUri(),
                true);
        assertTrue(es.isExtracted());
        assertEquals("div-wn31.xml", es.getFilepath());
        assertEquals(DivWn31.of()
                            .getXmlUri(),
                es.getSourceUrl());
        File f = es.toTempFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testImportXmlWordnetSample() {

        Diversicons.createTables(divConfig.getDbConfig());

        Diversicon div = Diversicon.connectToDb(divConfig);

        div.importXml(DivWn31.SAMPLE_XML_URI);

    }

    /**
     * Pretty useless, H2 can only Restore to a file
     * 
     * @since 0.1.0
     */
    // @Test
    public void testRestoreNativeH2Db() {
        Restore.execute(
                "../../diversicon-wordnet-3.1/src/main/resources/it/unitn/disi/diversicon/data/wn30/div-wn30.h2.db.zip",
                "target/restored-wn31", "restored-db", false);
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testFetchH2Db(){
        
        Path cacheRoot = Paths.get(DivTester.createTestDir().toString(),"test");
        
        assertFalse(cacheRoot.toFile().exists());
        Diversicons.fetchH2Db(cacheRoot.toFile(), DivWn31.NAME, DivWn31.of().getVersion());        
        assertTrue(Diversicons.getCachedDir(cacheRoot.toFile(), DivWn31.NAME, DivWn31.of().getVersion()).exists());
        // should be faster ...
        DBConfig config = Diversicons.fetchH2Db(cacheRoot.toFile(), DivWn31.NAME, DivWn31.of()
                                                                                         .getVersion());

        // should allow multiple connections ...

        Diversicon div1 = null;
        Diversicon div2 = null;

        try {
            div1 = Diversicon.connectToDb(DivConfig.of(config));
            div2 = Diversicon.connectToDb(DivConfig.of(config));

            LOG.debug(div1.formatImportJobs(false));
            LOG.debug(div2.formatImportJobs(false));
        } finally {
            if (div1 != null) {
                div1.getSession()
                    .close();
            }
            if (div2 != null) {
                div2.getSession()
                    .close();
            }
        }

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testApp1(){
        TestApp1.main(new String[]{});
    }
}
