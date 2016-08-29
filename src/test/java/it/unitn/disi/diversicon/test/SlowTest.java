package it.unitn.disi.diversicon.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.data.DivWn31;

import static it.unitn.disi.diversicon.test.DivTester.*;

/**
 * Unbearably slow tests go here (they might even nearly choke your system ...)
 * 
 * Normally they are skipped, both in regular and IT tests.
 * To execute them as IT tests with Maven, use -PslowTests
 * 
 * @since 0.1.0
 */
public class SlowTest {

    private static final Logger LOG = LoggerFactory.getLogger(SlowTest.class);

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testImportWordnetInMemoryDb() throws IOException {
        DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("mydb-" + new Date().getTime(), false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);        
        div.importXml(DivWn31.of().getXmlUri());
    }

    /**
     * Ignored ,this makes my laptop hang....
     * 
     * @since 0.1.0 
     */
    // @Test
    public void testRestoreAugmentedWordnetSqlToH2InMemory(){
        Diversicons.restoreH2Sql(DivWn31.of().getSqlUri(), dbConfig);
        
        Diversicon div = Diversicon.connectToDb(dbConfig);
                
        div.getSession().close();
    
    }    
    
    /**
     * @since 0.1.0
     */    
    @Test
    public void testImportWordnetInFileDb() throws IOException {
        DBConfig dbConfig = Diversicons.makeDefaultH2FileDbConfig("target/div-wn31-" + new Date().getTime(), false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importXml(DivWn31.of().getXmlUri());
    }

    /**
     * @since 0.1.0
     */    
    @Test
    public void testImportWordnetInMemoryExportToSql() throws IOException {
        DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("mydb-" + new Date().getTime(), false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);        
        div.importXml(DivWn31.of().getXmlUri());
        String zipFilePath = "target/div-wn31-" + new Date().getTime() + ".sql.zip";
        div.exportToSql(zipFilePath, true);
        assertTrue(new File(zipFilePath).exists());
        div.getSession().close();
    }
    
    /**
     * @since 0.1.0
     */    
    @Test
    public void testImportWordnetInFileDbExportToSql() throws IOException {
        DBConfig dbConfig = Diversicons.makeDefaultH2FileDbConfig("target/div-wn31-" + new Date().getTime(), false);
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importXml(DivWn31.of().getXmlUri());
        String zipFilePath = "target/div-wn31-" + new Date().getTime() + ".sql.zip";
        div.exportToSql(zipFilePath, true);
        assertTrue(new File(zipFilePath).exists());
        div.getSession().close();
    }

}
