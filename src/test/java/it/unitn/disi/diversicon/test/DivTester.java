package it.unitn.disi.diversicon.test;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;
import static it.unitn.disi.diversicon.internal.Internals.createTempFile;
import static it.unitn.disi.diversicon.test.LmfBuilder.lmf;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.tudarmstadt.ukp.lmf.model.core.Definition;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.TextRepresentation;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.interfaces.IHasID;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.disi.unitn.diversicon.exceptions.DivNotFoundException;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.ImportJob;
import it.unitn.disi.diversicon.LexResPackage;
import it.unitn.disi.diversicon.exceptions.DivValidationException;
import it.unitn.disi.diversicon.internal.Internals;

public final class DivTester {

    /**
     * @since 0.1.0
     */
    public static final String DEFAULT_TEST_PREFIX = "test";
    
    /**
     * @since 0.1.0
     */
    private static final String MINIMAL_XML =         
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<LexicalResource name=\"lexical resource 1\">   \n" 
            + "     <Lexicon id=\"lexicon 1\">       \n"
            + "         <Synset id=\"synset 1\"/>    \n"        
            + "     </Lexicon>                       \n" 
            + "</LexicalResource>";        
    

    private static final String TEST_RESOURCES_PATH = "it/unitn/disi/diversicon/test/";

    static final String DIVERSICON_TEST_STRING = Internals.DIVERSICON_STRING + "-test-";

    /**
     * Max amount of time for connection problems during tests, in millisecs.
     * 
     * @since 0.1.0
     */
    public static final long MAX_TEST_DELAY = 20000;

    private static int dbCounter = -1;

    
    
    /**
     * 2 verteces and 1 hypernym edge
     * 
     * @since 0.1.0
     */
    public static LexicalResource GRAPH_1_HYPERNYM = lmf().lexicon()
                                                          .synset()
                                                          .lexicalEntry()
                                                          .synset()
                                                          .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                          .build();
   
    
    /**
     * Graph with 3 verteces and 2 hypernym edges,  a good candidate for augmentation. 
     * 
     * @see #DAG_3_HYPERNYM
     * 
     * @since 0.1.0 
     */
    public static LexicalResource GRAPH_3_HYPERNYM = lmf().lexicon()
        .synset()
        .lexicalEntry()
        .synset()
        .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
        .synset()
        .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
        .build();
        
    /**
     * 4 verteces, last one is connected to others by respectively hypernym
     * edge, holonym and 'hello' edge
     * 
     * @since 0.1.0
     */
    public static LexicalResource GRAPH_4_HYP_HOL_HELLO = lmf().lexicon()
                                                               .synset()
                                                               .lexicalEntry()
                                                               .synset()
                                                               .synset()
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .synsetRelation(ERelNameSemantics.HOLONYM, 2)
                                                               .synsetRelation("hello", 3)
                                                               .build();

    /**
     * A full DAG, 3 verteces and 3 hypernyms
     * 
     * @see #GRAPH_3_HYPERNYM
     * 
     * @since 0.1.0
     */
    public static final LexicalResource DAG_3_HYPERNYM = lmf().lexicon()
                                                              .synset()
                                                              .lexicalEntry()
                                                              .synset()
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                              .synset()
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 2)
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                              .depth(2)
                                                              .build();

    /**
     * 2 verteces, second connected to first with two relations.
     * 
     * @since 0.1.0
     */
    public static final LexicalResource DAG_2_MULTI_REL = lmf().lexicon()
                                                               .synset()
                                                               .lexicalEntry()
                                                               .synset()
                                                               .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                               .synsetRelation(ERelNameSemantics.HOLONYM, 1)
                                                               .build();

    private static final Logger LOG = LoggerFactory.getLogger(DivTester.class);

    /**
     * @since 0.1.0
     */
    public static final String EXPERIMENTS_DIR = "src/test/resources/experiments/";

    private DivTester() {
    }

    /**
     * 
     * Retrieves the synset with id 'synset ' + {@code idNum}
     * 
     * @param idNum
     *            index starts from 1
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
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

        checkEqualSize("Invalid id collections!", col1, col2);

        Iterator<T> iter = col2.iterator();
        int i = 0;
        for (IHasID hid1 : col1) {
            T hid2 = iter.next();
            if (!hid1.getId()
                     .equals(hid2.getId())) {
                throw new DifferentCollectionError("Found different ids at position " + i
                        + "! id1 = " + hid1.getId() + "id2 = " + hid2.getId() + reportIds(col1, col2), col1, col2);
            }
            i++;
        }

    }

    private static <T extends IHasID> String reportIds(Collection<T> col1, Collection<T> col2) {
        StringBuilder sb = new StringBuilder("\n Full collections: \n");
        sb.append("1st: [");
        boolean first = true;
        for (IHasID hd : col1) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(hd.getId());
        }
        sb.append("]\n");
        sb.append("2nd: [");
        first = true;
        for (IHasID hd : col2) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(hd.getId());
        }
        sb.append("]\n");

        return sb.toString();
    }

    enum Flags {
        UNORDERED_SYNSET_RELATIONS
    }

    /**
     * returns permutations of numbers from 0 included to size excluded. 
     * i.e. [0,1,2],[0,2,1],[1,0,2], ... 
     * 
     * 
     * @since 0.1.0
     * 
     */
    // Taken from http://stackoverflow.com/a/14444037
    private static List<int[]> permutations(int size) {
        List<int[]> ret = new ArrayList();

        List<Integer> workingList = new ArrayList();
        for (int i = 0; i < size; i++) {
            workingList.add(i);
        }
        permute(workingList, 0, ret);
        return ret;
    }

    /**
     * Taken from http://stackoverflow.com/a/14444037
     * 
     * @since 0.1.0
     * 
     */
    private static void permute(java.util.List<Integer> arr, int k, List<int[]> ret) {
        for (int i = k; i < arr.size(); i++) {
            java.util.Collections.swap(arr, i, k);
            permute(arr, k + 1, ret);
            java.util.Collections.swap(arr, k, i);
        }
        if (k == arr.size() - 1) {
            int[] item = (int[]) Array.newInstance(int.class, arr.size());
            for (int i = 0; i < arr.size(); i++) {
                item[i] = arr.get(i);
            }
            ret.add(item);
        }
    }

    /**
     * 
     * Checks provided lexical resource corresponds to current db. Checks only
     * for elements we care about in Diversicon, and only for the
     * ones which are not {@code null} in provided model.
     * 
     * @param lexRes
     *            MUST have a non-null {@link LexicalResource#getName() name}
     * 
     * @since 0.1.0
     */
    public static void checkDb(LexicalResource lexRes, Diversicon diversicon, Set<Flags> flags) {
        checkNotNull(diversicon);
        Internals.checkNotNull(lexRes);
        checkNotNull(lexRes.getName());

        LexicalResource dbLr = diversicon.getLexicalResource(lexRes.getName());
        checkNotNull(dbLr);

        assertEquals(lexRes.getName(), dbLr.getName());

        for (Lexicon lex : lexRes.getLexicons()) {

            try {
                Lexicon dbLex = diversicon.getLexiconById(lex.getId());
                assertEquals(lex.getId(), dbLex.getId());
                checkEqualIds(lex.getSynsets(),
                        dbLex.getSynsets());

                for (Synset syn : lex.getSynsets()) {
                    checkDbSynset(syn, lex, diversicon, flags);
                }
            } catch (Throwable err) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new DivException("Error while checking lexicon " + lexId, err);

            }
        }

    }

    /**
     * Returns either the id or string "null" if {hasId} is null.
     *
     * @since 0.1.0
     */
    private static String getId(@Nullable IHasID hasId){
        return hasId== null ? "null" : hasId.getId();
    }
    
    /**
     * See {@link #checkDb(LexicalResource, Diversicon)}
     * 
     * @since 0.1.0
     */    
    private static void checkDbSynset(
            Synset syn, 
            Lexicon lex, 
            Diversicon diversicon, 
            Set<Flags> flags) {
        String synId = getId(syn);
        try {
            Synset dbSyn = diversicon.getSynsetById(syn.getId());
            assertEquals(syn.getId(), dbSyn.getId());

            checkEqualSize("Invalid synset relations!", syn.getSynsetRelations(),
                    dbSyn.getSynsetRelations());

            checkEqualSize("Invalid definitions!", syn.getDefinitions(),
                    dbSyn.getDefinitions());

            Iterator<Definition> dbDefIter = syn.getDefinitions()
                                                .iterator();
            for (Definition definition : syn.getDefinitions()) {
                Definition dbDef = dbDefIter.next();
                List<TextRepresentation> textReprs = definition.getTextRepresentations();
                List<TextRepresentation> dbTextReprs = dbDef.getTextRepresentations();
                checkEqualSize("Invalid text representations!", textReprs, dbTextReprs);

                Iterator<TextRepresentation> dbTextReprIter = dbDef.getTextRepresentations()
                                                                   .iterator();
                for (TextRepresentation tr : textReprs) {
                    TextRepresentation dbTextRepr = dbTextReprIter.next();

                    if (tr.getWrittenText() != null) {
                        assertEquals(tr.getWrittenText(), dbTextRepr.getWrittenText());
                    }

                }
            }

            checkDbSynsetRelations(syn, diversicon, flags);
        } catch (Throwable ex) {

            throw new DivException("Error while checking synset " + synId, ex);
        }

        for (LexicalEntry le : lex.getLexicalEntries()) {
            try {
                LexicalEntry dbLe = diversicon.getLexicalEntryById(le.getId());

                assertEquals(le.getId(), dbLe.getId());

                checkEqualSize("Invalid senses!",
                        le.getSenses(),
                        dbLe.getSenses());

                if (le.getLemma() != null) {
                    Lemma lemma = le.getLemma();
                    checkEqualSize("Invalid form represetnations for lemma " + le.getLemma(),
                            lemma.getFormRepresentations(),
                            dbLe.getLemma()
                                .getFormRepresentations());
                }

            } catch (Throwable err) {
                String leId = le == null ? "null" : le.getId();
                throw new DivException("Error while checking lexical entry " + leId, err);
            }
        }

    }

    /**
     * See {@link #checkDb(LexicalResource, Diversicon)}
     * 
     * @since 0.1.0
     */
    private static void checkDbSynsetRelations(Synset syn,  Diversicon diversicon, Set<Flags> flags) {
        
        String synId = getId(syn);
        Synset dbSyn = diversicon.getSynsetById(syn.getId());
        
        List<int[]> permutations;

        if (flags.contains(Flags.UNORDERED_SYNSET_RELATIONS)) {
            permutations = permutations(syn.getSynsetRelations()
                                           .size());
        } else {
            int[] normalPerm = (int[]) Array.newInstance(int.class, syn.getSynsetRelations()
                                                                       .size());

            for (int i = 0; i < syn.getSynsetRelations()
                                   .size(); i++) {
                normalPerm[i] = i;
            }
            permutations = new ArrayList();
            permutations.add(normalPerm);
        }

        boolean foundPermutation = false || syn.getSynsetRelations()
                                               .isEmpty();

        for (int[] permutation : permutations) {
            if (foundPermutation) {
                break;
            }

            try {

                Iterator<SynsetRelation> iter = dbSyn.getSynsetRelations()
                                                     .iterator();

                for (int i = 0; i < permutation.length; i++) {
                    SynsetRelation sr = syn.getSynsetRelations()
                                           .get(permutation[i]);
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
                        foundPermutation = true;
                    } catch (Throwable ex) {
                        throw new DivException("Error while checking synset relation: "
                                + Diversicons.toString(sr),
                                ex);
                    }
                }

            } catch (Throwable ex) {
                LOG.debug("Failed attempt for synset relations permutation " + Arrays.toString(permutation), ex);
            }

        }
        if (!foundPermutation) {
            throw new Error("Synset relations for synset " + synId + " don't match expected ones! (Tried "
                    + permutations.size() + " permutations.)");
        }
    }

    /**
     * See {@link #checkDb(LexicalResource, Diversicon, Set)}
     * 
     * @since 0.1.0
     */
    public static void checkDb(LexicalResource lr, Diversicon diversicon) {
        checkDb(lr, diversicon, new HashSet());
    }

    /**
     * @since 0.1.0
     */
    private static <T> void checkEqualSize(@Nullable String msg, @Nullable Collection<T> col1,
            @Nullable Collection<T> col2) {
        if (col1 == null ^ col2 == null) {
            throw new DifferentCollectionError("One collection is null and the other isnt!", col1, col2);
        }

        if (col1 != null) {
            if (col1.size() != col2.size()) {

                throw new DifferentCollectionError(String.valueOf(msg) + " - Sizes are different ! col1.size = "
                        + col1.size() + "  col2.size = " + col2.size(), col1, col2);
            }
        }
    }

    /**
     * Creates an xml file out of the provided lexical resource. Written 
     * lexical resource will include provided namespaces as {@code xmlns} attributes
     * 
     * @param namespaces Namespaces expressed as prefix : url
     * 
     * @since 0.1.0
     */
    public static File writeXml(
            LexicalResource lexRes, 
            LexResPackage lexResPackage) {
        checkNotNull(lexRes);
        checkNotNull(lexResPackage);
        
        File ret = createTempFile(DivTester.DIVERSICON_TEST_STRING, ".xml").toFile();        
        Diversicons.writeLexResToXml(lexRes, lexResPackage, ret);               

        return ret;        
    }
    
    /**
     * Creates an xml file out of the provided lexical resource and prefix.
     * 
     * Other required parameters will be automatcally 
     * generated in a predictable manner.
     * 
     * @since 0.1.0
     */
    public static File writeXml(LexicalResource lexRes, String prefix) {
        checkNotNull(lexRes);
        Diversicons.checkPrefix(prefix);        
        
        LexResPackage pack = createLexResPackage(lexRes, prefix);
        
        return writeXml(lexRes, pack);
    }
    
    /**
     * Creates a Lexical Resource package automatically filling id, name and namespaces
     * in a predictable manner.
     * 
     * @since 0.1.0
     */
    public static LexResPackage createLexResPackage(LexicalResource lexRes, String prefix){
        LexResPackage pack = new LexResPackage();
        
        pack.setName(lexRes.getName());
        if (lexRes.getGlobalInformation() == null){           
            pack.setLabel(prefix);
        } else {            
            pack.setLabel(lexRes.getGlobalInformation().getLabel());    
        }        
        pack.setPrefix(prefix);
        pack.putNamespace(prefix, "http://test-"+lexRes.hashCode() + ".xml");
        return pack;
    }
    
    /**
     * Creates a Lexical Resource package using the default test prefix
     * 
     * See {@link #createLexResPackage(LexicalResource, String)}
     * 
     * @since 0.1.0
     */
    public static LexResPackage createLexResPackage(LexicalResource lexRes){
        return createLexResPackage(lexRes, DEFAULT_TEST_PREFIX);
    }    
    
    
    
    /**
     * 
     * Imports a resource automatically creating id, prefix and namespaces in a predictable way.
     * 
     * See {@link Diversicon#importResource(LexicalResource, LexResPackage, boolean)
     * 
     * @throws DivException
     * @throws DivValidationException
     * 
     * @since 0.1.0
     */
    public static ImportJob importResource(Diversicon div,
            LexicalResource lexRes,        
            boolean skipAugment) {
                       
        return  div.importResource(lexRes,
                createLexResPackage(lexRes), 
                skipAugment);
    }
    
    /**
     * Creates an XML file out of the provided lexical resource and {@link #DEFAULT_TEST_PREFIX}.
     * 
     * Other required parameters will be automatically generated in a predictable manner.
     * 
     * @since 0.1.0
     */
    public static File writeXml(LexicalResource lexRes) {        
        return writeXml(lexRes, DEFAULT_TEST_PREFIX);
    }

    /**
     * Creates a configuration for a new in-memory H2 database
     * 
     * @since 0.1.0
     */
    public static DBConfig createNewDbConfig() {
        dbCounter += 1;
        return createDbConfig(dbCounter);
    }

    /**
     * 
     * @param n
     *            the number to identify the db.
     *            If -1 db name will be like default in-memory in uby.
     * 
     * @since 0.1.0
     */
    private static DBConfig createDbConfig(int n) {
        checkArgument(n >= -1, "Invalid n! Found ", n);
        String s;
        if (n == -1) {
            s = "";
        } else {
            s = Integer.toString(n);
        }
        return Diversicons.makeDefaultH2InMemoryDbConfig("test" + s, false);

    }

    /**
     * @since 0.1.0
     */
    static Path createTestDir() {
        return Internals.createTempDivDir("test");
    }

    /**
     * @since 0.1.0
     */
    static File writeXml(String content){
        checkNotNull(content);
        
        Path p = DivTester.createTestDir();
        File f = new File(p.toFile(),"test.xml");        
        try {
            FileUtils.writeStringToFile(f, content);
        } catch (IOException ex) {            
            throw new Error("Failed writing xml string to file " + f.getAbsolutePath(), ex);
        }
        return f;
    }
    
    /**
     * Adds default prefix used during tests.
     * 
     * @since 0.1.0
     */
    public static String tid(String name){        
        return pid(DEFAULT_TEST_PREFIX, name);
    }
        
    /**
     * Adds default prefix used during tests.
     * 
     * @since 0.1.0
     */
    public static String pid(String prefix, String name){        
        return prefix + Diversicons.NAMESPACE_SEPARATOR + name;
    }
    
    
    /**
     * Utility functions for developing Diversicon.
     * 
     * Inputs:
     * 
     * <pre>
     *      e FILEPATH : Extracts relType from SynsetRelation rows of OWA XML at FILEPATH
     *      s          : Generates the xsd schema and puts it in src/main/resources/diversicon-1.0.xsd}
     * </pre>
     *
     * @since 0.1.0
     */
    public static void main(String[] args) {

        switch (args[0]) {
        case "e":
            try {
                File file = new File(args[1]);

                Pattern p = Pattern.compile("(.*)<SynsetRelation(.*)relType='(.*)'/>");

                HashSet<String> set = new HashSet<>();

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher m = p.matcher(line);
                        if (m.matches()) {
                            set.add(m.group(3));
                        }
                    }
                }

                for (String s : set) {
                    LOG.info(s);
                }
            } catch (Exception ex) {
                throw new DivException(ex);
            }
            System.exit(0);
        case "s":            
            File f = new File("src/main/resources/diversicon-1.0.xsd");
            Internals.generateXmlSchemaFromDtd(f);            
            System.exit(0);
        default:
            LOG.error("Invalid command " + args[0]);
            System.exit(1);
        }

    }
    
    
}