package it.unitn.disi.diversicon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.Nullable;

import de.tudarmstadt.ukp.lmf.hibernate.HibernateConnect;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;

import static de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics.*;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.internal.Internals;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;

/**
 * Utility class for S-match Uby
 * 
 * @since 0.1
 */
public final class Diversicons {

    private static final Logger LOG = LoggerFactory.getLogger(Diversicons.class);

    /**
     * List of known relations, excluding the inverses.
     */
    private static final Set<String> canonicalRelations = new LinkedHashSet();

    /**
     * List of known relations, (including the inverses)
     */    

    private static final Map<String, ERelTypeSemantics> relationTypes = new LinkedHashMap<String, ERelTypeSemantics>();
    
    private static final LinkedHashSet<String> transitiveRelations = new LinkedHashSet<String>();
    private static final LinkedHashSet<String> canonicalTransitiveRelations = new LinkedHashSet<String>();

    private static final LinkedHashSet<String> partOfRelations = new LinkedHashSet<String>();
    private static final LinkedHashSet<String> canonicalPartOfRelations = new LinkedHashSet<String>();
    
    private static Map<String, String> inverseRelations = new HashMap();

    /**
     * Mappings from Uby classes to out own custom ones.
     * 
     */
    private static LinkedHashMap<String, String> customClassMappings;

    static {
        putRelations(HYPERNYM, HYPONYM, ERelTypeSemantics.taxonomic, true, false);
        putRelations(HYPERNYMINSTANCE, HYPONYMINSTANCE, ERelTypeSemantics.taxonomic, true, false);
        putRelations(HOLONYM, MERONYM, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMCOMPONENT, MERONYMCOMPONENT, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMMEMBER, MERONYMMEMBER, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMPART, MERONYMPART, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMPORTION, MERONYMPORTION, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMSUBSTANCE, MERONYMSUBSTANCE, ERelTypeSemantics.partWhole, true, true);
        putRelations(SYNONYM, SYNONYM, ERelTypeSemantics.association, false, false);
        putRelations(SYNONYMNEAR, SYNONYMNEAR, ERelTypeSemantics.association, false, false);
        putRelations(ANTONYM, ANTONYM, ERelTypeSemantics.complementary, false, false);        

        customClassMappings = new LinkedHashMap();
        customClassMappings.put(de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation.class.getCanonicalName(),
                DivSynsetRelation.class.getCanonicalName());

    }

    private Diversicons() {
    }

    /**
     * Sets {@code relNameA} as {@code relNameB}'s symmetric relation, and vice versa.
     *
     *             
     */
    private static void putRelations(
            String relNameA, 
            String relNameB, 
            ERelTypeSemantics relType, 
            boolean transitive,
            boolean partof) {
        checkNotEmpty(relNameA, "Invalid first relation!");
        checkNotEmpty(relNameB, "Invalid second relation!");


        canonicalRelations.add(relNameA);        

        relationTypes.put(relNameA, relType);
        relationTypes.put(relNameB, relType);
        
        inverseRelations.put(relNameA, relNameB);
        inverseRelations.put(relNameB, relNameA);
        
        
        if (transitive){
            transitiveRelations.add(relNameA);
            canonicalTransitiveRelations.add(relNameA);
            transitiveRelations.add(relNameB);            
        }
        
        if (partof){
            partOfRelations.add(relNameA);
            canonicalPartOfRelations.add(relNameA);
            partOfRelations.add(relNameB);
        }
        
        
    }

  
    
    
    /**
     * @throws DivNotFoundException
     *             if {code relation} does not have an inverse
     * @since 0.1
     */
    public static String getInverse(String relation) {
        checkNotEmpty(relation, "Invalid relation!");

        String ret = inverseRelations.get(relation);
        if (ret == null) {
            throw new DivNotFoundException("Couldn't find the relation " + relation);
        }
        return ret;
    }

    /**
     * Returns true if provided relation has a known inverse, otherwise returns
     * false.
     * 
     * @since 0.1
     */
    public static boolean hasInverse(String relation) {
        checkNotEmpty(relation, "Invalid relation!");

        String ret = inverseRelations.get(relation);
        if (ret == null) {
            return false;
        }
        return true;
    }

    /**
     * Note: if false is returned it means we <i> don't know </i> whether or not 
     * the relations are actually inverses.
     * 
     * @since 0.1
     */
    public static boolean isInverse(String a, String b) {
        checkNotEmpty(a, "Invalid first relation!");
        checkNotEmpty(b, "Invalid second relation!");

        String inverse = inverseRelations.get(a);
        if (inverse == null) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * First drops all existing tables and then creates a
     * database based on the hibernate mappings.
     * 
     * (adapted from LMFDBUtils.createTables(dbConfig) )
     * 
     * @since 0.1
     */
    public static void dropCreateTables(DBConfig dbConfig) {

        LOG.info("Creating database " + dbConfig.getJdbc_url() + " ...");

        Configuration hcfg = getHibernateConfig(dbConfig, false);

        SchemaExport se = new SchemaExport(hcfg);
        se.create(true, true);

        DbInfo dbInfo = new DbInfo();
        Session session = openSession(dbConfig, false);
        session.save(dbInfo);
        session.flush();
        session.close();

        LOG.info("Done creating database " + dbConfig.getJdbc_url() + "  .");
    }

    static Session openSession(DBConfig dbConfig, boolean validate) {
        Configuration cfg = Diversicons.getHibernateConfig(dbConfig, validate);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        return sessionFactory.openSession();

    }

    /**
     * Loads a given {@code xml} hibernate configuration into {@code hcfg}
     *
     * @since 0.1
     */
    static void loadHibernateXml(Configuration hcfg, Resource xml) {

        LOG.debug("Loading config " + xml.getDescription() + " ...");

        try {

            java.util.Scanner sc = new java.util.Scanner(xml.getInputStream()).useDelimiter("\\A");
            String s = sc.hasNext() ? sc.next() : "";
            sc.close();

            for (Map.Entry<String, String> e : customClassMappings.entrySet()) {
                s = s.replace(e.getKey(), e.getValue());
            }
            hcfg.addXML(s);

        } catch (Exception e) {
            throw new DivException("Error while reading file at path: " + xml.getDescription(), e);
        }

    }

    /**
     * 
     * Returns the hibernate configuration for accessing db specified by
     * {@code dbConfig}
     * 
     * NOTE: returned configuration will not do any change to an already
     * present database, nor it will create a new one if none is present.
     * 
     * @param validate
     *            if true database schema is validated upon first connection.
     * 
     * @since 0.1
     */
    public static Configuration getHibernateConfig(DBConfig dbConfig, boolean validate) {

        Configuration ret = new Configuration()
                                               .addProperties(HibernateConnect.getProperties(dbConfig.getJdbc_url(),
                                                       dbConfig.getJdbc_driver_class(),
                                                       dbConfig.getDb_vendor(), dbConfig.getUser(),
                                                       dbConfig.getPassword(), dbConfig.isShowSQL()));

        if (validate) {
            ret.setProperty("hibernate.hbm2ddl.auto", "validate");
        } else {
            ret.setProperty("hibernate.hbm2ddl.auto", "none");
        }

        LOG.info("Going to load UBY hibernate mappings...");

        ClassLoader cl = HibernateConnect.class.getClassLoader();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
        Resource[] mappings = null;
        try {
            mappings = resolver.getResources("hibernatemap/access/**/*.hbm.xml");
            for (Resource mapping : mappings) {
                boolean isCustomized = false;
                for (String c : customClassMappings.keySet()) {
                    String[] cs = c.split("\\.");
                    String cn = cs[cs.length - 1];
                    if (mapping.getFilename()
                               .replace(".hbm.xml", "")
                               .contains(cn)) {
                        isCustomized = true;
                    }
                }
                if (isCustomized) {
                    LOG.info("Skipping class customized by Smatch Uby: " + mapping.getDescription());
                } else {
                    loadHibernateXml(ret, mapping);

                }
            }

        } catch (IOException e) {
            throw new DivException("Error while loading hibernate mappings!", e);
        }
        LOG.info("Done loading UBY hibernate mappings...");

        LOG.info("Loading custom Diversicon hibernate mappings... ");

        try {

            Resource[] resources = new PathMatchingResourcePatternResolver(
                    Diversicons.class.getClassLoader())
                                                       .getResources(
                                                               "hybernatemap/access/**/*.hbm.xml");
            
            if (resources.length == 0){
                // div dirty
                String mydir = "file:///home/da/Da/prj/diversicon/prj/src/main/resources/hybernatemap/access/**/*.hbm.xml";
                LOG.info( "Can't find resources, looking in " + mydir 
                        + "(just when testing projects depending upon this in Eclipse!)");
                resources = new PathMatchingResourcePatternResolver(Diversicons.class.getClassLoader())
                .getResources(
                    mydir);                
            }
            
            checkNotEmpty(resources, "Cannot find custom hibernate mappings for Diversicon!");

            for (Resource r : resources) {
                ret.addURL(r.getURL());
                LOG.info("  Loaded " + r.getURL());
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error while loading hibernate mappings!", ex);
        }

        LOG.info("Done loading Diversicon custom mappings. ");

        return ret;
    }


    
    /**
     * Returns true if provided relation is canonical, that is, is privileged wrt 
     * the inverse it might have (example: since hypernymy is considered as canonical, transitive 
     * closure graph is computed only for hypernym, not hyponym)
     * 
     * @since 0.1
     */
    public static boolean isCanonicalRelation(String relName) {
        Internals.checkNotEmpty(relName, "Invalid relation name!");
        return canonicalRelations.contains(relName);
    }

    /**
     * Returns the type of the provided relation.
     * 
     * @throws DivNotFoundException
     * @since 0.1
     */
    public static ERelTypeSemantics getRelationType(String relName) {

        ERelTypeSemantics ret = relationTypes.get(relName);

        if (ret == null) {
            throw new DivNotFoundException("There is no reltaion type associated to relation " + relName);
        }
        return ret;
    }

    /**
     * 
     * @since 0.1
     */
    public static String synsetRelationToString(@Nullable SynsetRelation sr) {

        if (sr == null) {
            return "null";
        } else {
            if (sr instanceof DivSynsetRelation) {
                return sr.toString();
            } else {
                String sourceId = sr.getSource() == null ? "null" : sr.getSource()
                                                                      .getId();
                String targetId = sr.getTarget() == null ? "null" : sr.getTarget()
                                                                      .getId();
                return "SynsetRelation [source=" + sourceId + ", target="
                        + targetId + ", relType=" + sr.getRelType() + ", relName="
                        + sr.getRelName() + ", frequencies=" + sr.getFrequencies() + "]";
            }

        }

    }

    /**
     * Returns a list of relations used by Diversicon, in
     * {@link de.tudarmstadt.ukp.uby.lmf.model.ERelNameSemantics Uby format}
     * The list will contain only the {@link #isCanonicalRelation(String) canonical} relations 
     *  and not their inverses.
     * 
     * @since 0.1
     */
    public static List<String> getCanonicalRelations() {
        return new ArrayList(canonicalRelations);
    }

    /**
     * Returns a list of all relations used by Diversicon, in
     * {@link de.tudarmstadt.ukp.uby.lmf.model.ERelNameSemantics Uby format}
     * (including the inverses)
     * 
     * @since 0.1
     */
    public static List<String> getRelations() {
        return new ArrayList(relationTypes.keySet());
    }    
    
    
    /**
     * Returns true if provided database configuration points to an
     * existing database with all needed tables.
     * @since 0.1 
     */
    public static boolean exists(DBConfig dbConfig) {

        Configuration cfg = Diversicons.getHibernateConfig(dbConfig, false);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        Session session = sessionFactory.openSession();
        // dirty but might work
        try {
            session.get(DbInfo.class, 0L);
            return true;
        } catch (org.hibernate.exception.SQLGrammarException ex) {
            return false;
        } finally {
            try {
                session.close();
            } catch (Exception ex) {
                LOG.error("Couldn't close session properly!", ex);
            }
        }

    }

    /**
     * Returns true if {@code relName} is known to be transitive.
     */
    public static boolean isTransitive(String relName){
        checkNotEmpty(relName, "Invalid relation name!");        
        return transitiveRelations.contains(relName);
    }
    
    /**
     * Returns the {@link #isCanonicalRelation(String) canonical} transitive relations (thus inverses are not included).
     */
    public static List<String> getCanonicalTransitiveRelations(){               
        return new ArrayList(canonicalTransitiveRelations);
    }

    /**
     * Returns all the transitive relations (inverses included).
     */
    public static List<String> getTransitiveRelations(){               
        return new ArrayList(transitiveRelations);
    }
    
    
    /**
     * Returns all the  {@link #isCanonicalRelation(String) canonical} {@code partof}
     *  relations (thus inverses are not included).
     */    
    public static List<String> getCanonicalPartOfRelations(){
        return new ArrayList(canonicalPartOfRelations);
    }

    /**
     * Returns all the {@code partof} relations (inverses included).
     */
    public static List<String> getPartOfRelations(){
        return new ArrayList(partOfRelations);
    }
    
    
    public static boolean isPartOf(String relName){
        checkNotEmpty(relName, "Invalid relation name!");
        return partOfRelations.contains(relName);
    }
}
