package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;
import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.miscellaneous.ConstraintSet;
import de.tudarmstadt.ukp.lmf.model.multilingual.SenseAxis;
import de.tudarmstadt.ukp.lmf.model.semantics.SemanticPredicate;
import de.tudarmstadt.ukp.lmf.model.semantics.SynSemCorrespondence;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.syntax.SubcategorizationFrame;
import de.tudarmstadt.ukp.lmf.model.syntax.SubcategorizationFrameSet;
import de.tudarmstadt.ukp.lmf.transform.LMFDBTransformer;
import eu.kidf.diversicon.core.internal.Internals;

/**
 * 
 * Simple transformer to directly put into the db a LexicalResource complete
 * with all the lexicons, synsets, etc.
 * 
 * @since 0.1.0
 */
class JavaToDbTransformer extends LMFDBTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(JavaToDbTransformer.class);

    private LexicalResource lexicalResource;
    private Iterator<Lexicon> lexiconIter;
    private Iterator<LexicalEntry> lexicalEntryIter;
    private Iterator<SubcategorizationFrame> subcategorizationFrameIter;
    private Iterator<SubcategorizationFrameSet> subcategorizationFrameSetIter;
    private Iterator<SemanticPredicate> semanticPredicateIter;
    private Iterator<SynSemCorrespondence> synSemCorrespondenceIter;
    private Iterator<Synset> synsetIter;
    private Iterator<SenseAxis> senseAxisIter;
    private Iterator<ConstraintSet> constraintSetIter;

    /**
     * Constructor of the transformer.
     * 
     * @param lexRes
     *            a LexicalResource complete with all the lexicons, synsets,
     *            etc. MUST have a {@code name}
     * @throws FileNotFoundException
     * 
     * @since 0.1.0
     */
    @SuppressWarnings("deprecation")
    public JavaToDbTransformer(
            Diversicon div,
            LexicalResource lexRes)
                    throws FileNotFoundException {
        super(div.getDbConfig());        
        sessionFactory.close();  // div dirty but needed...       
        sessionFactory = div.getSessionFactory();
                
        checkNotNull(lexRes);
        checkNotEmpty(lexRes.getName(), "Invalid lexicalResource name!");
        
                
        session = sessionFactory.openSession();
        
        @Nullable
        LexicalResource existingLexicalResource = (LexicalResource) session.get(LexicalResource.class,
                lexRes.getName());
        session.close();
        
        if (existingLexicalResource == null) {
            /** copy to avoid double additions by LMFDBTransformer */
            LexicalResource lexicalResourceCopy = Internals.deepCopy(lexRes);            
            this.lexicalResource = lexicalResourceCopy;
            this.lexicalResource.setLexicons(new ArrayList<Lexicon>());
            this.lexicalResource.setSenseAxes(new ArrayList<SenseAxis>());
        } else {
            LOG.info("Importing into existing lexical resource " + lexRes.getName());
            this.lexicalResource = existingLexicalResource;            
        }

        this.lexiconIter = lexRes.getLexicons()
                                          .iterator();
        this.senseAxisIter = lexRes.getSenseAxes()
                                            .iterator();       
        

    }

    /**
     * @since 0.1.0
     */
    @Override
    protected LexicalResource createLexicalResource() {
        return lexicalResource;
    }

    /**
     * @since 0.1.0
     */    
    @Override
    protected Lexicon createNextLexicon() {
        // resetting lexicon array properties to avoid double additions by
        // LMFDBTransformer
        if (lexiconIter.hasNext()) {
            Lexicon lexicon = Internals.deepCopy(lexiconIter.next());

            LOG.info("Creating Lexicon " + lexicon.getId());

            subcategorizationFrameIter = lexicon.getSubcategorizationFrames()
                                                .iterator();
            lexicon.setSubcategorizationFrames(new ArrayList<SubcategorizationFrame>());
            subcategorizationFrameSetIter = lexicon.getSubcategorizationFrameSets()
                                                   .iterator();
            lexicon.setSubcategorizationFrameSets(new ArrayList<SubcategorizationFrameSet>());
            lexicalEntryIter = lexicon.getLexicalEntries()
                                      .iterator();
            lexicon.setLexicalEntries(new ArrayList<LexicalEntry>());
            semanticPredicateIter = lexicon.getSemanticPredicates()
                                           .iterator();
            lexicon.setSemanticPredicates(new ArrayList<SemanticPredicate>());
            synSemCorrespondenceIter = lexicon.getSynSemCorrespondences()
                                              .iterator();
            lexicon.setSynSemCorrespondences(new ArrayList<SynSemCorrespondence>());
            constraintSetIter = lexicon.getConstraintSets()
                                       .iterator();
            lexicon.setConstraintSets(new ArrayList<ConstraintSet>());
            return lexicon;
        } else {
            return null;
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */    
    @Override
    protected LexicalEntry getNextLexicalEntry() {
        if (lexicalEntryIter.hasNext()) {
            LexicalEntry lexicalEntry = lexicalEntryIter.next();
            synsetIter = lexicalEntry.getSynsets()
                                     .iterator();
            return lexicalEntry;
        } else {
            return null;
        }
    }

    /**
     * Return the next element of the iterator or {@code null} if there is none
     * 
     * @since 0.1.0
     */
    @Nullable
    private static <T> T next(Iterator<T> iter) {
        if (iter != null && iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected SubcategorizationFrame getNextSubcategorizationFrame() {
        return next(subcategorizationFrameIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected SubcategorizationFrameSet getNextSubcategorizationFrameSet() {
        return next(subcategorizationFrameSetIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected SemanticPredicate getNextSemanticPredicate() {
        return next(semanticPredicateIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected Synset getNextSynset() {
        return next(synsetIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected SynSemCorrespondence getNextSynSemCorrespondence() {
        return next(synSemCorrespondenceIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected ConstraintSet getNextConstraintSet() {
        return next(constraintSetIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected SenseAxis getNextSenseAxis() {
        return next(senseAxisIter);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */        
    @Override
    protected void finish() {

    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */    
    @Override
    protected String getResourceAlias() {
        return lexicalResource.getName();
    }

}
