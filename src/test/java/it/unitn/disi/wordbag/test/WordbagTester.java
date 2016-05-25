package it.unitn.disi.wordbag.test;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import it.unitn.disi.wordbag.WbException;
import it.unitn.disi.wordbag.WbNotFoundException;
import it.unitn.disi.wordbag.WbSynsetRelation;
import it.unitn.disi.wordbag.Wordbags;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

final public class WordbagTester {

    private WordbagTester() {

    }

    /**
     * 
     * Retrieves the synset with id 'synset ' + {@code idNum}
     * 
     * @param idNum
     *            index starts from 1
     * @throws WbNotFoundException
     */
    public static Synset getSynset(LexicalResource lr, int idNum) {
        Wordbags.checkArgument(idNum >= 1, "idNum must be positive, found instead " + idNum);

        for (Lexicon lexicon : lr.getLexicons()) {
            for (Synset synset : lexicon.getSynsets()) {
                if (synset.getId()
                          .equals("synset " + idNum)) {
                    return synset;
                }
            }
        }
        throw new WbNotFoundException("Couldn't find synset with id 'synset " + idNum);
    }

    /**
     * 
     * Checks provided lexical resource corresponds to current db.
     * 
     * Checks only for elements we care about in S-Match Uby, and only for the
     * ones which are not {@code null} in provided model.
     */
    public static void checkDb(LexicalResource lr, Uby uby) {
        Wordbags.checkNotNull(lr);

        LexicalResource ulr = uby.getLexicalResource(lr.getName());

        assertEquals(lr.getName(), ulr.getName());

        for (Lexicon lex : lr.getLexicons()) {

            try {
                Lexicon uLex = uby.getLexiconById(lex.getId());
                assertEquals(lex.getId(), uLex.getId());
                assertEquals(lex.getSynsets()
                                .size(),
                        uLex.getSynsets()
                            .size());

                for (Synset syn : lex.getSynsets()) {
                    try {
                        Synset uSyn = uby.getSynsetById(syn.getId());
                        assertEquals(syn.getId(), uSyn.getId());

                        assertEquals(syn.getSynsetRelations()
                                        .size(),
                                uSyn.getSynsetRelations()
                                    .size());

                        Iterator<SynsetRelation> iter = uSyn.getSynsetRelations()
                                                            .iterator();

                        for (SynsetRelation sr : syn.getSynsetRelations()) {

                            try {
                                SynsetRelation usr = iter.next();

                                if (sr.getRelName() != null) {
                                    assertEquals(sr.getRelName(), usr.getRelName());
                                }

                                if (sr.getRelType() != null) {
                                    assertEquals(sr.getRelType(), usr.getRelType());
                                }

                                if (sr.getSource() != null) {
                                    assertEquals(sr.getSource()
                                                   .getId(),
                                            usr.getSource()
                                               .getId());
                                }

                                if (sr.getTarget() != null) {
                                    assertEquals(sr.getTarget()
                                                   .getId(),
                                            usr.getTarget()
                                               .getId());
                                }

                                if (sr instanceof WbSynsetRelation) {
                                    WbSynsetRelation Wbsr = (WbSynsetRelation) sr;
                                    WbSynsetRelation Wbusr = (WbSynsetRelation) usr;

                                    assertEquals(Wbsr.getDepth(), Wbusr.getDepth());

                                    if (Wbsr.getProvenance() != null) {
                                        assertEquals(Wbsr.getProvenance(), Wbusr.getProvenance());
                                    }
                                }
                            } catch (Error ex) {
                                throw new WbException("Error while checking synset relation: " + Wordbags.toString(sr),
                                        ex);
                            }

                        }
                    } catch (Error ex) {
                        String synId = syn == null ? "null" : syn.getId();
                        throw new WbException("Error while checking synset " + synId, ex);
                    }
                }
            } catch (Error ex) {
                String lexId = lex == null ? "null" : lex.getId();
                throw new WbException("Error while checking lexicon " + lexId, ex);

            }
        }
    }
}
