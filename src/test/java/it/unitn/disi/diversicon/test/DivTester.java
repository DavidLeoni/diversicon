package it.unitn.disi.diversicon.test;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect;
import de.tudarmstadt.ukp.lmf.model.core.Definition;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.TextRepresentation;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.interfaces.IHasID;
import de.tudarmstadt.ukp.lmf.model.morphology.FormRepresentation;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.DBToXMLTransformer;
import it.unitn.disi.diversicon.DivException;
import it.unitn.disi.diversicon.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.internal.Internals;

public final class DivTester {

    private static final String TEST_RESOURCES_PATH = "it/unitn/disi/diversicon/test/";

    private static int dbCounter = -1;
    
    /**
     * 2 verteces and 1 hypernym edge
     */
    public static LexicalResource GRAPH_1_HYPERNYM = lmf().lexicon()
                                                          .synset()
                                                          .synset()
                                                          .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                          .build();

    /**
     * 4 verteces, last one is connected to others by respectively hypernym
     * edge, holonym and 'hello' edge
     */
    public static LexicalResource GRAPH_4_HYP_HOL_HELLO = lmf().lexicon()
                                                               .synset()
                                                               .synset()
                                                               .synset()
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .synsetRelation(ERelNameSemantics.HOLONYM, 2)
                                                               .synsetRelation("hello", 3)
                                                               .build();

    /**
     * A full DAG, 3 verteces and 3 hypernyms
     */
    public static final LexicalResource DAG_3_HYPERNYM = lmf().lexicon()
                                                              .synset()
                                                              .synset()
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                              .synset()
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                              .depth(2)
                                                              .build();

    /**
     * 2 verteces, second connected to first with two relations.
     */
    public static final LexicalResource DAG_2_MULTI_REL = lmf().lexicon()
                                                               .synset()
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .synsetRelation(ERelNameSemantics.HOLONYM, 1)
                                                               .build();

    private static final Logger LOG = LoggerFactory.getLogger(DivTester.class);

    private DivTester() {
    }

    /**
     * 
     * Retrieves the synset with id 'synset ' + {@code idNum}
     * 
     * @param idNum
     *            index starts from 1
     * @throws DivNotFoundException
     */
    public static Synset getSynset(LexicalResource lr, int idNum) {
        Internals.checkArgument(idNum >= 1, "idNum must be positive, found instead " + idNum);

        for (Lexicon lexicon : lr.getLexicons()) {
            for (Synset synset : lexicon.getSynsets()) {
                if (synset.getId()
                          .equals("synset " + idNum)) {
                    return synset;
                }
            }
        }
        throw new DivNotFoundException("Couldn't find synset with id 'synset " + idNum);
    }

    public static <T extends IHasID> void checkEqualIds(Collection<T> col1, Collection<T> col2) {

        if (col1 == null) {
            throw new Error("First collection is null !" + reportIds(col1, col2));
        }

        if (col2 == null) {
            throw new Error("Second collection is null !" + reportIds(col1, col2));
        }

        if (col1.size() != col2.size()) {
            throw new Error("Collections size differs! First:" + col1.size() + " Second:" + col2.size() + reportIds(col1, col2));
        }
        
        Iterator<T> iter = col2.iterator();
        int i = 0;
        for (IHasID hid1 : col1){            
            T hid2 = iter.next();
            if (!hid1.getId().equals(hid2.getId())){
                throw new RuntimeException("Found different ids at position " + i 
                        + "! id1 = " + hid1.getId() + "id2 = " + hid2.getId() + reportIds(col1, col2));
            }
            i++;
        }

    }

    private static <T extends IHasID> String reportIds(Collection<T> col1, Collection<T> col2) {
        StringBuilder sb = new StringBuilder("\n Full collections: \n");
        sb.append("1st: [");
        boolean first = true;
        for (IHasID hd : col1){
            if (first){
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(hd.getId());            
        }
        sb.append("]\n");
        sb.append("2st: [");
        first = true;
        for (IHasID hd : col2){
            if (first){
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(hd.getId());            
        }
        sb.append("]\n");
        
        return sb.toString();
    }

    /**
     * 
     * Checks provided lexical resource corresponds to current db.
     * 
     * Checks only for elements we care about in Diversicon, and only for the
     * ones which are not {@code null} in provided model.
     */
    public static void checkDb(LexicalResource lr, Diversicon diversicon) {
        Internals.checkNotNull(lr);

        LexicalResource dbLr = diversicon.getLexicalResource(lr.getName());

        assertEquals(lr.getName(), dbLr.getName());

        for (Lexicon lex : lr.getLexicons()) {

            try {
                Lexicon dbLex = diversicon.getLexiconById(lex.getId());
                assertEquals(lex.getId(), dbLex.getId());
                checkEqualIds(lex.getSynsets(),
                        dbLex.getSynsets());

                for (Synset syn : lex.getSynsets()) {
                    try {
                        Synset dbSyn = diversicon.getSynsetById(syn.getId());
                        assertEquals(syn.getId(), dbSyn.getId());

                        assertEquals(syn.getSynsetRelations()
                                        .size(),
                                dbSyn.getSynsetRelations()
                                     .size());

                        assertEquals(syn.getDefinitions()
                                        .size(),
                                dbSyn.getDefinitions()
                                     .size());

                        Iterator<Definition> dbDefIter = syn.getDefinitions()
                                                            .iterator();
                        for (Definition definition : syn.getDefinitions()) {
                            Definition dbDef = dbDefIter.next();
                            List<TextRepresentation> textReprs = definition.getTextRepresentations();
                            List<TextRepresentation> dbTextReprs = dbDef.getTextRepresentations();
                            assertEquals(textReprs.size(), dbTextReprs.size());

                            Iterator<TextRepresentation> dbTextReprIter = dbDef.getTextRepresentations()
                                                                               .iterator();
                            for (TextRepresentation tr : textReprs) {
                                TextRepresentation dbTextRepr = dbTextReprIter.next();

                                if (tr.getWrittenText() != null) {
                                    assertEquals(tr.getWrittenText(), dbTextRepr.getWrittenText());
                                }

                            }
                        }

                        Iterator<SynsetRelation> iter = dbSyn.getSynsetRelations()
                                                             .iterator();

                        for (SynsetRelation sr : syn.getSynsetRelations()) {

                            try {
                                SynsetRelation dbSr = iter.next();

                                if (sr.getRelName() != null) {
                                    assertEquals(sr.getRelName(), dbSr.getRelName());
                                }

                                if (sr.getRelType() != null) {
                                    assertEquals(sr.getRelType(), dbSr.getRelType());
                                }

                                if (sr.getSource() != null) {
                                    assertEquals(sr.getSource()
                                                   .getId(),
                                            dbSr.getSource()
                                                .getId());
                                }

                                if (sr.getTarget() != null) {
                                    assertEquals(sr.getTarget()
                                                   .getId(),
                                            dbSr.getTarget()
                                                .getId());
                                }

                                if (sr instanceof DivSynsetRelation) {
                                    DivSynsetRelation divSr = (DivSynsetRelation) sr;
                                    DivSynsetRelation divDbSr = (DivSynsetRelation) dbSr;

                                    assertEquals(divSr.getDepth(), divDbSr.getDepth());

                                    if (divSr.getProvenance() != null
                                            && !divSr.getProvenance()
                                                     .isEmpty()) {
                                        assertEquals(divSr.getProvenance(), divDbSr.getProvenance());
                                    }
                                }
                            } catch (Error ex) {
                                throw new DivException("Error while checking synset relation: "
                                        + Diversicons.toString(sr),
                                        ex);
                            }

                        }
                    } catch (Error ex) {
                        String synId = syn == null ? "null" : syn.getId();
                        throw new DivException("Error while checking synset " + synId, ex);
                    }

                    for (LexicalEntry le : lex.getLexicalEntries()) {
                        try {
                            LexicalEntry dbLe = diversicon.getLexicalEntryById(le.getId());

                            assertEquals(le.getId(), dbLe.getId());

                            assertEquals(
                                    le.getSenses()
                                      .size(),
                                    dbLe.getSenses()
                                        .size());

                            if (le.getLemma() != null) {
                                Lemma lemma = le.getLemma();
                                assertEquals(lemma.getFormRepresentations(),
                                        dbLe.getLemma()
                                            .getFormRepresentations());
                            }

                        } catch (Error err) {
                            String leId = le == null ? "null" : le.getId();
                            throw new DivException("Error while checking lexical entry " + leId, err);
                        }
                    }

                }
            } catch (Error err) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new DivException("Error while checking lexicon " + lexId, err);

            }
        }
    }


    /**
     * Creates an xml file out of the provided
     */
    public static File writeXml(LexicalResource lexicalResource) {
        checkNotNull(lexicalResource);

        DBConfig dbConfig = createNewDbConfig();
        Diversicons.dropCreateTables(dbConfig);
        Diversicon div = Diversicon.connectToDb(dbConfig);
        div.importResource(lexicalResource, true);
        LexicalResource dbLe = div.getLexicalResource(lexicalResource.getName());

        try {
            Path outPath = Files.createTempFile("diversicon-test", "");

            new DBToXMLTransformer(dbConfig, outPath.toString(), null)
                                                                      .transform(dbLe);
            return outPath.toFile();
        } catch (IOException | SAXException ex) {
            throw new DivException("Error while making xml file!", ex);
        } finally {
            div.getSession().close();
        }
        
    }

    
    /**
     * Creates a configuration for a new in-memory H2 datase
     */
    public static DBConfig createNewDbConfig() {
        dbCounter += 1;
        return createDbConfig(dbCounter);
     }

    /**
     * 
     * @param n the number to identify the db. 
     * If -1 db name will be like default in-memory in uby.
     * 
     * @since 0.1
     */
    private static DBConfig createDbConfig(int n) {
        checkArgument(n >= -1, "Invalid n! Found " , n);
        String s;
        if (n == -1){
            s = "";
        } else {
            s = Integer.toString(n);
        }
        return Diversicons.makeDefaultH2InMemoryDbConfig("test" + s);
        
    }

}