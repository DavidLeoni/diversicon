package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkEquals;
import static it.unitn.disi.diversicon.internal.Internals.checkLexResPackage;
import static it.unitn.disi.diversicon.internal.Internals.checkNotBlank;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.Diversicons.checkId;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.Deflater;

import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.h2.tools.Script;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.DBToXMLTransformer;
import it.unitn.disi.diversicon.exceptions.InvalidStateException;
import it.unitn.disi.diversicon.exceptions.DivException;
import it.unitn.disi.diversicon.exceptions.DivIoException;
import it.unitn.disi.diversicon.exceptions.DivNotFoundException;
import it.unitn.disi.diversicon.exceptions.DivValidationException;
import it.unitn.disi.diversicon.exceptions.InterruptedImportException;
import it.unitn.disi.diversicon.exceptions.InvalidImportException;
import it.unitn.disi.diversicon.exceptions.InvalidSchemaException;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * Extension of {@link de.tudarmstadt.ukp.lmf.api.Uby Uby} LMF knowledge base
 * with some
 * additional fields in the db to speed up computations. In particular, original
 * {@link SynsetRelation}
 * is extended by {@link DivSynsetRelation} by adding {@code depth} and
 * {@code provenance} edges.
 * 
 * To create instances use {@link #connectToDb(DBConfig)} method
 * 
 * <h3>Concurrency</h3>
 * <p>
 * A Diversicon opens a hibernate {@link org.hibernate.Session Session} with the
 * DB
 * upon instance creation, as Uby does. This implies a single instance of
 * Diversicon <i> cannot</i> be shared among concurrent processes,
 * see <a href=
 * "https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html#transactions-basics"
 * target="_blank">Hibernate documentation</a>.
 * TODO maybe it's possible to have one different instance per thread.
 * </p>
 *
 * @since 0.1.0
 */
public class Diversicon extends Uby {

    

    private static final Logger LOG = LoggerFactory.getLogger(Diversicon.class);    
    
    /**
     * Time between progress reports in millisecs
     */
    private static final int LOG_DELAY = 5000;

    /**
     * Amount of items to flush when writing into db with Hibernate.
     */
    private static int COMMIT_STEP = 10000;
    
    /**
     * Maps a Diversicon Session hashcode to its Diversicon
     * 
     * @since 0.1.0
     */
    private static final Map<Integer, Diversicon> INSTANCES = new HashMap<>();
    

    /**
     * @throws DivIoException
     * @throws InvalidSchemaException
     * 
     * @since 0.1.0
     */
    protected Diversicon(DBConfig dbConfig) {
        super(); // so it doesn't open connections! Let's hope they don't delete
                 // it!

        checkNotNull(dbConfig, "database configuration is null");

        this.dbConfig = dbConfig;

        LOG.info("Connecting to database   " + dbConfig.getJdbc_url() + "   ...");

        cfg = Diversicons.checkSchema(dbConfig);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        session = sessionFactory.openSession();

        int hashcode = session.hashCode();
        if (INSTANCES.containsKey(hashcode)) {
            throw new DivException("INTERNAL ERROR: Seems like there is some sort of duplicate Diversicon session!!");
        }
        INSTANCES.put(hashcode, this);

    }

    /**
     * Note: search is done by exact match on {@code writtenForm}
     * 
     * @since 0.1.0
     */
    // todo review, not so clear how lemmas are supposed to work
    public List<Lemma> getLemmasByWrittenForm(String writtenForm) {
        checkNotEmpty(writtenForm, "Invalid written form!");

        // searching only in lemmas because UBY wordnet only generates lemmas
        // FormRepresentations
        // and doesn't create any WordForm (see
        // https://github.com/dkpro/dkpro-uby/blob/5cab2846e3c27069c08ebd3bf91bd5a6f8ed02ca/de.tudarmstadt.ukp.uby.integration.wordnet-gpl/src/main/java/de/tudarmstadt/ukp/lmf/transform/wordnet/LexicalEntryGenerator.java#L271)

        @SuppressWarnings("unchecked")
        List<Lemma> lemmas = session.createCriteria(Lemma.class)
                                    .createCriteria("formRepresentations")
                                    .add(Restrictions.like("writtenForm", writtenForm))
                                    .list();

        return lemmas;
    }

    /**
     * See {@link #getLemmasByWrittenForm(String)}.
     * 
     * @since 0.1.0
     */
    public List<String> getLemmaStringsByWrittenForm(String writtenForm) {
        List<Lemma> lemmas = getLemmasByWrittenForm(writtenForm);

        List<String> ret = new ArrayList();
        for (Lemma lemma : lemmas) {
            if (lemma.getFormRepresentations()
                     .isEmpty()) {
                LOG.error(
                        "Found a lemma with no form representation! Lemma's lexical entry is " + lemma.getLexicalEntry()
                                                                                                      .getId());
            } else {
                ret.add(lemma.getFormRepresentations()
                             .get(0)
                             .getWrittenForm());
            }
        }
        return ret;
    }

    /**
     * Finds all of the synsets to which {@code synsetId} is connected with
     * edges of
     * {@code relNames}
     * having depth less or equal the given one.
     * 
     * @param relNames
     *            if none is provided an empty set iterator is returned.
     * @param depth
     *            the maximum depth edges can have. If -1 no depth limit is
     *            applied.
     *            If zero an empty set iterator is returned.
     * 
     * @since 0.1.0
     */
    public Iterator<Synset> getConnectedSynsets(
            String synsetId,
            int depth,
            Iterable<String> relNames) {

        checkId(synsetId, "Invalid synset id!");
        checkNotNull(relNames, "Invalid relation names!");
        checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

        if (!relNames.iterator()
                     .hasNext()
                || depth == 0) {
            return new ArrayList().iterator();
        }

        List<String> directRelations = new ArrayList();
        List<String> inverseRelations = new ArrayList();

        for (String relName : relNames) {
            if (Diversicons.isCanonicalRelation(relName) || !Diversicons.hasInverse(relName)) {
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
        } else { // UNION doesn't work in hibernate
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

        Query query = session.createQuery(queryString);
        query
             .setParameter("synsetId", synsetId);

        return query.iterate();
    }

    /**
     * See {{@link #getConnectedSynsets(String, int, Iterable)}}
     * 
     * @since 0.1.0
     */
    public Iterator<Synset> getConnectedSynsets(
            String synsetId,
            int depth,
            String... relNames) {
        return getConnectedSynsets(synsetId, depth, Arrays.asList(relNames));
    }

    /**
     * Validates, normalizes and augments the synsetRelation graph with edges to
     * speed up
     * searches.
     * 
     * @throws DivValidationException
     * 
     * @since 0.1.0
     */
    // todo what about provenance? todo instances?
    public void processGraph() {

        validateGraph();

        normalizeGraph();

        computeTransitiveClosure();

    }

    /**
     * Validates input graph. For now checks are minimal.
     * 
     * 
     * 
     * @throws DivValidationException
     * 
     * @since 0.1.0
     */
    private void validateGraph() {

        LOG.info("");
        LOG.info("Executing post-import db validation ... ");
        LOG.info("");
        
        Transaction tx = null;
        Date checkpoint = new Date();

        Date start = checkpoint;

        try {
            tx = session.beginTransaction();

            String hql = "FROM SynsetRelation SR"
                    + "   WHERE "
                    + "         SR.source = SR.target"
                    + "    AND  SR.relName IN " + makeSqlList(Diversicons.getCanonicalTransitiveRelations());
            Query query = session.createQuery(hql);

            ScrollableResults synsetRelations = query
                                                     .setCacheMode(CacheMode.IGNORE)
                                                     .scroll(ScrollMode.FORWARD_ONLY);
            int count = 0;

            while (synsetRelations.next()) {

                SynsetRelation sr = (SynsetRelation) synsetRelations.get(0);

                LOG.error("Found transitive canonical SynsetRelation with a self loop: " + Diversicons.toString(sr));
                count += 1;
            }

            if (count > 0) {
                throw new DivValidationException("Found " + count + " invalid relations!");
            } else {
                DbInfo dbInfo = getDbInfo();
                dbInfo.setToValidate(false);
                session.saveOrUpdate(dbInfo);
                tx.commit();
            }

            LOG.info("DB is valid!");
            LOG.info("");
            LOG.info("   Elapsed time: " + Internals.formatInterval(start, new Date()));
            LOG.info("");

        } catch (Exception ex) {
            LOG.error("Error while validating graph! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivValidationException("Error while validating the graph!", ex);
        }

    }

    /**
     * Returns the {@link DbInfo}
     * 
     * @throws DivNotFoundException
     *             if dbInfo is not found
     * @since 0.1.0
     */
    public DbInfo getDbInfo() {

        Criteria crit = session.createCriteria(DbInfo.class);
        crit.setMaxResults(50);
        DbInfo ret = (DbInfo) crit.uniqueResult();

        if (ret == null) {
            throw new DivNotFoundException("Couldn't find DbInfo in the database!!");
        } else {
            return ret;
        }
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

    /**
     * @since 0.1.0
     */
    public long getSynsetCount() {
        return ((Number) session.createCriteria(Synset.class)
                                .setProjection(Projections.rowCount())
                                .uniqueResult()).longValue();

    }

    /*
     * Adds missing edges of depth 1 for relations we consider as canonical.
     * 
     * @throws DivException
     *
     * @since 0.1.0
     */
    private void normalizeGraph() {

        checkArgument(!getDbInfo().isToValidate(), "Tried to normalize a graph which is yet to validate!");

        LOG.info("Normalizing SynsetRelations...");

        Transaction tx = null;
        Date checkpoint = new Date();

        Date start = checkpoint;

        try {
            tx = session.beginTransaction();

            long totalSynsets = getSynsetCount();

            String hql = "FROM Synset";
            Query query = session.createQuery(hql);

            ScrollableResults synsets = query
                                             .setCacheMode(CacheMode.IGNORE)
                                             .scroll(ScrollMode.FORWARD_ONLY);
            int count = 0;

            InsertionStats insStats = new InsertionStats();

            insStats.setEdgesPriorInsertion(getSynsetRelationsCount());

            LOG.info("\nFound " + totalSynsets + " synsets.\n");

            while (synsets.next()) {

                Synset synset = (Synset) synsets.get(0);
                LOG.trace("Processing synset with id " + synset.getId() + " ...");

                List<SynsetRelation> relations = synset.getSynsetRelations();

                for (SynsetRelation sr : relations) {
                    DivSynsetRelation ssr = (DivSynsetRelation) sr;

                    if (Diversicons.hasInverse(ssr.getRelName())) {
                        String inverseRelName = Diversicons.getInverse(ssr.getRelName());
                        if (Diversicons.isCanonicalRelation(inverseRelName)
                                && Diversicons.isTransitive(inverseRelName)
                                // don't want to add cycles
                                && !(ssr.getSource()
                                        .getId()
                                        .equals(ssr.getTarget()
                                                   .getId()))
                                && !containsRel(ssr.getTarget(),
                                        ssr.getSource(),
                                        inverseRelName)) {
                            DivSynsetRelation newSsr = new DivSynsetRelation();

                            newSsr.setDepth(1);
                            newSsr.setProvenance(Diversicon.getProvenanceId());
                            newSsr.setRelName(inverseRelName);
                            newSsr.setRelType(Diversicons.getRelationType(inverseRelName));
                            newSsr.setSource(ssr.getTarget());
                            newSsr.setTarget(ssr.getSource());

                            ssr.getTarget()
                               .getSynsetRelations()
                               .add(newSsr);
                            session.save(newSsr);
                            session.saveOrUpdate(ssr.getTarget());
                            insStats.inc(inverseRelName);
                        }
                    }

                }

                if (++count % COMMIT_STEP == 0) {
                    // flush a batch of updates and release memory:
                    session.flush();
                    session.clear();
                    checkpoint = reportLog(checkpoint, "SynsetRelation normalization - processed synsets", count);
                }
            }

            DbInfo dbInfo = getDbInfo();
            dbInfo.setToNormalize(false);
            session.saveOrUpdate(dbInfo);

            tx.commit();

            LOG.info("");
            LOG.info("Done normalizing SynsetRelations.");
            LOG.info("");
            LOG.info(insStats.toString());
            LOG.info("   Elapsed time: " + Internals.formatInterval(start, new Date()));
            LOG.info("");

        } catch (Exception ex) {
            LOG.error("Error while normalizing graph! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivException("Error while computing normalizing graph!", ex);
        }
    }

    /**
     * 
     * @param checkpoint
     *            last time in millisecs a message was displayed
     * @param msg
     *            something like: "SynsetRelations normalization"
     * @param itemsName
     *            i.e. 'edges' or 'synsets'
     * 
     * @since 0.1.0
     */
    protected Date reportLog(Date checkpoint, @Nullable String msg, long count) {
        try {
            checkNotNull(checkpoint);
            checkArgument(count >= 0);
        } catch (IllegalArgumentException ex) {
            LOG.error("Error while reporting counts!", ex);
        }

        Date newTime = new Date();
        if ((newTime.getTime() - checkpoint.getTime()) > LOG_DELAY) {
            LOG.info(String.valueOf(msg) + ": " + Internals.formatInteger(count));
            return newTime;
        } else {
            return checkpoint;
        }
    }

    /**
     * Computes the transitive closure of
     * {@link Diversicons#getCanonicalTransitiveRelations() canonical relations}
     * 
     * Before calling this, the graph has to be normalized by calling
     * {@link #normalizeGraph()}
     * 
     * Caveats: Hibernate does not support recursive queries:
     * 
     * @throws DivException
     *             when transaction goes wrong it is automatically rolled back
     *             and DivException is thrown
     * 
     * @since 0.1.0
     */
    private void computeTransitiveClosure() {
        Date start = new Date();

        LOG.info("Computing transitive closure for SynsetRelations (may take some minutes) ...");

        checkArgument(!getDbInfo().isToNormalize(),
                "Tried to compute transitive closure of a graph which is yet to normalize!");

        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            InsertionStats relStats = new InsertionStats();

            relStats.setEdgesPriorInsertion(getSynsetRelationsCount());

            LOG.info("\n   Found " + Internals.formatInteger(relStats.getEdgesPriorInsertion())
                    + " synset relations.\n");

            int count = 0;

            // As: the edges computed so far
            // Bs: original edges

            // NOTE: THIS ONE IS FAST BUT STILL COMPUTES DUPLICATES !
            String sqlSelect = "  "
                    + " WITH RECURSIVE SR_A(synsetId, relName, target, depth) AS ("
                    + "    ("
                    + "        SELECT synsetId, relName, target, depth"
                    + "        FROM SynsetRelation"
                    + "        WHERE depth = 1"
                    + "              AND relName IN  " + makeSqlList(Diversicons.getCanonicalTransitiveRelations())
                    + "    )"
                    + "    UNION ALL"
                    + "    ("
                    + "        SELECT SR_A.synsetId, SR_A.relName, SR_B.target, (SR_A.depth + 1)"
                    + "        FROM SR_A, SynsetRelation SR_B"
                    + "        WHERE"
                    + "            SR_A.relName = SR_B.relName"
                    + "        AND SR_A.target = SR_B.synsetId"
                    + "        AND SR_B.depth = 1  "
                    + "    )"
                    + " ) ((SELECT synsetId, relName, target,  depth"
                    + " FROM  SR_A)"
                    + " MINUS " // doesn't remove all duplicates, but can be
                                // enough for now
                    + " (SELECT synsetId, relName, target,  depth"
                    + " FROM SynsetRelation)"
                    + " )";

            Date checkpoint = new Date();

            SQLQuery query = session.createSQLQuery(sqlSelect);

            ScrollableResults results = query
                                             .setCacheMode(CacheMode.IGNORE)
                                             .scroll(ScrollMode.FORWARD_ONLY);

            LOG.info("\n   Elapsed time: " + Internals.formatInterval(start, new Date()) + "\n");
            LOG.info("\nGoing to write closure into the db...\n");

            while (results.next()) {

                Synset source = (Synset) session.get(Synset.class, (String) results.get(0));
                String relName = (String) results.get(1);
                String targetId = (String) results.get(2);
                int depth;
                Object depthCandidate = results.get(3);
                if (depthCandidate instanceof String) {
                    depth = Integer.parseInt((String) depthCandidate);
                } else if (depthCandidate instanceof Integer || depthCandidate instanceof Long) {
                    depth = (int) results.get(3);
                } else {
                    throw new DivException("Internal error, couldn't parse depth " + depthCandidate + " its class is "
                            + depthCandidate.getClass()
                                            .getName());
                }

                if (depth > relStats.getMaxLevel()) {
                    relStats.setMaxLevel(depth);
                }

                DivSynsetRelation ssr = new DivSynsetRelation();

                ssr.setDepth(depth);
                ssr.setProvenance(Diversicon.getProvenanceId());
                ssr.setRelName(relName);
                ssr.setRelType(Diversicons.getRelationType(relName));
                ssr.setSource(source);
                Synset targetSynset = new Synset();
                targetSynset.setId(targetId);
                ssr.setTarget(targetSynset);

                source.getSynsetRelations()
                      .add(ssr);
                session.save(ssr);
                session.merge(source);

                relStats.inc(relName);

                if (++count % COMMIT_STEP == 0) {
                    // flush a batch of updates and release memory:
                    session.flush();
                    session.clear();
                    checkpoint = reportLog(checkpoint,
                            "SynsetRelation transitive closure - written edges",
                            count);
                }
            }

            DbInfo dbInfo = getDbInfo();
            dbInfo.setToAugment(false);
            session.saveOrUpdate(dbInfo);

            tx.commit();

            LOG.info("");
            LOG.info("Done writing transitive closure for SynsetRelations.");
            LOG.info("");
            LOG.info(relStats.toString());
            LOG.info("Total elapsed time:  " + Internals.formatInterval(start, new Date()));
            LOG.info("");

        } catch (Exception ex) {
            LOG.error("Error while computing transitive closure! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivException("Error while computing transitive closure!", ex);
        }

    }

    /**
     * @since 0.1.0
     */
    public long getSynsetRelationsCount() {
        // tried to put DivSynsetRelation.class but was always giving a 0 count
        // ..
        return ((Number) session.createCriteria(SynsetRelation.class)
                                .setProjection(Projections.rowCount())
                                .uniqueResult()).longValue();

    }

    /**
     * See {@link #importFiles(ImportConfig)}
     * 
     * For supported URL formats see
     * {@link it.unitn.disi.diversicon.internal.Internals#readData(String, boolean)
     * Internals.readData}
     * 
     * @since 0.1.0
     */
    public ImportJob importXml(String fileUrl) {

        ImportConfig config = new ImportConfig();

        config.setAuthor(Diversicons.DEFAULT_AUTHOR);
        config.setFileUrls(Arrays.asList(fileUrl));

        return importFiles(config).get(0);
    }

    /**
     * Imports files, and each file import is going to be a separate
     * transaction. In case one
     * fails... TODO! Call is synchronous, after finishing returns logs of each
     * import.
     * 
     * @throws DivValidationException
     * 
     * @since 0.1.0
     */
    public List<ImportJob> importFiles(ImportConfig config) {

        Date start = new Date();

        List<ImportJob> ret = new ArrayList<>();

        checkNotNull(config);

        checkNotBlank(config.getAuthor(), "Invalid ImportConfig author!");
        checkNotEmpty(config.getFileUrls(), "Invalid ImportConfig filepaths!");

        int i = 0;
        for (String fileUrl : config.getFileUrls()) {
            checkNotBlank(fileUrl, "Invalid file url at position " + i + "!");
            i++;
        }

        LOG.info("Going to import " + config.getFileUrls()
                                            .size()
                + " files by import author " + config.getAuthor() + "...");

        DbInfo oldDbInfo = prepareDbForImport();

        try {
            for (String fileUrl : config.getFileUrls()) {
                ImportJob job = importFile(config, fileUrl);
                ret.add(job);
            }
        } catch (InvalidImportException ex) {
            if (ret.isEmpty()) {
                LOG.error("Import failed, no LexicalResource data was written to disk. "
                        + "Aborting all imports.", ex);
                setDbInfo(oldDbInfo);
            }
            throw ex;
        }

        if (config.isSkipAugment()) {
            LOG.info("Skipping graph augmentation as requested by user.");
        } else {
            try {
                processGraph();
            } catch (Exception ex) {
                throw new InterruptedImportException("Error while augmenting graph with computed edges!", ex);
            }
        }

        logImportResult(config, start, ret);
        return ret;

    }

    /**
     * @since 0.1.0
     */
    private void logImportResult(ImportConfig config, Date start, List<ImportJob> jobs) {

        checkNotNull(config);
        checkNotNull(start);
        checkNotNull(jobs);

        String plural = config.getFileUrls()
                              .size() > 1 ? "s" : "";

        Date end = new Date();

        LOG.info("Elapsed time: " + Internals.formatInterval(start, end) + "   Started: " + Internals.formatDate(start)
                + "   Ended: " + Internals.formatDate(end));
        LOG.info("");
        LOG.info("");
        LOG.info("");
        LOG.info("Done importing " + config.getFileUrls()
                                           .size()
                + " LMF" + plural + " by import author " + config.getAuthor());
        LOG.info("");
        LOG.info("Imported lexical resources: ");
        LOG.info("");

        for (ImportJob job : jobs) {
            LOG.info("    " + job.getLexResPackage()
                                 .getName()
                    + "    from    " + job.getFileUrl());
        }

    }

    /**
     * Imports a single LMF XML
     * 
     * @throws InvalidImportException
     * @throws InterruptedImportException
     * 
     * @since 0.1.0
     */
    private ImportJob importFile(ImportConfig config, String fileUrl) {

        LOG.info("Loading LMF : " + fileUrl + " ...");

        File file;
        try {
            file = Internals.readData(fileUrl, true)
                            .toTempFile();
        } catch (Exception ex) {
            throw new InvalidImportException("Couldn't read file to import: " + fileUrl, ex);
        }

        LexResPackage pack;

        try {
            pack = Diversicons.readPackageFromLexRes(fileUrl);
        } catch (Exception ex) {
            throw new InvalidImportException("Couldn't extract attributes from LexicalResource " + fileUrl, ex);
        }

        ImportJob job = newImportJob(
                config,
                fileUrl,
                file, 
                pack);
        
        LOG.info("");
        LOG.info("Starting import...");
        LOG.info("");
        
        try {
            DivXmlToDbTransformer trans = new DivXmlToDbTransformer(this);
            trans.transform(file, null);
            endImportJob(job);
            
        } catch (Exception ex) {
            throw new InterruptedImportException("Error while loading lmf xml " + fileUrl, ex);
        }

        LOG.info("Done loading LMF : " + fileUrl + " .");

        return job;
    }

    /**
     * @since 0.1.0
     */
    private void setDbInfo(DbInfo dbInfo) {
        checkNotNull(dbInfo);

        Transaction tx = null;
        try {

            tx = session.beginTransaction();

            session.saveOrUpdate(dbInfo);

            tx.commit();
        } catch (Exception ex) {
            LOG.error("Error while setting dbInfo!");
            if (tx != null) {
                tx.rollback();
            }

            throw new DivException("Error while setting dbInfo!", ex);
        }

    }

    /**
     * Writes DBInfo flags to DB.
     * 
     * @since 0.1.0
     */
    private DbInfo prepareDbForImport() {

        Transaction tx = null;
        try {

            tx = session.beginTransaction();
            DbInfo oldDbInfo = getDbInfo();

            DbInfo dbInfo = getDbInfo();
            dbInfo.setToValidate(true);
            dbInfo.setToNormalize(true);
            dbInfo.setToAugment(true);
            session.saveOrUpdate(dbInfo);

            tx.commit();
            return oldDbInfo;
        } catch (Exception ex) {
            LOG.error("Error while setting import flags in db, rolling back!");
            if (tx != null) {
                tx.rollback();
            }

            throw new DivException("Error while setting import flags in db", ex);
        }

    }

    /**
     * Ends an import job. To be called after LMF data has been written to the
     * DB.
     * 
     * @throws InterruptedImportException
     *             when db is eft with pending changes.
     * 
     * @since 0.1.0
     */
    private void endImportJob(ImportJob job) {
        checkNotNull(job);

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            job.setEndDate(new Date());
            session.saveOrUpdate(job);

            DbInfo dbInfo = getDbInfo();
            dbInfo.setCurrentImportJob(null);
            session.saveOrUpdate(dbInfo);

            tx.commit();

        } catch (Exception ex) {
            LOG.error("Error while ending import job in db, rolling back!");
            if (tx != null) {
                tx.rollback();
            }

            throw new InterruptedImportException("Error while ending import job in db!", ex);
        }

    }

    /**
     * 
     * Creates an {@link ImportJob} Java object. No data will be written into the db.
     * Still, db can get accessed to validate the input.
     * 
     * !!!! IMPORTANT !!!!! {@code pack} must already contain data about the resource!!
     * 
     * @throws InvalidImportException
     *             if thrown means some validation failed but no
     *             LexicalResource data was written to disk.
     * 
     * @param the phyisical source LMF XML file. If there was none, use {@code null} 
     * @see #setCurrentImportJob(ImportJob)
     * @since 0.1.0
     */
    private ImportJob newImportJob(
            ImportConfig config,
            String fileUrl,
            @Nullable
            File file,
            LexResPackage pack) {

        try {
            checkNotNull(config);
            checkNotEmpty(fileUrl, "Invalid fileUrl!");
            checkArgument(config.getFileUrls()
                                .contains(fileUrl),
                    "Couldn't find fileUrl " + fileUrl + "in importConfig!");

            Internals.checkLexResPackage(pack);
            
            if (file != null){
                LOG.info("");
                LOG.info("Validating XML Schema of " + file.getAbsolutePath() + "   ...");
                LOG.info("");
                try {
                    Diversicons.validateXml(file, LOG, config.getLogLimit());
                    LOG.info("XML is valid!");
                    LOG.info("");
                } catch (Exception ex){
                    throw new InvalidImportException("Found invalid XML !", ex);
                }                
            }
            
            List<ImportJob> jobs = getImportJobs();
            
            for (ImportJob job : jobs){
                
                if (pack.equals(job.getLexResPackage())){
                    throw new InvalidImportException("Looks like you're trying to import the same lexical resource twice!");
                }
                
                if (Objects.equals(pack.getPrefix(), job.getLexResPackage().getPrefix())){
                    throw new InvalidImportException("Tried to import a lexical resource which "
                            + "has the same prefix of resource:\n " + job.getLexResPackage().getName(),
                            config, fileUrl, pack);
                }
            }
            
            
            
            Map<String, String> dbNss = getNamespaces();
                       
            for (String prefix : pack.getNamespaces()
                                     .keySet()) {
                if (dbNss.containsKey(prefix)) {
                    String urlInDb = dbNss.get(prefix);
                    String urlToImport = pack.getNamespaces()
                                             .get(prefix);
                    if (!Objects.equals(urlInDb, urlToImport)) {
                        throw new InvalidImportException(
                                "Tried to import a prefix which is assigned to another resource url in the db! "
                                        + "Prefix is " + prefix + "  ; url to import = " + urlToImport
                                        + "  ;  url in the db = " + urlInDb,
                                        config, fileUrl, pack);
                    }
                }
            }

        } catch (Exception ex) {
            throw new InvalidImportException(
                      "Invalid import for " + fileUrl + "! \n", ex,
                      config, fileUrl, pack);
        }
        
        ImportJob job = new ImportJob();

        job.setAuthor(config.getAuthor());
        job.setDescription(config.getDescription());
        job.setStartDate(new Date());
        job.setFileUrl(fileUrl);
        job.setLexResPackage(pack);

        return job;
        
    }
    
    /**
     * @since 0.1.0
     * @param job
     */
    private void setCurrentImportJob(ImportJob job){
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            
            session.saveOrUpdate(job);

            DbInfo dbInfo = getDbInfo();
            dbInfo.setCurrentImportJob(job);
            session.saveOrUpdate(dbInfo);

            tx.commit();            

        } catch (Exception ex) {
            LOG.error("Error while adding import job in db, rolling back!");
            if (tx != null) {
                tx.rollback();
            }

            throw new DivException("Error while adding import job in db!", ex);
        }
        
    }

    /**
     * Returns all registered namespaces.
     * 
     * @since 0.1.0
     * 
     */
    public Map<String, String> getNamespaces() {

        Map<String, String> ret = new HashMap<>();

        for (LexResPackage pack : getLexResPackages()) {
            ret.putAll(pack.getNamespaces());
        }

        return ret;
    }

    /**
     * @since 0.1.0
     */
    public List<LexResPackage> getLexResPackages() {
        Criteria crit = session.createCriteria(LexResPackage.class);
        @SuppressWarnings("unchecked")
        List<LexResPackage> ret = crit.list();
        return ret;
    }

    /**
     * 
     * Saves a {@link LexicalResource} complete with all the lexicons, synsets,
     * etc into
     * a database. Call is synchronous, after finishing returns a log of the
     * import.
     * <p>
     * NOTE: This method is suitable only for small lexical resources,
     * generally for testing purposes. If you have a big resource, stream the
     * load process by providing your implementation of <a href=
     * "https://github.com/dkpro/dkpro-uby/blob/master/de.tudarmstadt.ukp.uby.persistence.transform-asl/src/main/java/de/tudarmstadt/ukp/lmf/transform/LMFDBTransformer.java"
     * target="_blank"> LMFDBTransformer</a> and then call {@code transform()}
     * on it instead.
     * </p>
     * 
     * @param prefix
     *            The prefix to use within the database
     * @param namespaces
     *            The namespaces used by the resource
     * @param skipAugment
     *            if true, after the import prevents the graph froom being
     *            normalized
     *            and augmented with transitive closure.
     * @throws InvalidImportException
     * @throws InterruptedImportException
     * 
     * 
     * @since 0.1.0
     */
    public ImportJob importResource(
            LexicalResource lexicalResource,
            LexResPackage lexResPackage,
            boolean skipAugment) {

        checkLexResPackage(lexResPackage, lexicalResource);

        LOG.info("Going to save lexical resource to database...");

        ImportJob job = null;
        ImportConfig config; 

        config = new ImportConfig();
        config.setSkipAugment(skipAugment);
        config.setAuthor(Diversicons.DEFAULT_AUTHOR);
        String fileUrl = Diversicons.MEMORY_PROTOCOL + ":" + lexicalResource.hashCode();
        config.addLexicalResource(fileUrl);
        
        try {
            
            job = newImportJob(config,
                    fileUrl,
                    null,
                    lexResPackage);            
        
            prepareDbForImport();
            
            setCurrentImportJob(job);
            
            new JavaToDbTransformer(this, lexicalResource).transform();
            
            if (!skipAugment) {
                processGraph();
            }
            
            endImportJob(job);

        } catch (InvalidImportException ex) {

            LOG.error("Import failed! Aborting all imports! (no LexicalResource data was written to disk) \n", ex);
            
            throw ex;
        } catch (Exception ex) {
            throw new InterruptedImportException("Error when importing lexical resource "
                    + lexicalResource.getName() + " !", ex);
        }
        
        LOG.info("Done saving.");       
        
        return job;
    }

    /**
     * Returns the fully qualified package name.
     * 
     * @since 0.1.0
     */
    public static String getProvenanceId() {
        return Diversicon.class.getPackage()
                               .getName();
    }

    /**
     * Creates an instance of a Diversicon and opens a connection to the db.
     * Db must already exists. Present schema will be validated against required
     * one
     * and if it doesn't match {@link InvalidSchemaException} will be thrown.
     * 
     * @param dbConfig
     *
     * @throws DivIoException
     * @throws InvalidSchemaException
     * @throws DivException
     * 
     * @since 0.1.0
     */
    public static Diversicon connectToDb(DBConfig dbConfig) {
        Diversicon ret = new Diversicon(dbConfig);
        return ret;
    }

    /**
     * Simple export to a UBY-LMF {@code .xml} file.
     * 
     * @param outPath
     *            a path to a file, if compressed it is suggested to end with
     *            {@code .xml.zip}.
     *            If the path includes non-existing directories, they will be
     *            automatically created.
     * @param lexResName
     *            the name of the lexical resource to export.
     * @param compress
     *            if true file is compressed to zip
     * @throws DivIoException
     *             if file in {@code xmlPath} already exists or there are write
     *             errors.
     * @throws DivNotFoundException
     *             if {@code lexicalResourceName} does not exists.
     * 
     * @throws InvalidStateException
     *             if database is not normalized
     * 
     * @since 0.1.0
     */
    public void exportToXml(
            String outPath,
            String lexResName,
            boolean compress) {

        checkNotBlank(outPath, "invalid sql path!");
        checkNotBlank(lexResName, "invalid lexical resource name!");

        File outFile = new File(outPath);

        if (compress) {
            String ext = FilenameUtils.getExtension(outFile.getName());
            checkArgument("zip".equals(ext), "Compression extension not supported: " + ext);
        }

        if (outFile.exists()) {
            throw new DivIoException("Tried to export xml to an already existing file: "
                    + outFile.getAbsolutePath());
        }

        if (getDbInfo().isToNormalize()) {
            LOG.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            LOG.warn("     DATABASE WAS NOT NORMALIZED, RESULTING XML MAY HAVE ISSUES!       ");
            LOG.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        LOG.info("Exporting xml to " + outPath + "  ...");

        LexicalResource dbLe = getLexicalResource(lexResName);

        if (dbLe == null) {
            throw new DivNotFoundException("Couldn't find lexical resource " + lexResName + "  !");
        }

        try {

            if (compress) {
                Path tempOut = Internals.createTempFile(Internals.DIVERSICON_STRING, ".xml");
                new DBToXMLTransformer(dbConfig, tempOut.toString(), null).transform(dbLe);
                ZipArchiveOutputStream zar = new ZipArchiveOutputStream(outFile);
                zar.setLevel(Deflater.BEST_COMPRESSION);

                String xmlEntryName = Internals.makeExtension(outPath, "xml");

                ZipArchiveEntry entry = new ZipArchiveEntry(xmlEntryName);
                entry.setSize(tempOut.toFile()
                                     .length());
                zar.putArchiveEntry(entry);
                IOUtils.copy(new FileInputStream(tempOut.toFile()), zar);
                zar.closeArchiveEntry();

            } else {
                LexResPackage pack = getLexResPackageByName(lexResName);
                new DivDbToXmlTransformer(
                        this,
                        new FileOutputStream(outPath),
                        null,
                        pack).transform(dbLe);
            }

        } catch (IOException | SAXException ex) {
            throw new DivException("Error while making xml file " + outFile.getAbsolutePath(), ex);
        }

        LOG.info("Done exporting xml to " + outPath + "  ...");

    }

    /**
     * Exports to a {@code .sql} file. Currently, only supported db is
     * {@code H2}.
     * 
     * @param sqlPath
     *            a path to a file, which is suggested to end with {@code .sql}
     *            or {@code .sql.zip}, if compressed.
     *            If the path includes non-existing directories, they will be
     *            automatically created.
     * @param compress
     *            if true file is compressed to zip
     * @throws DivIoException
     *             if file in {@code sqlPath} already exists or there are write
     *             errors.
     * 
     * @since 0.1.0
     */
    public void exportToSql(String sqlPath, boolean compress) {

        Diversicons.checkH2Db(dbConfig);
        checkNotBlank(sqlPath, "invalid sql path!");

        File f = new File(sqlPath);

        if (f.exists()) {
            throw new DivIoException("Tried to export SQL to an already existing file: "
                    + f.getAbsolutePath());
        }

        LOG.info("Backing up database to " + sqlPath + "  ...");

        List<String> params = new ArrayList<>();
        params.add("-url");
        params.add(dbConfig.getJdbc_url());
        params.add("-user");
        params.add(dbConfig.getUser());
        params.add("-password");
        params.add(dbConfig.getPassword());
        params.add("-script");
        params.add(sqlPath);

        if (compress) {
            params.add("-options");
            params.add("compression");
            params.add("zip");
        }

        String[] bkp = (String[]) params.toArray(new String[0]);
        try {
            Script.main(bkp);
        } catch (SQLException ex) {
            throw new DivIoException("Error while exporting to sql to " + f.getAbsolutePath() + "  !", ex);
        }
        LOG.info("Done backing up database to " + f.getAbsolutePath());

    }

    /**
     * See {@link #isConnected(String, String, int, List)}
     *
     * @since 0.1.0
     */
    public boolean isConnected(
            String sourceSynsetId,
            String targetSynsetId,
            int depth,
            String... relNames) {
        return isConnected(sourceSynsetId, targetSynsetId, depth, Arrays.asList(relNames));
    }

    /**
     * 
     * Returns true if {@code sourceSynset} is connected to {@code targetSynset}
     * with some relation
     * {@code relNames} within given {@code depth}. This function only looks for
     * edges already
     * present in the database, without calculating new ones (except for known
     * inverses).
     * 
     * @param sourceSynset
     *            the source synset
     * @param targetSynset
     *            the target synset
     * @param depth
     *            the maximum edge depth for relations.
     *            if {@code -1} no depth limit is applied. If {@code zero}
     *            returns true only if source and target coincide.
     * @param relNames
     *            the relation names (in particular see
     *            {@link ERelNameSemantics}). if none is provided returns true
     *            only if source and target
     *            coincide.
     * 
     * @since 0.1.0
     */
    public boolean isConnected(
            String sourceSynsetId,
            String targetSynsetId,
            int depth,
            List<String> relNames) {

        checkId(sourceSynsetId, "Invalid source synset id!");
        checkId(targetSynsetId, "Invalid target synset id!");
        checkNotNull(relNames, "Invalid relation names!");

        checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

        if (sourceSynsetId.equals(targetSynsetId)) {
            return true;
        }

        if (relNames.isEmpty()) {
            return false;
        }

        List<String> directRelations = new ArrayList();
        List<String> inverseRelations = new ArrayList();

        for (String relName : relNames) {
            if (Diversicons.isCanonicalRelation(relName) || !Diversicons.hasInverse(relName)) {
                directRelations.add(relName);
            } else {
                inverseRelations.add(Diversicons.getInverse(relName));
            }
        }

        String depthConstraint;
        if (depth == -1) {
            depthConstraint = "";
        } else {
            depthConstraint = " SR.depth <= " + depth + " AND ";
        }

        String directHsql;
        if (directRelations.isEmpty()) {
            directHsql = "";
        } else {
            directHsql = " "
                    + " ("
                    + "     SR.source.id = :sourceSynsetId"
                    + "     AND   SR.target.id = :targetSynsetId"
                    + "     AND   SR.relName IN " + makeSqlList(directRelations)
                    + " )";
        }

        String inverseHsql;
        if (inverseRelations.isEmpty()) {
            inverseHsql = "";
        } else {
            inverseHsql = ""
                    + "  ("
                    + "      SR.source.id = :targetSynsetId"
                    + "      AND SR.target.id = :sourceSynsetId"
                    + "      AND SR.relName IN " + makeSqlList(inverseRelations)
                    + "  )";

        }

        String orHsql;
        if (!directRelations.isEmpty() && !inverseRelations.isEmpty()) {
            orHsql = " OR ";
        } else {
            orHsql = "";
        }

        String queryString = " SELECT 'TRUE'"
                + " FROM SynsetRelation SR"
                + " WHERE "
                + depthConstraint
                + "("
                + directHsql
                + orHsql
                + inverseHsql
                + ")";

        Query query = session.createQuery(queryString);
        query
             .setParameter("sourceSynsetId", sourceSynsetId)
             .setParameter("targetSynsetId", targetSynsetId);

        return query.iterate()
                    .hasNext();
    }

    /**
     * @since 0.1.0
     */
    private static String formatDate(@Nullable Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");

        if (date == null) {
            return "missing";
        } else {
            return sdf.format(date);
        }
    }

    /**
     * Returns a nicely formatted import logc
     * 
     * @param fullLog
     *            includes full log in the output
     * @throws DivNotFoundException
     *
     * @since 0.1.0
     */
    public String formatImportJob(long importJobId, boolean fullLog) {
        ImportJob job = getImportJob(importJobId);
        return formatImportJob(job, fullLog);
    }

    /**
     * Returns a nicely formatted import logc
     * 
     * @param fullLog
     *            includes full log in the output
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
     */
    public String formatImportJob(ImportJob job, boolean fullLog) {
        StringBuilder sb = new StringBuilder();

        sb.append("IMPORT ID: ");
        sb.append(job.getId());
        sb.append("   LEXICAL RESOURCE: ");
        sb.append(job.getLexResPackage()
                     .getName());
        sb.append("   IMPORT AUTHOR: ");
        sb.append(job.getAuthor());
        sb.append("\n");
        sb.append("NAMESPACE: ");
        sb.append(job.getLexResPackage()
                     .getPrefix()
                + ":" + job.getLexResPackage()
                           .getNamespaces()
                           .get(job.getLexResPackage()
                                   .getPrefix()));
        sb.append("   FROM FILE: ");
        sb.append(job.getFileUrl());

        if (job.getLogMessages()
               .size() > 0) {
            sb.append("\n");
            sb.append("   THERE WHERE " + job.getLogMessages()
                                             .size()
                    + " WARNINGS/ERRORS");
        }
        sb.append("\n");
        sb.append("STARTED: ");
        sb.append(formatDate(job.getStartDate()));
        sb.append("   ENDED: ");
        sb.append(formatDate(job.getEndDate()));
        sb.append("\n");
        sb.append(job.getDescription());
        sb.append("\n");
        if (fullLog) {
            sb.append("\n");
            sb.append("Full log:");
            List<LogMessage> msgs = job.getLogMessages();
            if (msgs.isEmpty()) {
                sb.append(" No logs to report.\n");
            } else {
                sb.append("\n");
                for (LogMessage msg : job.getLogMessages()) {
                    sb.append(msg.getMessage() + "\n");
                }
            }

        }
        sb.append("\n");
        return sb.toString();

    }

    /**
     * Returns a nicely formatted import log
     * 
     * @param showFullLogs
     *            includes full logs in the output
     * 
     * @see #formatImportJob(ImportJob, boolean)
     * 
     * @since 0.1.0
     */
    public String formatImportJobs(boolean showFullLogs) {
        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("unchecked")
        List<ImportJob> importJobs = session.createCriteria(ImportJob.class)
                                            .addOrder(Order.desc("startDate"))
                                            .setMaxResults(50)
                                            .list();

        if (importJobs.isEmpty()) {
            sb.append("There are no imports to show.\n");
        }

        for (ImportJob job : importJobs) {
            sb.append(formatImportJob(job, showFullLogs));
        }
        return sb.toString();
    }

    /**
     * A list of the imports performed so far.
     * 
     * @since 0.1.0
     *
     */
    public List<ImportJob> getImportJobs() {
        Criteria crit = session.createCriteria(ImportJob.class);
        @SuppressWarnings("unchecked")
        List<ImportJob> ret = crit.list();
        return ret;
    }

    /**
     * Returns a nicely formatted status of the database
     * 
     * @param shortProcessedInfo
     *            if true no distinction is made
     *            between graph normalization and augmentation
     * 
     * @since 0.1.0
     */
    public String formatDbStatus(boolean shortProcessedInfo) {
        StringBuilder sb = new StringBuilder();
        DbInfo dbInfo = getDbInfo();
        String dataVersion = dbInfo.getVersion()
                                   .isEmpty() ? "-" : dbInfo.getVersion();

        sb.append(" Schema version: " + dbInfo.getSchemaVersion());
        sb.append("   Data version: " + dataVersion + "\n");
        sb.append("\n");

        if (shortProcessedInfo) {
            if (dbInfo.isToAugment() || dbInfo.isToNormalize()) {
                sb.append(" * Synset relation graph needs to be processed. \n");
            }

        } else {
            if (dbInfo.isToNormalize()) {
                sb.append(" * Synset relation graph needs to be normalized.\n");
            }

            if (dbInfo.isToAugment()) {
                sb.append(" * Synset relation graph needs to be augmented.\n");
            }

        }

        ImportJob importJob = dbInfo.getCurrentImportJob();

        if (importJob != null) {
            sb.append("- There is an import job in progress for lexical resource "
                    + importJob.getLexResPackage()
                               .getLabel()
                    + " from file " + importJob.getFileUrl() + "\n");
        }
        return sb.toString();

    }

    /**
     * 
     * Returns an import job.
     * 
     * @param importId
     *            must be >= 0;
     * 
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
     */
    public ImportJob getImportJob(long importId) {

        ImportJob ret = (ImportJob) session.get(ImportJob.class, importId);
        if (ret == null) {
            throw new DivNotFoundException("Couldn't find import job " + importId);
        } else {
            return ret;
        }

    }

    /**
     * 
     * Returns the free memory in KB (where 1024 bytes is a KB). This method
     * returns an int.
     * 
     * @since 0.1.0
     */
    public int memoryUsed() {
        Diversicons.checkH2Db(dbConfig);
        return (int) getSession().createSQLQuery("SELECT MEMORY_FREE()")
                                 .uniqueResult();
    }

    /**
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
     */
    public LexResPackage getLexResPackageByName(String lexResName) {
        checkNotBlank(lexResName, "Invalid lexical resource name!");

        Criteria criteria = session.createCriteria(LexResPackage.class)
                                   .add(Restrictions.like("name", lexResName));

        @SuppressWarnings("unchecked")
        LexResPackage ret = (LexResPackage) criteria.uniqueResult();

        if (ret == null) {
            throw new DivNotFoundException("Couldn't find lexical resource package with name " + lexResName);
        }

        return ret;
    }

    /**
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
     */
    public LexResPackage getLexResPackageById(String lexResId) {
        checkNotBlank(lexResId, "Invalid lexical resource id!");

        Criteria criteria = session.createCriteria(LexResPackage.class)
                                   .add(Restrictions.like("id", lexResId));

        @SuppressWarnings("unchecked")
        LexResPackage ret = (LexResPackage) criteria.uniqueResult();

        if (ret == null) {
            throw new DivNotFoundException("Couldn't find lexical resource package with id " + lexResId);
        }

        return ret;
    }
    

    /*
     * Returns a list of {@link Namespace namespaces} used within database.
     * 
     * @since 0.1.0
     * 
     * public Map<String, String> getNamespaces() {
     * 
     * Criteria criteria = session.createCriteria(Namespace.class);
     * 
     * @SuppressWarnings("unchecked")
     * List<Namespace> res = criteria.list();
     * HashMap<String, String> ret = new HashMap();
     * 
     * if (res == null) {
     * ret = new HashMap<>();
     * } else {
     * for (Namespace ns : res) {
     * ret.put(ns.getPrefix(), ns.getUrl());
     * }
     * }
     * 
     * return ret;
     * }
     */
}
