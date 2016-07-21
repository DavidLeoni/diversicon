package it.unitn.disi.diversicon.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;

import javax.annotation.Nullable;

import org.dom4j.DocumentException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;

import static it.unitn.disi.diversicon.test.DivTester.GRAPH_1_HYPERNYM;
import static it.unitn.disi.diversicon.test.DivTester.checkDb;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests to show Uby corner cases or bugs to report.
 *
 */
public class UbyTest {

    private static final Logger LOG = LoggerFactory.getLogger(UbyTest.class);

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = DivTester.createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;       
    }

    /**
     * Uby bug to report. Shows getSynsetById as of Uby 0.7 throws
     * IndexOutOfBoundsException
     * 
     * See https://github.com/DavidLeoni/diversicon/issues/7
     */
    @Test
    public void testGetSynsetById() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig);
        Uby uby = new Uby(dbConfig);

        try {
            uby.getSynsetById("666");
            Assert.fail("Shouldn't arrive here!");
        } catch (IndexOutOfBoundsException ex) {

        }

        uby.getSession()
           .close();
    }

    /**
     * 
     * @param lexicalResourceName the name of an existing lexical resource into which merge. 
     * If it doesn't exist or null, a new lexical resource is created using name attribute found 
     * in the XML (thus provided {@code lexicaResourceName} will be ignored).
     */
    private void importIntoUby(LexicalResource res, @Nullable String lexicalResourceName) {

        try {           
            File xml = DivTester.writeXml(res);

            new XMLToDBTransformer(dbConfig).transform(xml, null);
        } catch (Exception ex) {
            throw new DivException("Error while import file into Uby!", ex);
        }
    }

    /**
     * 
     * Demonstrates you can't import twice a lexicon with the same id with
     * uby 0.7.0 xml transformer, even if you explicitly select an existing
     * lexical resource, see https://github.com/DavidLeoni/diversicon/issues/6 
     * 
     * See also {@link DiversiconTest#testCantMergeSameLexicon()}
     * 
     * @since 0.1.0
     */
    @Test
    public void testCantMergeSameLexicon() throws FileNotFoundException, IllegalArgumentException, DocumentException {

        LMFDBUtils.createTables(dbConfig);
                           
        File xml = DivTester.writeXml(GRAPH_1_HYPERNYM);

        new XMLToDBTransformer(dbConfig).transform(xml, null);
        
        try {
            new XMLToDBTransformer(dbConfig).transform(xml, GRAPH_1_HYPERNYM.getName());
        } catch (ConstraintViolationException ex) {
            assertTrue(ex.getMessage()
                         .contains(
                                 "Unique index or primary key violation: \"PRIMARY_KEY_2 ON PUBLIC.LEXICON(LEXICONID)"));
        }

    }
}
