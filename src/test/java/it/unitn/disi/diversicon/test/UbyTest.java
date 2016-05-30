package it.unitn.disi.diversicon.test;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
import static it.unitn.disi.diversicon.test.UtilsTest.checkDb;
import static it.unitn.disi.diversicon.test.UtilsTest.createDbConfig;


/**
 * Tests to show Uby corner cases or bugs to report.
 *
 */
public class UbyTest {

    private static final Logger LOG = LoggerFactory.getLogger(UbyTest.class);

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = createDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }
    
    
    /**
     * Uby bug to report. Shows getSynsetById as of Uby 0.7 throws IndexOutOfBoundsException
     */
    @Test
    public void testGetSynsetById() throws FileNotFoundException{
        
        LMFDBUtils.createTables(dbConfig);
        Uby uby = new Uby(dbConfig);        
        
        try {
            uby.getSynsetById("666");
            Assert.fail("Shouldn't arrive here!");
        } catch (IndexOutOfBoundsException ex){
            
        }
        
        uby.getSession().close();
    }

    /**
     * todo this seems a uby bug, it always return null !
     * @throws FileNotFoundException 
     * @since 0.1
     */
    @Test
    @Ignore
    public void todo() throws FileNotFoundException {
        
        LMFDBUtils.createTables(dbConfig);        
        Uby uby = new Uby(dbConfig);
        
        //Synset syn = uby.getSynsetIterator(null);
        
        uby.getSession().close();
    }
}
