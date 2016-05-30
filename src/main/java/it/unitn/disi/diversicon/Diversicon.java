package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.api.CriteriaIterator;
import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * Extension of {@link de.tudarmstadt.ukp.lmf.api.Uby Uby} LMF knowledge base
 * with some
 * additional fields in the db to speed up computations
 * 
 * To create instances use {@link #create(DBConfig)} method
 *
 * @since 0.1
 */
public class Diversicon extends Uby {

    private static final Logger LOG = LoggerFactory.getLogger(Diversicon.class);

    /**
     * Amount of items to flush when writing into db with Hibernate.
     */
    private static final int BATCH_FLUSH_COUNT = 20;

    protected Diversicon(DBConfig dbConfig) {
        super(); // so it doesn't open connections! Let's hope they don't delete
                 // it!
        checkNotNull(dbConfig, "database configuration is null");

        this.dbConfig = dbConfig;

        // note: here we are overwriting cfg and sessionFactory
        if (Diversicons.exists(dbConfig)) {
            LOG.info("Reusing existing database at " + dbConfig.getJdbc_url());
            cfg = Diversicons.getHibernateConfig(dbConfig, true);
        } else {
            LOG.info("Database doesn't exist, going to create it");
            Diversicons.dropCreateTables(dbConfig);
            cfg = Diversicons.getHibernateConfig(dbConfig, false);
        }

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        session = sessionFactory.openSession();

    }

    /**
     * Returns the HQL query which finds all of the synsets reachable from
     * {@code synsetId} along paths of
     * {@code relNames}
     * within given depth. In order to actually find them,
     * relations in {@code relNames} must be among the ones for which transitive
     * closure is computed (or their inverses).
     * (see {@link Diversicons#getCanonicalRelations()}).
     * 
     * @param synsetId
     * @param relNames
     *            if none is provided {@link IllegalArgumentException} is thrown
     * @param depth
     *            if -1 all parents until the root are retrieved. If zero
     *            throws {@link IllegalArgumentException}.
     */
    private String getTransitiveSynsetsQuery(
            String synsetId,
            int depth,
            Iterable<String> relNames) {

        checkNotEmpty(synsetId, "Invalid synset id!");
        checkNotEmpty(relNames, "Invalid relation names!");
        checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

        List<String> directRelations = new ArrayList();
        List<String> inverseRelations = new ArrayList();

        for (String relName : relNames) {
            if (Diversicons.isCanonical(relName) || !Diversicons.hasInverse(relName)) {
                directRelations.add(relName);
            } else {
                inverseRelations.add(Diversicons.getInverse(relName));
            }
        }

        String directDepthConstraint;
        String inverseDepthConstraint;
        if (depth == -1) {
            directDepthConstraint = "";
            inverseDepthConstraint = "";
        } else {
            directDepthConstraint = " AND SRD.depth <= " + depth;
            inverseDepthConstraint = " AND SRR.depth <= " + depth;
        }

        String directHsql;
        if (directRelations.isEmpty()) {
            directHsql = "";
        } else {
            directHsql = "  S.id IN "
                    + "             ("
                    + "                 SELECT SRD.target.id"
                    + "                 FROM   SynsetRelation SRD"
                    + "                 WHERE  SRD.source.id = :synsetId"
                    + "                 AND SRD.relName IN " + makeSqlList(directRelations)
                    + directDepthConstraint
                    + "             )";
        }

        String inverseHsql;
        if (inverseRelations.isEmpty()) {
            inverseHsql = "";
        } else {
            inverseHsql = " S.id IN"
                    + "             ("
                    + "                 SELECT SRR.source.id"
                    + "                 FROM   SynsetRelation SRR"
                    + "                 WHERE        SRR.target.id = :synsetId"
                    + "                          AND SRR.relName IN " + makeSqlList(inverseRelations)
                    + inverseDepthConstraint
                    + "              )";

        }

        String orHsql;
        if (!directRelations.isEmpty() && !inverseRelations.isEmpty()) {
            orHsql = " OR ";
        } else {
            orHsql = "";
        }

        String queryString = " "
                + "             FROM Synset S"
                + "             WHERE "
                + directHsql
                + orHsql
                + inverseHsql;

        return queryString;
    }

    /**
     * Finds all of the synsets reachable from {@code synsetId} along paths of
     * {@code relNames}
     * within given depth. In order
     * to actually find them, relations in {@code relNames} must be among the
     * ones for which transitive
     * closure is computed (or their inverses).
     * (see {@link Diversicons#getCanonicalRelations()}).
     * 
     * @param relNames
     *            if none is provided an empty set iterator is returned.
     * @param depth
     *            if -1 all parents until the root are retrieved. If zero
     *            an empty set iterator is returned.
     */
    public Iterator<Synset> getTransitiveSynsets(
            String synsetId,
            int depth,
            Iterable<String> relNames) {

        checkNotEmpty(synsetId, "Invalid synset id!");
        checkNotNull(relNames, "Invalid relation names!");
        checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

        if (!relNames.iterator()
                     .hasNext()
                || depth == 0) {
            return new ArrayList().iterator();
        }

        String queryString = getTransitiveSynsetsQuery(synsetId, depth, relNames);

        Query query = session.createQuery(queryString);
        query
             .setParameter("synsetId", synsetId);

        return query.iterate();
    }

    /**
     * See {{@link #getTransitiveSynsets(String, int, Iterable)}}
     */
    public Iterator<Synset> getTransitiveSynsets(
            String synsetId,
            int depth,
            String... relNames) {
        return getTransitiveSynsets(synsetId, depth, Arrays.asList(relNames));
    }

    /**
     * Augments the synsetRelation graph with transitive closure of
     * {@link Diversicons#getCanonicalRelations() canonical relations}
     * and eventally adds needed symmetric relations.
     * 
     * @since 0.1
     */
    // todo what about provenance? todo instances?
    public void augmentGraph() {

        normalizeGraph();

        computeTransitiveClosure();

    }

    /**
     * Returns {@code true} if {@code source} contains a relation toward
     * {@code target} synset.
     * Returns false otherwise.
     */
    private static boolean containsRel(Synset source, Synset target, String relName) {
        checkNotNull(source, "Invalid source!");
        checkNotNull(target, "Invalid target!");
        checkNotEmpty(relName, "Invalid relName!");

        for (SynsetRelation synRel : source.getSynsetRelations()) {
            if (relName.equals(synRel.getRelName())
                    && Objects.equals(synRel.getTarget()
                                            .getId(),
                            (target.getId()))) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return "'" + s + "'";
    }

    private static String makeSqlList(String[] iterable) {
        return makeSqlList(Arrays.asList(iterable));
    }

    protected static String makeSqlList(Iterable<String> iterable) {
        StringBuilder retb = new StringBuilder("(");

        boolean first = true;
        for (String s : iterable) {
            if (first) {
                retb.append(quote(s));
                first = false;
            } else {
                retb.append(", " + quote(s));
            }
        }
        retb.append(")");
        return retb.toString();
    }

    /*
     * Adds missing edges of depth 1 for relations we consider as canonical.
     */
    private void normalizeGraph() {
        LOG.warn("TODO: SHOULD CHECK FOR LOOPS!");

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        String hql = "FROM Synset";
        Query query = session.createQuery(hql);

        ScrollableResults synsets = query
                                         .setCacheMode(CacheMode.IGNORE)
                                         .scroll(ScrollMode.FORWARD_ONLY);
        int count = 0;

        InsertionStats relStats = new InsertionStats();

        while (synsets.next()) {

            Synset synset = (Synset) synsets.get(0);
            LOG.info("Processing synset with id " + synset.getId() + " ...");

            List<SynsetRelation> relations = synset.getSynsetRelations();

            for (SynsetRelation sr : relations) {
                DivSynsetRelation ssr = (DivSynsetRelation) sr;

                if (Diversicons.hasInverse(ssr.getRelName())) {
                    String inverseRelName = Diversicons.getInverse(ssr.getRelName());
                    if (Diversicons.isCanonical(inverseRelName)
                            && !containsRel(ssr.getTarget(),
                                    ssr.getSource(),
                                    inverseRelName)) {
                        DivSynsetRelation newSsr = new DivSynsetRelation();

                        newSsr.setDepth(1);
                        newSsr.setProvenance(Diversicon.getProvenanceId());
                        newSsr.setRelName(inverseRelName);
                        newSsr.setRelType(Diversicons.getCanonicalRelationType(inverseRelName));
                        newSsr.setSource(ssr.getTarget());
                        newSsr.setTarget(ssr.getSource());

                        ssr.getTarget()
                           .getSynsetRelations()
                           .add(newSsr);
                        session.save(newSsr);
                        session.saveOrUpdate(ssr.getTarget());
                        relStats.inc(inverseRelName);
                    }
                }

            }

            if (++count % BATCH_FLUSH_COUNT == 0) {
                // flush a batch of updates and release memory:
                session.flush();
                session.clear();
            }
        }

        tx.commit();

        LOG.info("");
        LOG.info("Done normalizing SynsetRelations:");
        LOG.info("");
        LOG.info(relStats.toString());
    }

    /**
     * Before calling this, the graph has to be normalized by calling
     * {@link #normalizeGraph()}
     */
    private void computeTransitiveClosure() {

        LOG.info("Computing transitive closure for SynsetRelations ...");

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        InsertionStats relStats = new InsertionStats();

        int depthToSearch = 1;
        int count = 0;

        // As: the edges computed so far
        // Bs: original edges

        String hqlSelect = "    SELECT SR_A.source, SR_B.target,  SR_A.relName"
                + "      FROM SynsetRelation SR_A, SynsetRelation SR_B"
                + "      WHERE"
                + "          SR_A.relName IN " + makeSqlList(Diversicons.getCanonicalRelations())
                + "      AND SR_A.depth = :depth"
                + "      AND SR_B.depth = 1"
                + "      AND SR_A.relName = SR_B.relName"
                + "      AND SR_A.target = SR_B.source"
                // don't want to add twice edges
                + "      AND NOT EXISTS"
                + "             ("
                + "               "
                + "               FROM SynsetRelation SR_C"
                + "               WHERE "
                + "                         SR_A.relName = SR_C.relName"
                + "                    AND  SR_A.source=SR_C.source"
                + "                    AND  SR_B.target=SR_C.target"
                + "             )";

        int processedRelationsInCurLevel = 0;

        do {
            processedRelationsInCurLevel = 0;

            // log.info("Augmenting SynsetRelation graph with edges of depth " +
            // (depthToSearch + 1) + " ...");

            Query query = session.createQuery(hqlSelect);
            query.setParameter("depth", depthToSearch);

            ScrollableResults results = query
                                             .setCacheMode(CacheMode.IGNORE)
                                             .scroll(ScrollMode.FORWARD_ONLY);
            while (results.next()) {

                Synset source = (Synset) results.get(0);
                Synset target = (Synset) results.get(1);
                String relName = (String) results.get(2);

                DivSynsetRelation ssr = new DivSynsetRelation();
                ssr.setDepth(depthToSearch + 1);
                ssr.setProvenance(Diversicon.getProvenanceId());
                ssr.setRelName(relName);
                ssr.setRelType(Diversicons.getCanonicalRelationType(relName));
                ssr.setSource(source);
                ssr.setTarget(target);

                source.getSynsetRelations()
                      .add(ssr);
                session.save(ssr);
                session.saveOrUpdate(source);
                // log.info("Inserted " + ssr.toString());
                relStats.inc(relName);
                processedRelationsInCurLevel += 1;

                if (++count % BATCH_FLUSH_COUNT == 0) {
                    // flush a batch of updates and release memory:
                    session.flush();
                    session.clear();
                }
            }

            depthToSearch += 1;

        } while (processedRelationsInCurLevel > 0);

        tx.commit();

        LOG.info("");
        LOG.info("Done computing transitive closure for SynsetRelations:");
        LOG.info("");
        LOG.info(relStats.toString());
    }

    /**
     * See {@link #importFiles(Collection, Collection)}
     */
    public void importFiles(String filepath,
            String lexicalResourceName) {

        importFiles(Arrays.asList(filepath), Arrays.asList(lexicalResourceName));
    }

    /**
     * imports provided resources into db and automatically augments graph with
     * transitive
     * closure at the end of the loading.
     * 
     * @param filepaths
     *            paths to lmf xml files
     * @param lexicalResourceName
     *            todo meaning? name seems not be required to be in the xml
     */
    // todo think about .sql files
    public void importFiles(Collection<String> filepaths,
            Collection<String> lexicalResourceNames) {

        checkNotEmpty(filepaths, "invalid filepaths length!");
        checkNotEmpty(lexicalResourceNames, "invalid lexicalResourceNames length!");

        Internals.checkArgument(filepaths.size() == lexicalResourceNames.size(),
                "Lexical resource names don't match with files! Found "
                        + filepaths.size() + " filepaths and " + lexicalResourceNames.size() + " resource names");

        Iterator<String> namesIter = lexicalResourceNames.iterator();

        for (String filepath : filepaths) {
            String lexicalResourceName = namesIter.next();

            LOG.info("Loading LMF : " + filepath + " with lexical resource name " + lexicalResourceName + " ...");

            XMLToDBTransformer trans = new XMLToDBTransformer(dbConfig);

            try {
                trans.transform(new File(filepath), lexicalResourceName);
            } catch (Exception ex) {
                throw new DivException("Error while loading lmf xml " + filepath, ex);
            }

            LOG.info("Done loading LMF : " + filepath + " with lexical resource name " + lexicalResourceName + " .");
        }

        try {
            augmentGraph();
        } catch (Exception ex) {
            throw new DivException("Error while augmenting graph with computed edges!", ex);
        }

        LOG.info("Done loading LMFs.");
    }

    /**
     * 
     * Saves a LexicalResource complete with all the lexicons, synsets, etc into
     * a database. This method is suitable only for small lexical resources,
     * generally for testing purposes. If you have a big resource, stream the
     * loading by providing your implementation of <a href=
     * "https://github.com/dkpro/dkpro-uby/blob/master/de.tudarmstadt.ukp.uby.persistence.transform-asl/src/main/java/de/tudarmstadt/ukp/lmf/transform/LMFDBTransformer.java"
     * target="_blank"> LMFDBTransformer</a> and then call {@code transform()}
     * on it
     * instead.
     * 
     * @param lexicalResourceId
     *            todo don't know well the meaning
     * 
     * @throws DivException
     * @since 0.1
     */
    public void importResource(
            LexicalResource lexicalResource,
            String lexicalResourceId) {
        LOG.info("Going to save lexical resource to database...");
        try {
            new JavaToDbTransformer(dbConfig, lexicalResource, lexicalResourceId).transform();
        } catch (Exception ex) {
            throw new DivException("Error when importing lexical resource " + lexicalResourceId + " !", ex);
        }
        LOG.info("Done saving.");
    }

    /**
     * Returns the fully qualified package name.
     */
    public static String getProvenanceId() {
        return Diversicon.class.getPackage()
                               .getName();
    }

    /**
     * Creates an instance of a Diversicon.
     * 
     * @param dbConfig
     */
    public static Diversicon create(DBConfig dbConfig) {
        Diversicon ret = new Diversicon(dbConfig);
        return ret;
    }

    /**
     * 
     * Returns true if {@code targetSynset} is reachable from
     * {@code sourceSynset} along some
     * path of {@code relNames} within given depth. In order to actually find
     * them,
     * relations in {@code relNames} must be among the ones for which transitive
     * closure is computed (or their inverses).
     * (see {@link Diversicons#getCanonicalRelations()}).
     * 
     * @param sourceSynset
     *            the source synset
     * @param targetSynset
     *            the target synset
     * @param depth
     *            the maximum number of edges explored along any path.
     *            if {@code -1} full paths are explored. If {@code zero}
     *            returns true only if source and target coincide.
     * @param relNames
     *            if none is provided returns true only if source and target
     *            coincide.
     * 
     */
    public boolean isReachable(Synset sourceSynset, Synset targetSynset,
            int depth, List<String> relNames) {

        checkNotNull(sourceSynset, "Invalid source synset!");
        checkNotEmpty(sourceSynset.getId(), "Invalid source synset id!");
        checkNotNull(targetSynset, "Invalid target synset!");
        checkNotNull(relNames, "Invalid relation names!");

        checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

        if (sourceSynset.getId()
                        .equals(targetSynset.getId())) {
            return true;
        }

        if (relNames.isEmpty()) {
            return false;
        }

        String queryString = "     SELECT 'TRUE'"
                + "   FROM Synset"
                + "   WHERE :targetSynset IN "
                + "   ("
                + getTransitiveSynsetsQuery(sourceSynset.getId(), depth, relNames)
                + "   )";

        /*
         * this query compiles:
         * String queryString =
         * "     SELECT 'TRUE'"
         * + "   FROM Synset"
         * + "   WHERE :targetSynset IN "
         * + "   (SELECT 'bastard'"
         * + "    FROM Synset S"
         * + "    WHERE S.id = :synsetId"
         * // + getTransitiveSynsetsQuery(sourceSynset.getId(), depth, relNames)
         * + "   )";
         */

        Query query = session.createQuery(queryString);
        query
             .setParameter("synsetId", sourceSynset.getId())
             .setParameter("targetSynset", targetSynset);

        return query.iterate()
                    .hasNext();
    }

}
