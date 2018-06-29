package eu.kidf.diversicon.core.test.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.test.DivTester;

import static eu.kidf.diversicon.core.internal.Internals.checkArgument;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;
import java.util.HashSet;
import java.util.Set;

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
    @SuppressWarnings("unused")
    private void normalizeGraphWithSqlCrap() {

        LOG.info("Going to normalizing graph with canonical relations ...");

        DivConfig divConfig = DivTester.createNewDivConfig();

        Diversicon uby = Diversicon.connectToDb(divConfig);

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
                    + "         '" + relName + "', 1, '" + Diversicons.getProvenanceId() + "'"
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

    @SuppressWarnings("unused")
    private static class Expericon extends Diversicon {

        Expericon(DivConfig divConfig) {
            super(divConfig);
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
