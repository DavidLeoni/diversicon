package it.unitn.disi.diversicon.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;

public class DivUtilsIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(DivUtilsTest.class);
    
    private DBConfig dbConfig;
        
    
    @Before
    public void beforeMethod(){
         dbConfig = DivTester.createNewDbConfig();                
    }
    
    @After
    public void afterMethod(){
        dbConfig = null;       
    }

    @Test
    public void testRestoreH2Dump(){
        Diversicons.restoreH2Dump(Diversicons.WORDNET_RESOURCE_URI, dbConfig);
        
        Diversicon div = Diversicon.connectToDb(dbConfig);
        
        
        div.getSession().close();
    }
}
