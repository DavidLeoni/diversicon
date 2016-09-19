package it.unitn.disi.diversicon.test.experiments;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.xerces.impl.Constants;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;
import it.unitn.disi.diversicon.test.DivTester;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 * Various experiments and discarded code .
 *
 *
 * @since 0.1.0
 */
public class HibernateExperimentsTest {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateExperimentsTest.class);

    /**
     * Adds missing edges for relations we consider as canonical
     * 
     * Keeping this here just to remember what a horror it was
     * Adds missing edges for relations we consider as canonical
     * 
     * @since 0.1.0
     */
    private void normalizeGraphWithSqlCrap() {

        LOG.info("Going to normalizing graph with canonical relations ...");

        DBConfig dbConfig = DivTester.createNewDbConfig();

        Diversicon uby = Diversicon.connectToDb(dbConfig);

        Session session = uby.getSession();

        Transaction tx = session.beginTransaction();

        for (String relName : Diversicons.getCanonicalRelations()) {

            LOG.info("Normalizing graph with canonical relation " + relName + " ...");

            String inverseRelName = Diversicons.getInverse(relName);
            LOG.info("inverse relation name = " + inverseRelName);

            /*
             * String hqlInsert =
             * "INSERT INTO SynsetRelation (source, target, relType, relName, depth, provenance) "
             * +
             * "  SELECT SR.target, SR.source,  SR.relType, :relName,  1, :provenance"
             * + "  FROM SynsetRelation SR";
             */
            /*
             * String hqlInsert =
             * "INSERT INTO SynsetRelation (source, target,  relType, relName, idx, depth, provenance) "
             * + "  SELECT SR.target, SR.source,  SR.relType, '" + relName +
             * "', ROWNUM + 100, 1, '" + getProvenance() + "'"
             * + "  FROM SynsetRelation SR";
             */

            /*
             * query.setParameter("relName", "'" + relName + "'")
             * .setParameter("inverseRelName", "'" +inverseRelName + "'")
             * .setParameter("provenance", "'" + getProvenance() + "'");
             */

            // log.info("Inserted " + createdEntities + " " + relName + "
            // edges.");

            String hqlInsert = "INSERT INTO SynsetRelation (synsetId, target,  relType, relName,  depth, provenance) "
                    + "  SELECT SR.target, SR.synsetId,  SR.relType, "
                    + "         '" + relName + "', 1, '" + Diversicon.getProvenanceId() + "'"
                    + "  FROM SynsetRelation SR"
                    + "  WHERE"
                    + "        SR.relName='" + inverseRelName + "'";
            /*
             * + "      AND SR.depth=1"
             * + "    AND SR.provenance=''";
             */
            /*
             * + "      AND (SR.target, SR.synsetId) NOT IN " // so
             * + "         ("
             * + "                SELECT (SR2.synsetId, SR2.target)"
             * + "                FROM SynsetRelation SR2"
             * + "                WHERE      "
             * + "                      SR2.relName='"+relName + "'"
             * + "                  AND SR2.depth=1"
             * + "         )";
             */
            // Query query = session.createQuery(hqlInsert);
            Query query = session.createSQLQuery(hqlInsert);

            /*
             * query.setParameter("relName", "'" + relName + "'")
             * .setParameter("inverseRelName", "'" +inverseRelName + "'")
             * .setParameter("provenance", "'" + getProvenance() + "'");
             */
            int createdEntities = query.executeUpdate();
            LOG.info("Inserted " + createdEntities + " " + relName + " edges.");

        }

        tx.commit();
        session.close();

        LOG.info("Done normalizing graph with canonical relations.");

    }

    private static class Expericon extends Diversicon {

        Expericon(DBConfig dbConfig) {
            super(dbConfig);
        }

        /**
         * EXPERIMENTAL VERSION USING UNION. HQL CAN'T HANDLE IT, OF COURSE.
         * 
         * Finds all of the synsets reachable from {@code synsetId} along paths
         * of
         * {@code relNames}
         * within given depth. In order to actually find them,
         * relations in {@code relNames} must be among the ones for which
         * transitive
         * closure is computed.
         * (see {@link Diversicons#getKnownRelations()}).
         * 
         * @param synsetId
         * @param relNames
         *            if none is provided all reachable parent synsets are
         *            returned.
         * @param depth
         *            if -1 all parents until the root are retrieved. If zero
         *            nothing is returned.
         * @param lexicon
         * 
         * @since 0.1.0
         */
        public Iterator<Synset> getConnectedSynsets(
                String synsetId,
                int depth,
                String... relNames) {

            checkNotEmpty(synsetId, "Invalid synset id!");
            checkArgument(depth >= -1, "Depth must be >= -1 , found instead: " + depth);

            List<String> directRelations = new ArrayList();
            List<String> inverseRelations = new ArrayList();

            for (String relName : relNames) {
                if (Diversicons.isCanonicalRelation(relName) || !Diversicons.hasInverse(relName)) {
                    directRelations.add(relName);
                } else {
                    inverseRelations.add(Diversicons.getInverse(relName));
                }
            }

            if (depth == 0) {
                return new ArrayList<Synset>().iterator();
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

            String directRelnameConstraint;
            if (directRelations.isEmpty()) {
                directRelnameConstraint = "";
            } else {
                directRelnameConstraint = " AND SRD.relName IN " + makeSqlList(directRelations);
            }

            String inverseRelnameConstraint;
            if (inverseRelations.isEmpty()) {
                inverseRelnameConstraint = "";
            } else {
                inverseRelnameConstraint = " AND SRR.relName IN " + makeSqlList(inverseRelations);
            }

            String queryString = "  SELECT DISTINCT s"
                    + "             FROM"
                    + "             ("
                    + "                 SELECT SRD.target AS s"
                    + "                 FROM   SynsetRelation SRD"
                    + "                 WHERE  SRD.source.id = :synsetId"
                    + directRelnameConstraint
                    + directDepthConstraint
                    + "             )"
                    + "             UNION"
                    + "             ("
                    + "                 SELECT SRR.source AS s"
                    + "                 FROM   SynsetRelation SRR"
                    + "                 WHERE  SRR.target.id = :synsetId"
                    + inverseRelnameConstraint
                    + inverseDepthConstraint
                    + "              )";

            Query query = session.createQuery(queryString);
            query
                 .setParameter("synsetId", synsetId);

            return query.iterate();
        }

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testLogger() {
        Logger logger = LoggerFactory.getLogger(Diversicon.class);

        logger.info("zumzum");
    }

  
    
    
    

}
