package eu.kidf.diversicon.core.test;

import de.tudarmstadt.ukp.lmf.model.core.Definition;
import de.tudarmstadt.ukp.lmf.model.core.GlobalInformation;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.Sense;
import de.tudarmstadt.ukp.lmf.model.core.TextRepresentation;
import de.tudarmstadt.ukp.lmf.model.enums.ELabelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.EPartOfSpeech;
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.meta.SemanticLabel;
import de.tudarmstadt.ukp.lmf.model.morphology.FormRepresentation;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.morphology.WordForm;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.internal.Internals;

import static eu.kidf.diversicon.core.internal.Internals.checkArgument;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;
import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;
import static eu.kidf.diversicon.core.internal.Internals.newArrayList;
import static eu.kidf.diversicon.core.test.DivTester.pid;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Experimental builder helper for {@link LexicalResource} data structures, to
 * use for testing purposes.
 * 
 * The builder will automatically create necessary ids for you, like
 * 'test_lexical-resource-1'
 * , 'test_synset-3', ... according to the order of insertion.
 * 
 * Start building with {@link #lmf()} or {@link #lmf(String)} and finish with
 * {@link #build()}. Each builder instance can build only one object.
 * 
 * @since 0.1.0
 *
 */
// todo implement other id naming policies...
// todo implement all elements builders... huge!
public class LmfBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LmfBuilder.class);

    private LexicalResource lexicalResource;
    private long lastSenseId;

    private boolean built;
    private String prefix;
    private boolean uby;

    /**
     * @since 0.1.0
     */
    private LmfBuilder() {
        this("");
    }

    /**
     * @since 0.1.0
     */
    private LmfBuilder(String prefix) {
        checkNotNull(prefix);
        String name = pid(prefix, "lexical-resource");

        this.prefix = prefix;
        this.lexicalResource = new LexicalResource();
        this.lexicalResource.setName(name);
        GlobalInformation globInfo = new GlobalInformation();
        globInfo.setLabel(prefix + " lexical resource");
        this.lexicalResource.setGlobalInformation(globInfo);
        MetaData md = new MetaData();
        md.setAutomatic(false);
        md.setCreationDate(new Date());
        md.setCreationProcess("for tests");
        md.setCreationTool("by hand");
        md.setId(pid(prefix,"md"));
        md.setVersion("0.1.0-SNAPSHOT");
        this.lexicalResource.setMetaData(Internals.newArrayList(md));
        
        this.built = false;
        this.lastSenseId = 0;
        this.uby = false;

    }
    
    /**
     * Tells the builder to use Uby only classes (so for example
     * will alwyas use  {@link SynsetRelation} instead of {@link DivSynsetRelation})
     * 
     * @since 0.1.0
     */
    public LmfBuilder uby(){
       this.uby = true;
       return this;
    }

    /**
     * @since 0.1.0
     */
    public LmfBuilder lexicon() {
        checkBuilt();
        Lexicon lexicon = new Lexicon();
        lexicon.setId(id("lexicon", lexicalResource.getLexicons()));
        lexicalResource.addLexicon(lexicon);
        return this;
    }

    /**
     * @since 0.1.0
     */
    private Synset getSynset(int idNum) {
        Internals.checkArgument(idNum >= 1, "idNum must be greater than zero! Found instead " + idNum);
        return getSynset(id("synset", idNum));
    }

    /**
     * @since 0.1.0
     */
    private Synset getSynset(String synsetId) {
        checkNotEmpty(synsetId, "Invalid synset id!");

        for (Lexicon lex : lexicalResource.getLexicons()) {
            for (Synset synset : lex.getSynsets()) {
                if (synset.getId()
                          .equals(synsetId)) {
                    return synset;
                }
            }
        }
        LOG.debug("Couldn't find synset with id " + synsetId + ", returning fake synset");
        Synset ret = new Synset();
        ret.setId(synsetId);
        return ret;
    }

    /**
     * Returns something like {@code myprefix_name-3} where 3 is the collection
     * size
     * 
     * @since 0.1.0
     */
    private String id(String name, Collection c) {
        return id(name, (c.size() + 1));
    }

    /**
     * Returns something like {@code myprefix-name-3}
     * 
     * @since 0.1.0
     */
    private String id(String name, long num) {
        checkArgument(num >= 0, "Invalid id number, should be >= 0 !");
        if (name.startsWith(" ") || name.endsWith(" ")) {
            LOG.warn("Found name with spaces at the beginning / end: -->" + name + "<--");
        }
        return pid(prefix, name + "-" + num);
    }

    /**
     * Creates synset with id like 'prefix_synset-n'
     * 
     * @since 0.1.0
     */
    public LmfBuilder synset() {
        Lexicon lexicon = getCurLexicon();
        return synset(lexicon.getSynsets()
                             .size()
                + 1);
    }

    
    /**
     * See {@link #synset()}
     * @since 0.1.0
     */
    public LmfBuilder synset(long id) {
        checkBuilt();
        Synset synset = new Synset();
        Lexicon lexicon = getCurLexicon();
        synset.setId(id("synset", id));

        lexicon.getSynsets()
               .add(synset);
        return this;
    }

    /**
     * Creates a definition and attaches it to current synset
     * 
     * @since 0.1.0
     */
    // todo what about sense definitions?
    public LmfBuilder definition(String writtenText) {
        checkNotEmpty(writtenText, "Invalid written text!");
        Definition def = new Definition();
        TextRepresentation textRepr = new TextRepresentation();
        textRepr.setWrittenText(writtenText);
        def.setTextRepresentations(newArrayList(textRepr));
        getCurSynset().getDefinitions()
                      .add(def);
        return this;
    }

    /**
     * 
     * Adds to current synset a synsetRelation pointing to target synset
     * specified by {@code targetIdNum}
     * 
     * @param targetIdNum
     *            must be > 0.
     * 
     * @since 0.1.0
     */
    public LmfBuilder synsetRelation(String relName, int targetIdNum) {
        checkBuilt();
        checkNotEmpty(relName, "Invalid relation name!");
        Internals.checkArgument(targetIdNum > 0,
                "Expected idNum greater than zero, found " + targetIdNum + " instead!");
        SynsetRelation sr;
        if (uby){
            sr = new SynsetRelation();
        } else {
            sr = new DivSynsetRelation();
        }
        
        sr.setTarget(getSynset(targetIdNum));
        Synset curSynset = getCurSynset();
        sr.setSource(curSynset);
        sr.setRelName(relName);
        curSynset.getSynsetRelations()
                 .add(sr);
        return this;

    }

    /**
     * 
     * Adds to current synset a synsetRelation pointing to target synset
     * specified by {@code targetId}
     *
     * @since 0.1.0
     */
    public LmfBuilder synsetRelation(String relName, String targetId) {
        checkBuilt();
        checkNotEmpty(relName, "Invalid relation name!");
        checkNotNull(targetId);

        SynsetRelation sr;
        if (uby){
            sr = new SynsetRelation();
        } else {
            sr = new DivSynsetRelation();
        }
        
        Synset ts = new Synset();
        ts.setId(targetId);
        sr.setTarget(ts);
        Synset curSynset = getCurSynset();
        sr.setSource(curSynset);
        sr.setRelName(relName);
        curSynset.getSynsetRelations()
                 .add(sr);
        return this;

    }

    /**
     * @since 0.1.0
     */
    public LmfBuilder depth(int i) {
        SynsetRelation sr = getCurSynsetRelation();

        if (sr instanceof DivSynsetRelation) {
            DivSynsetRelation dsr = (DivSynsetRelation) sr;
            dsr.setDepth(i);
        } else {
            if (i != 1){
                throw new IllegalStateException(
                        "Expected " + DivSynsetRelation.class.getCanonicalName()
                        + " Found instead: " + sr.getClass().getCanonicalName());
            }
        }

        return this;
    }

    /**
     * 
     * @param targetIdNum
     *            must be > 0.
     * 
     * @since 0.1.0
     */
    public LmfBuilder synsetRelation(String relName, int sourceIdNum, int targetIdNum) {
        checkBuilt();
        checkNotEmpty(relName, "Invalid relation name!");
        Internals.checkArgument(targetIdNum > 0,
                "Expected idNum greater than zero, found " + targetIdNum + " instead!");
        SynsetRelation sr = new SynsetRelation();
        sr.setTarget(getSynset(targetIdNum));
        Synset source = getSynset(sourceIdNum);
        sr.setSource(getSynset(sourceIdNum));
        sr.setRelName(relName);
        source.getSynsetRelations()
              .add(sr);
        return this;

    }

    /**
     * @since 0.1.0
     */
    private SynsetRelation getCurSynsetRelation() {
        checkBuilt();
        Synset synset = getCurSynset();

        int size = synset.getSynsetRelations()
                         .size();
        if (size == 0) {
            throw new IllegalStateException("There are no synsets relations in current synset " + synset.getId() + "!");
        }
        return synset.getSynsetRelations()
                     .get(size - 1);
    }

    /**
     * @since 0.1.0
     */
    private Synset getCurSynset() {
        checkBuilt();
        Lexicon lexicon = getCurLexicon();
        int size = lexicon.getSynsets()
                          .size();
        if (size == 0) {
            throw new IllegalStateException("There are no synsets in current lexicon " + lexicon.getId() + "!");
        }
        return lexicon.getSynsets()
                      .get(size - 1);
    }

    /**
     * @since 0.1.0
     */
    private LexicalEntry getCurLexicalEntry() {
        checkBuilt();
        Lexicon lexicon = getCurLexicon();
        int size = lexicon.getLexicalEntries()
                          .size();
        if (size == 0) {
            throw new IllegalStateException("There are no lexical entries in current lexicon " + lexicon.getId() + "!");
        }
        return lexicon.getLexicalEntries()
                      .get(size - 1);
    }

    /**
     * Adds a wordform to the current lexical entry. New wordform will have 
     * exactly one form representation with given writtenForm. 

     * @since 0.1.0
     */
    public LmfBuilder wordform(String writtenForm){
        checkBuilt();
        
        WordForm wf = new WordForm();
        FormRepresentation formRepr = new FormRepresentation();
        formRepr.setWrittenForm(writtenForm);
        wf.setFormRepresentations(Internals.newArrayList(formRepr));
                
        List<WordForm> wordForms = getCurLexicalEntry().getWordForms();
        
        if (wordForms == null){            
            getCurLexicalEntry().setWordForms(Internals.newArrayList(wf));
        } else {
            wordForms.add(wf);
        }
        
        return this;
    }
    
    /**
     * @since 0.1.0 
     */
    public WordForm getCurWordForm(){
        checkBuilt();
        LexicalEntry le = getCurLexicalEntry();                                     
        
        int size = le.getWordForms()
                .size();
        
        if (size == 0) {
           throw new IllegalStateException("There are no word forms in current lexical entry" + le.getId() + "!");
        }
        
        return le.getWordForms()
                    .get(size - 1);        
    }
    
    
    /**
     * @since 0.1.0
     */
    private Lexicon getCurLexicon() {
        checkBuilt();
        int size = lexicalResource.getLexicons()
                                  .size();
        if (size == 0) {
            throw new IllegalStateException("There are no lexicons!");
        }
        return lexicalResource.getLexicons()
                              .get(size - 1);
    }

    /**
     * Calls {@link #lmf(String)} with {@link DivTester#DEFAULT_TEST_PREFIX}.
     * 
     * @since 0.1.0
     */
    public static LmfBuilder lmf() {
        return new LmfBuilder(DivTester.DEFAULT_TEST_PREFIX);
    };

    /**
     * Start building a lexical resource. Every id inside will
     * be prepended with {@code prefix} (even for nested elements 
     * such as synsets). No space will be added after the prefix.
     *
     * @since 0.1.0
     */
    public static LmfBuilder lmf(String prefix) {
        return new LmfBuilder(prefix);
    }

    /**
     * Builds a simple minimally valid LexicalResource
     * 
     * @since 0.1.0
     */
    public static LexicalResource simpleLexicalResource() {
        return lmf().lexicon()
                    .synset()
                    .lexicalEntry("a")
                    .build();
    }

    /**
     * @since 0.1.0
     */
    public LexicalResource build() {
        checkBuilt();
        built = true;
        return lexicalResource;
    }

    /**
     * @since 0.1.0
     */
    private void checkBuilt() {
        if (built) {
            throw new IllegalStateException("A LexicalResource was already built with this !");
        }
    }

    /**
     * Creates a LexicalEntry, automatically creating a Sense and Lemma within it. 
     * Sense is linked to current synset.
     * 
     * @since 0.1.0
     */
    public LmfBuilder lexicalEntry(String writtenForm) {
        return lexicalEntry(writtenForm, getCurSynset().getId());
    }

    /**
     * Creates a LexicalEntry, automatically creating a Sense and Lemma within it. 
     * Sense is linked to current synset. LexicalEntry {@code writtenForm}
     * is set equal to "textN", where N is the number of current lexical entries. 
     * 
     * @since 0.1.0
     */
    public LmfBuilder lexicalEntry() {
        
        return lexicalEntry(
                "text" + getCurLexicon().getLexicalEntries(),
                getCurSynset().getId());
    }
        

    /**
     * Creates a LexicalEntry, automatically creating a Sense and Lemma within it. 
     * Sense is linked to provided {@code synsetId}.
     * 
     * @param synsetId
     *            must exist
     * 
     * @since 0.1.0
     */
    public LmfBuilder lexicalEntry(String writtenForm, String synsetId) {
        checkNotEmpty(writtenForm, "Invalid writtenForm!");
        checkNotEmpty(writtenForm, "Invalid writtenForm!");
        checkNotEmpty(synsetId, "Invalid synsetId!");

        checkBuilt();
        LexicalEntry lexEntry = new LexicalEntry();
        lexEntry.setId(id("lexical-entry", getCurLexicon().getLexicalEntries()));
        lexEntry.setPartOfSpeech(EPartOfSpeech.noun);
        Lemma lemma = new Lemma();

        FormRepresentation formRepresentation = new FormRepresentation();
        formRepresentation.setWrittenForm(writtenForm);

        lemma.setFormRepresentations(Arrays.asList(formRepresentation));
        lemma.setLexicalEntry(lexEntry);
        lexEntry.setLemma(lemma);

        Sense sense = newSense(lexEntry, synsetId);
        lexEntry.setSenses(Arrays.asList(sense));

        getCurLexicon().addLexicalEntry(lexEntry);
        return this;
    }

    /**
     * Returns a new Sense within provided {@code lexicalEntry}
     * 
     * @since 0.1.0
     */
    private Sense newSense(LexicalEntry lexicalEntry, String synsetId) {
        Sense sense = new Sense();
        sense.setId(pid(prefix, "sense-" + (lastSenseId + 1)));
        lastSenseId++;

        sense.setLexicalEntry(lexicalEntry);
        sense.setSynset(getSynset(synsetId));

        return sense;
    }

    /**
     * Sets the provenance of the last SynsetRelation created.
     * 
     * @since 0.1.0
     */
    public LmfBuilder provenance(String provenanceId) {
        checkNotNull(provenanceId);
        SynsetRelation sr = getCurSynsetRelation();

        if (sr instanceof DivSynsetRelation) {
            DivSynsetRelation dsr = (DivSynsetRelation) sr;
            dsr.setProvenance(provenanceId);
        } else {
            if (!(provenanceId == null || provenanceId.isEmpty())){
                throw new IllegalStateException(
                        "Expected " + DivSynsetRelation.class.getCanonicalName()
                        + " Found instead: " + sr.getClass());
            }
         
        }

        return this;

    }

    /**
     * Adds a semantic label to current sense.
     * 
     * @since 0.1.0
     */
    public LmfBuilder semanticLabel(String label, ELabelTypeSemantics type){
        SemanticLabel semLabel = new SemanticLabel();
        semLabel.setLabel(label);
        semLabel.setType(type);
        Sense sense = getCurSense();
        sense.addSemanticLabel(semLabel);
        return this;
    }

    /**
     * Returns the current Sense. If there is none, throws IllegalStateException 
     * 
     * @since 0.1.0
     */
    private Sense getCurSense() {
        checkBuilt();
        
        LexicalEntry entry = getCurLexicalEntry();
                
        int size = entry.getSenses()
                          .size();
        if (size == 0) {
            throw new IllegalStateException("There are no senses in current lexical entry " + entry.getId() + "!");
        }
        return entry.getSenses()
                      .get(size - 1);
        
    }
    
}
