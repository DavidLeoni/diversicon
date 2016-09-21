package it.unitn.disi.diversicon.test;

import de.tudarmstadt.ukp.lmf.model.core.Definition;
import de.tudarmstadt.ukp.lmf.model.core.GlobalInformation;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.Sense;
import de.tudarmstadt.ukp.lmf.model.core.TextRepresentation;
import de.tudarmstadt.ukp.lmf.model.morphology.FormRepresentation;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import it.unitn.disi.diversicon.DivSynsetRelation;
import it.unitn.disi.diversicon.internal.Internals;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;
import static it.unitn.disi.diversicon.internal.Internals.newArrayList;
import static it.unitn.disi.diversicon.test.DivTester.pid;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Experimental builder helper for {@link LexicalResource} data structures, to
 * use for testing purposes.
 * 
 * The builder will automatically create necessary ids for you, like 'lexical-resource 1'
 * , 'synset-3', ... according to the order of insertion.
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
        String name = pid(prefix , "lexical-resource-1");
        
        this.prefix = prefix;
        this.lexicalResource = new LexicalResource();
        this.lexicalResource.setName(name);
        GlobalInformation globInfo = new GlobalInformation();
        globInfo.setLabel("Lexical Resource 1");
        this.lexicalResource.setGlobalInformation(globInfo);
        
        this.built = false;
        this.lastSenseId = 0;

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
        throw new IllegalStateException("Couldn't find a synset with id: '" + synsetId + "'");
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
        if (name.startsWith(" ") || name.endsWith(" ")){
            LOG.warn("Found name with spaces at the beginning / end: -->" + name + "<--");
        }
        return pid(prefix, name + "-" + num);
    }

    /**
     * Creates synset with id like 'synset-n'
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
        DivSynsetRelation sr = new DivSynsetRelation();
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
       
        DivSynsetRelation sr = new DivSynsetRelation();
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
            throw new IllegalStateException(
                    "Expected " + DivSynsetRelation.class.getCanonicalName() + " Found instead: " + sr.getClass()
                                                                                                      .getCanonicalName());
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
     * Start building a lexical resource
     * 
     * @since 0.1.0
     */
    public static LmfBuilder lmf() {
        return new LmfBuilder(DivTester.DEFAULT_TEST_PREFIX);
    };

    /**
     * Start building a lexical resource, prepending every id of every element
     * inside
     * (even nested ones such as synsets) with {@code prefix}. No space will be
     * added after the prefix.
     *
     * @since 0.1.0
     */
    public static LmfBuilder lmf(String prefix) {
        return new LmfBuilder(prefix);
    }

    /**
     * Builds a simple minimalistic LexicalResource
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
     * Automatically creates a Sense and Lemma with given {@code writtenForm}
     * Sense is linked to current synset.
     * 
     * @since 0.1.0
     */
    public LmfBuilder lexicalEntry(String writtenForm) {
        return lexicalEntry(writtenForm, getCurSynset().getId());
    }

    /**
     * Automatically creates a Sense and Lemma with given {@code writtenForm}
     * 
     * 
     * @param synsetIdNum
     *            must exist
     * 
     * @since 0.1.0
     */
    public LmfBuilder lexicalEntry(String writtenForm, String synsetId) {
        checkNotEmpty(writtenForm, "Invalid writtenForm!");
        checkNotEmpty(writtenForm, "Invalid writtenForm!");
        checkNotEmpty(synsetId, "Invalid synsetId!");

        checkBuilt();
        LexicalEntry lexicalEntry = new LexicalEntry();
        lexicalEntry.setId(id("lexicalEntry", getCurLexicon().getLexicalEntries()));
        Lemma lemma = new Lemma();

        FormRepresentation formRepresentation = new FormRepresentation();
        formRepresentation.setWrittenForm(writtenForm);

        lemma.setFormRepresentations(Arrays.asList(formRepresentation));
        lemma.setLexicalEntry(lexicalEntry);
        lexicalEntry.setLemma(lemma);

        Sense sense = newSense(lexicalEntry, synsetId);
        lexicalEntry.setSenses(Arrays.asList(sense));

        getCurLexicon().addLexicalEntry(lexicalEntry);
        return this;
    }

    /**
     * Creates a new Sense within provided {@code lexicalEntry}
     * 
     * @since 0.1.0
     */
    private Sense newSense(LexicalEntry lexicalEntry, String synsetId) {
        Sense sense = new Sense();
        sense.setId(prefix + "sense " + (lastSenseId + 1));
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
            throw new IllegalStateException(
                    "Expected " + DivSynsetRelation.class.getCanonicalName() + " Found instead: " + sr.getClass()
                                                                                                      .getCanonicalName());
        }

        return this;

    }

}
