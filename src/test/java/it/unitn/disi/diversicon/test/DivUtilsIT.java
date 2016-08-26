package it.unitn.disi.diversicon.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.h2.tools.Restore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.InvalidSchemaException;
import it.unitn.disi.diversicon.data.Smartphones;
import it.unitn.disi.diversicon.internal.ExtractedStream;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * @since 0.1.0 
 */
public class DivUtilsIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;
        
    /**
     * @since 0.1.0
     */
    @Before
    public void beforeMethod() throws IOException {
        // needed for testing caching
        Path newHome = Internals.createTempDir(DivTester.DIVERSICON_TEST_STRING + "-home");
        System.setProperty("user.home", newHome.toString());
        
        dbConfig = DivTester.createNewDbConfig();                
    }

    /**
     * @since 0.1.0
     */    
    @After
    public void afterMethod(){
        dbConfig = null;       
    }

    /**
     * Ignored ,this makes my laptop hang....
     * 
     * @since 0.1.0 
     */
    // @Test
    public void testRestoreAugmentedWordnetSqlToH2InMemory(){
        Diversicons.restoreH2Sql(Smartphones.of().getSqlUri(), dbConfig);
        
        Diversicon div = Diversicon.connectToDb(dbConfig);
                
        div.getSession().close();
    
    }
    
    /**
     * @since 0.1.0 
     */
    @Test
    public void testRestoreH2DbToFile() throws IOException {

        Path dir = DivTester.createTestDir();
        
        File target = new File(dir.toString() + "/test");
        
        Diversicons.restoreH2Db(Smartphones.of().getH2DbUri(), target.getAbsolutePath());
        
        DBConfig dbCfg = Diversicons.makeDefaultH2FileDbConfig(target.getAbsolutePath(), false); 
        
        Diversicon div = Diversicon.connectToDb(dbCfg);
        
        div.getLexiconNames();
                
        div.getSession().close();
    }


    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDataWordnetSql(){
        ExtractedStream es = Internals.readData(Smartphones.of().getSqlUri(), true);
        assertTrue(es.isExtracted());
        assertEquals("div-wn31.sql", es.getFilepath());
        assertEquals(Smartphones.of().getSqlUri(), es.getSourceUrl());
        File f = es.toTempFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);        
    }
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testReadDataWordnetXml(){
        ExtractedStream es = Internals.readData(Smartphones.of().getXmlUri(), true);
        assertTrue(es.isExtracted());
        assertEquals("div-wn31.xml", es.getFilepath());
        assertEquals(Smartphones.of().getXmlUri(), es.getSourceUrl());
        File f = es.toTempFile();
        assertTrue(f.exists());
        assertTrue(f.length() > 0);                
    }
    
    
    /**
     * Pretty useless, H2 can only Restore  to a file
     * 
     * @since 0.1.0
     */
    // @Test
    public void testRestoreNativeH2Db(){
        Restore.execute("../../diversicon-wordnet-3.1/src/main/resources/it/unitn/disi/diversicon/data/wn30/div-wn30.h2.db.zip", "target/restored-wn31", "restored-db", false);
    }


    /**
     * @since 0.1.0
     */
    @Test
    public void testFetchH2Db(){
        
        assertFalse(Diversicons.getCacheDir().exists());
        Diversicons.fetchH2Db(Smartphones.ID, Smartphones.of().getVersion());        
        assertTrue(Diversicons.getCachedH2DbDir(Smartphones.ID, Smartphones.of().getVersion()).exists());
        // should be faster ...
        DBConfig config = Diversicons.fetchH2Db(Smartphones.ID, Smartphones.of().getVersion());
        
        // should allow multiple connections ...
        
        Diversicon div1 = null;
        Diversicon div2 = null;

        try {
            div1 = Diversicon.connectToDb(config);
            div2 = Diversicon.connectToDb(config);
            
            LOG.debug(div1.formatImportJobs(false));
            LOG.debug(div2.formatImportJobs(false));            
        } finally {
            if (div1 != null){
                div1.getSession().close();    
            }
            if (div2 != null){
                div2.getSession().close();    
            }    
        }
        
        
    }
    
    
    /**
     * @since 0.1.0
     */
    @Test
    public void testCleanCache() throws IOException {
        
        assertFalse(Diversicons.getCacheDir().exists());
        Files.createDirectories(Diversicons.getCacheDir().toPath());
        Files.createFile(Paths.get(Diversicons.getCacheDir().getAbsolutePath(), "test.txt"));
        assertTrue(Diversicons.getCacheDir().exists());
        Diversicons.cleanCache();
        assertFalse(Diversicons.getCacheDir().exists());        
        
    }
    

}
