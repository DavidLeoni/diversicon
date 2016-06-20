package it.unitn.disi.diversicon.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DbInfo;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.ImportConfig;
import it.unitn.disi.diversicon.ImportJob;
import it.unitn.disi.diversicon.internal.Internals;

import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static it.unitn.disi.diversicon.test.DivTester.*;

public class DiversiconTrial {

    private static final Logger LOG = LoggerFactory.getLogger(DiversiconTrial.class);

    private DBConfig dbConfig;

    @Before
    public void beforeMethod() {
        dbConfig = createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }


    @Test
    public void testImportWordnet() throws IOException {
        DBConfig dbConfig = Diversicons.makeDefaultH2InMemoryDbConfig("mydb");
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        File xml = DivTester.writeXml(GRAPH_4_HYP_HOL_HELLO);
        //div.importFile(xml.getAbsolutePath());
        div.importFile(Diversicons.WORDNET_XML_RESOURCE_URI);
        String zipFilePath = "target/div-wn30-" + new Date().getTime() + ".sql.zip";
        div.exportToSql(zipFilePath, true);
        assertTrue(new File(zipFilePath).exists());
        div.getSession().close();
    }
}
