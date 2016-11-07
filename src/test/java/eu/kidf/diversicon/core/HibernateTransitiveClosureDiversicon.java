package eu.kidf.diversicon.core;

import java.util.Date;

import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import eu.kidf.diversicon.core.DbInfo;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.InsertionStats;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.core.test.DivTester;


/**
 * Old slow version for transitive closure, see https://github.com/diversicon-kb/diversicon/issues/14
 * 
 * The  good side is it is portable between dbs.
 * 
 * @since 0.1.0 
 * @deprecated
 */
public class HibernateTransitiveClosureDiversicon extends Diversicon {

    public HibernateTransitiveClosureDiversicon() {
        super(DivTester.createNewDivConfig());
    }

    private static final Logger LOG = LoggerFactory.getLogger(HibernateTransitiveClosureDiversicon.class);

    /**
     * Computes the transitive closure of
     * {@link Diversicons#getCanonicalTransitiveRelations() canonical relations}
     * using Hibernate stuff
     * 
     * Before calling this, the graph has to be normalized by calling
     * {@link #normalizeGraph()}
     * 
     * @throws DivException
     *             when transaction goes wrong it is automatically rolled back
     *             and DivException is thrown
     * 
     * @deprecated This method was very slow with H2, see
     *             https://github.com/diversicon-kb/diversicon/issues/14 . Use
     *             {@link #computeTransitiveClosure()} instead
     * @since 0.1.0
     */
    private void computeTransitiveClosureWithoutRecursiveQueries() {

        Date start = new Date();

        LOG.info("Computing transitive closure for SynsetRelations ...");

        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            InsertionStats relStats = new InsertionStats();

            relStats.setEdgesPriorInsertion(getSynsetRelationsCount());

            LOG.info("\nFound " + Internals.formatInteger(relStats.getEdgesPriorInsertion()) + " synset relations.\n");

            int depthToSearch = 1;
            int count = 0;

            // As: the edges computed so far
            // Bs: original edges

            String hqlSelect = "    SELECT SR_A.source, SR_B.target,  SR_A.relName"
                    + "      FROM SynsetRelation SR_A, SynsetRelation SR_B"
                    + "      WHERE"
                    + "          SR_A.relName IN " + makeSqlList(Diversicons.getCanonicalTransitiveRelations())
                    + "      AND SR_A.depth = :depth"
                    + "      AND SR_B.depth = 1"
                    + "      AND SR_A.relName = SR_B.relName"
                    + "      AND SR_A.target = SR_B.source"
                    // don't want to add edges twice
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
                Date checkpoint = new Date();

                // Augmenting SynsetRelation graph with edges of depth
                // (depthToSearch + 1)

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
                    ssr.setRelType(Diversicons.getRelationType(relName));
                    ssr.setSource(source);
                    ssr.setTarget(target);

                    source.getSynsetRelations()
                          .add(ssr);
                    session.save(ssr);
                    session.merge(source);

                    relStats.inc(relName);
                    processedRelationsInCurLevel += 1;
                    relStats.setMaxLevel(depthToSearch);

                    if (++count % 10000 == 0) {
                        // flush a batch of updates and release memory:
                        session.flush();
                        session.clear();
                        checkpoint = reportLog(checkpoint,
                                "SynsetRelation transitive closure depth level " + depthToSearch + " - written edges",
                                processedRelationsInCurLevel);
                    }
                }

                depthToSearch += 1;

            } while (processedRelationsInCurLevel > 0);

            DbInfo dbInfo = getDbInfo();
            dbInfo.setToAugment(false);
            session.saveOrUpdate(dbInfo);

            tx.commit();

            LOG.info("");
            LOG.info("Done writing transitive closure for SynsetRelations.");
            LOG.info(relStats.toString());
            LOG.info("   Elapsed time:  " + Internals.formatInterval(start, new Date()));
            LOG.info("");

        } catch (Exception ex) {
            LOG.error("Error while computing transitive closure! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivException("Error while computing transitive closure!", ex);
        }
    }

}
