package eu.kidf.diversicon.core.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.data.DivUpper;
import eu.kidf.diversicon.data.DivWn31;
import eu.kidf.diversicon.data.Smartphones;

/**
 * Showcases XML import  
 * 
 * @since 0.1.0
 */
public class TestApp1 {
    
    /**
     * Erases the database {@code dbName} if it exists.
     * 
     * @param dbName the database name WITHOUT '.h2.db'
     * 
     * @since 0.1.0
     */
    private static void cleanDb(String dbName){
        
        File dbFile = new File(dbName + ".h2.db");               
        
        if (dbFile.exists()){
            System.out.println(" ****  CLEANING EXISTING DB " + dbFile.getName());
            if (!dbFile.delete()){
                throw new RuntimeException("FAILED TO CLEAN DB ! " + dbFile.getAbsolutePath());    
            }            
        }
        
        File dbLockFile = new File(dbName + ".lock.db");               
        
        if (dbLockFile.exists()){            
            if (!dbLockFile.delete()){
                throw new RuntimeException("FAILED TO CLEAN DB ! "  + dbLockFile.getAbsolutePath() );    
            }            
        }
        
        File dbTraceFile = new File(dbName + ".trace.db");               
        
        if (dbTraceFile.exists()){            
            if (!dbTraceFile.delete()){
                throw new RuntimeException("FAILED TO CLEAN DB TRACE " + dbTraceFile.getAbsolutePath());    
            }            
        }
        
        
    }
    
    /**
     * @since 0.1.0
     */    
    public static void main(String [] args){
        
        String dbName = "test-div-db";
        
        cleanDb(dbName);

        System.out.println(" ****  ");
        System.out.println(" ****  RESTORING WORDNET 3.1 H2 DB: '" + dbName + "'");
        System.out.println(" ****  ");        
        
        Diversicons.h2RestoreDb(DivWn31.of().getH2DbUri(), dbName);

        DivConfig divConfig = DivConfig.of(Diversicons.h2FileConfig(dbName, false));

        Diversicon div1 = Diversicon.connectToDb(divConfig);               
        
        // checks div upper preloading
        Diversicon div = Diversicon.connectToDb(divConfig);
        assertNotNull(div.getLexicalResource(DivUpper.of().getName()));

        System.out.println(" ****  ");
        System.out.println(" ****  LOGS:");
        System.out.println(" ****  ");
        System.out.println(div.formatImportJobs(false));                              
        System.out.println(" ****  ROOT DOMAIN = " +  div.getSynsetById(DivUpper.SYNSET_ROOT_DOMAIN).toString());
        System.out.println("");
        System.out.println("");       
        
        System.out.println(" ****  IMPORTING SMARTPHONES XML              ****");
        System.out.println(" ****  ");
        System.out.println(" ****  (its synsets link to Wordnet synsets)  ****");
        System.out.println(" ****  ");
        
        div1.importXml(Smartphones.of().getXmlUri());        

        System.out.println(" ****  ");
        System.out.println(" ****  LOGS:");
        System.out.println(" ****  ");        
        System.out.println(div.formatImportJobs(false));             
        
        div1.getSession().close();
               
    }
}
