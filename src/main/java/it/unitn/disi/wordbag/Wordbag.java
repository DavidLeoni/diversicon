package it.unitn.disi.wordbag;

import static it.unitn.disi.wordbag.internal.Internals.checkNotEmpty;
import static it.unitn.disi.wordbag.internal.Internals.checkNotNull;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.unitn.disi.wordbag.internal.Internals;

/**
 * Extension of {@link de.tudarmstadt.ukp.lmf.api.Uby Uby} LMF knowledge base with some 
 * additional fields in the db to speed up computations
 * 
 * To create instances use {@link #create(DBConfig)} method
 *
 * @since 0.1
 */
public class Wordbag extends Uby {

    private static final Logger LOG = LoggerFactory.getLogger(Wordbag.class);
        
    /**
     * Amount of items to flush when writing into db with Hibernate.
     */
    private static final int BATCH_FLUSH_COUNT = 20;
    
    
    private Wordbag(DBConfig dbConfig) {
        super(); // so it doesn't open connections! Let's hope they don't delete it!
        checkNotNull(dbConfig, "database configuration is null");

        this.dbConfig = dbConfig;

        // note: here we are overwriting cfg and sessionFactory
        if (Wordbags.exists(dbConfig)){
            LOG.info("Reusing existing database at " + dbConfig.getJdbc_url());
            cfg = Wordbags.getHibernateConfig(dbConfig, true);            
        } else {
            LOG.info("Database doesn't exist, going to create it");
            Wordbags.dropCreateTables(dbConfig);            
            cfg = Wordbags.getHibernateConfig(dbConfig, false);             
        }
        
              
        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        session = sessionFactory.openSession();
    }

    /**
     * Augments the synsetRelation graph with transitive closure of {@link Wordbags#getCanonicalRelations() canonical relations}
     *  and eventally adds needed symmetric relations.
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
        return "'"+s+"'";
    }
    
    private static String makeSqlList(Iterable<String> iterable) {
        StringBuilder retb = new StringBuilder("(");

        boolean first = true;
        for (String s : iterable) {
            if (first) {
                retb.append( quote(s));
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
                WbSynsetRelation ssr = (WbSynsetRelation) sr;

                if (Wordbags.hasInverse(ssr.getRelName())) {
                    String inverseRelName = Wordbags.getInverse(ssr.getRelName());
                    if (Wordbags.isCanonical(inverseRelName)
                            && !containsRel(ssr.getTarget(),
                                    ssr.getSource(),
                                    inverseRelName)) {
                        WbSynsetRelation newSsr = new WbSynsetRelation();

                        newSsr.setDepth(1);
                        newSsr.setProvenance(Wordbag.getProvenanceId());
                        newSsr.setRelName(inverseRelName);
                        newSsr.setRelType(Wordbags.getCanonicalRelationType(inverseRelName));
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
     * Before calling this, the graph has to be normalized by calling {@link #normalizeGraph()}
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
                + "          SR_A.relName IN " + makeSqlList(Wordbags.getCanonicalRelations())
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

            // log.info("Augmenting SynsetRelation graph with edges of depth " + (depthToSearch + 1) + "  ...");

            Query query = session.createQuery(hqlSelect);
            query.setParameter("depth", depthToSearch);            

            
            ScrollableResults results = query
                                             .setCacheMode(CacheMode.IGNORE)
                                             .scroll(ScrollMode.FORWARD_ONLY);
            while (results.next()) {

                Synset source = (Synset) results.get(0);
                Synset target = (Synset) results.get(1);
                String relName = (String) results.get(2);
                
                WbSynsetRelation ssr = new WbSynsetRelation();
                ssr.setDepth(depthToSearch + 1);
                ssr.setProvenance(Wordbag.getProvenanceId());
                ssr.setRelName(relName);
                ssr.setRelType(Wordbags.getCanonicalRelationType(relName));
                ssr.setSource(source);
                ssr.setTarget(target);

                source.getSynsetRelations().add(ssr);
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
     * See {@link #loadLexicalResources(Collection, Collection)}
     */
    public void loadLexicalResources(String filepath,
                                     String lexicalResourceName) {
        
        loadLexicalResources(Arrays.asList(filepath), Arrays.asList(lexicalResourceName));
    }
    
    /**
     * Loads provided resources and automatically augments graph with transitive closure at the 
     * end of the loading.
     *  
     * @param filepaths paths to lmf xml files
     * @param lexicalResourceName
     *            todo meaning? name seems not be required to be in the xml
     */
    // todo think about .sql files
    public void loadLexicalResources(Collection<String> filepaths,
                                     Collection<String> lexicalResourceNames) {

        checkNotEmpty(filepaths, "invalid filepaths length!");
        checkNotEmpty(lexicalResourceNames, "invalid lexicalResourceNames length!");
        
        Internals.checkArgument(filepaths.size() == lexicalResourceNames.size(),
                "Lexical resource names don't match with files! Found "
                + filepaths.size() + " filepaths and " + lexicalResourceNames.size() + " resource names");
        
        Iterator<String> namesIter = lexicalResourceNames.iterator();
        
        for (String filepath : filepaths){
            String lexicalResourceName = namesIter.next();
            
            LOG.info("Loading LMF : " + filepath + " with lexical resource name " + lexicalResourceName + " ...");
                        
            XMLToDBTransformer trans = new XMLToDBTransformer(dbConfig);

            try {
                trans.transform(new File(filepath), lexicalResourceName);
            } catch (Exception ex) {
                throw new RuntimeException("Error while loading lmf xml " + filepath, ex);
            }
            
            LOG.info("Done loading LMF : " + filepath + " with lexical resource name " + lexicalResourceName + " .");
        }

        try {
            augmentGraph();
        } catch (Exception ex) {
            throw new WbException("Error while augmenting graph with computed edges!", ex);
        }

        LOG.info("Done loading LMFs.");
    }

    /**
     * Returns the fully qualified package name.
     */
    public static String getProvenanceId() {
        return Wordbag.class.getPackage()
                           .getName();
    }
    
    /**
     * Creates an instance of a Wordbag.
     * 
     * @param dbConfig
     */
    public static Wordbag create(DBConfig dbConfig){
        Wordbag ret = new Wordbag(dbConfig);
        return ret;
    }
}
