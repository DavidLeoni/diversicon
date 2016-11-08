package eu.kidf.diversicon.core.test;

import static eu.kidf.diversicon.core.test.DivTester.*;
import static eu.kidf.diversicon.core.test.LmfBuilder.lmf;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import org.dom4j.DocumentException;
import org.hibernate.exception.GenericJDBCException;
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
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import eu.kidf.diversicon.core.DbInfo;
import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.ImportConfig;
import eu.kidf.diversicon.core.ImportJob;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivIoException;
import eu.kidf.diversicon.core.exceptions.DivNotFoundException;
import eu.kidf.diversicon.core.exceptions.InterruptedImportException;
import eu.kidf.diversicon.core.exceptions.InvalidImportException;
import eu.kidf.diversicon.core.exceptions.InvalidSchemaException;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.data.DivWn31;
import eu.kidf.diversicon.data.Smartphones;

public class DivConfigTest {

    private static final Logger LOG = LoggerFactory.getLogger(DivConfigTest.class);

    @Test
    public void testBuilder(){
                
        assertEquals(null, DivConfig.of().getDbConfig());
        
        DivConfig.builder().setDbConfig(null).build();
        
        DivConfig.Builder twiceBuilder = DivConfig.builder();
        twiceBuilder.build();
        try {                        
            twiceBuilder.build();
        } catch (IllegalStateException ex){
            
        }
    }

}
