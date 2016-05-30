package it.unitn.disi.diversicon;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
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
    
    
    private static final List<String> CANONICAL_RELATIONS = Collections.unmodifiableList(
            Arrays.asList(ERelNameSemantics.HYPERNYM,
                    ERelNameSemantics.HYPERNYMINSTANCE,
                    ERelNameSemantics.HOLONYM,
                    ERelNameSemantics.HOLONYMCOMPONENT,
                    ERelNameSemantics.HOLONYMMEMBER,
                    ERelNameSemantics.HOLONYMPART,
                    ERelNameSemantics.HOLONYMPORTION,
                    ERelNameSemantics.HOLONYMSUBSTANCE));

    private static final Map<String, ERelTypeSemantics> CANONICAL_RELATION_TYPES = Collections.unmodifiableMap(
            Internals.newMap(ERelNameSemantics.HYPERNYM, ERelTypeSemantics.taxonomic,
                    ERelNameSemantics.HYPERNYMINSTANCE, ERelTypeSemantics.taxonomic,
                    ERelNameSemantics.HOLONYM, ERelTypeSemantics.partWhole,
                    ERelNameSemantics.HOLONYMCOMPONENT, ERelTypeSemantics.partWhole,
                    ERelNameSemantics.HOLONYMMEMBER, ERelTypeSemantics.partWhole,
                    ERelNameSemantics.HOLONYMPART, ERelTypeSemantics.partWhole,
                    ERelNameSemantics.HOLONYMPORTION, ERelTypeSemantics.partWhole,
                    ERelNameSemantics.HOLONYMSUBSTANCE, ERelTypeSemantics.partWhole));

    private static Map<String, String> inverseRelations = new HashMap();


    /**
     * Mappings from Uby classes to out own custom ones.
     * 
     */
    private static LinkedHashMap<String, String> customClassMappings;
    
    
    static {
        putInverseRelations(ANTONYM, ANTONYM);
        putInverseRelations(HYPERNYM, HYPONYM);
        putInverseRelations(HYPERNYMINSTANCE, HYPONYMINSTANCE);
        putInverseRelations(HOLONYM, MERONYM);
        putInverseRelations(HOLONYMCOMPONENT, MERONYMCOMPONENT);
        putInverseRelations(HOLONYMMEMBER, MERONYMMEMBER);
        putInverseRelations(HOLONYMPART, MERONYMPART);
        putInverseRelations(HOLONYMPORTION, MERONYMPORTION);
        putInverseRelations(HOLONYMSUBSTANCE, MERONYMSUBSTANCE);
        
        customClassMappings = new LinkedHashMap();
        customClassMappings.put(de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation.class.getCanonicalName(),
                DivSynsetRelation.class.getCanonicalName());        

    }



    
    private Diversicons() {
    }

    /**
     * Sets {@code a} as {@code b}'s symmetric type, and vice versa.
     *
     * @param a
     *            pointer type
     * @param b
     *            pointer type
     */
    private static void putInverseRelations(String a, String b) {
        checkNotEmpty(a, "Invalid first relation!");
        checkNotEmpty(b, "Invalid second relation!");

        inverseRelations.put(a, b);
        inverseRelations.put(b, a);
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
     * Note: if false is returned it means we <i> don't know </i> the relations
     * are actually inverses.
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

    
    static Session openSession(DBConfig dbConfig, boolean validate){
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
     * @param validate if true database schema is validated upon first connection.  
     * 
     * @since 0.1
     */
    public static Configuration getHibernateConfig(DBConfig dbConfig, boolean validate) {       

        Configuration ret = new Configuration()
                                                .addProperties(HibernateConnect.getProperties(dbConfig.getJdbc_url(),
                                                        dbConfig.getJdbc_driver_class(),
                                                        dbConfig.getDb_vendor(), dbConfig.getUser(),
                                                        dbConfig.getPassword(), dbConfig.isShowSQL()));
        
        if (validate){
            ret.setProperty("hibernate.hbm2ddl.auto", "validate");            
        } else {
            ret.setProperty("hibernate.hbm2ddl.auto", "none");
        }

        LOG.info("Going to load default UBY hibernate mappings...");

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
        LOG.info("Done loading default UBY hibernate mappings...");

        LOG.info("Loading custom S-Match Uby hibernate mappings... ");

        try {

            Resource[] resources = new PathMatchingResourcePatternResolver(Diversicons.class.getClassLoader())
                                                                                                           .getResources(
                                                                                                                   "hybernatemap/access/**/*.hbm.xml");
            for (Resource r : resources) {                
                ret.addURL(r.getURL());
                LOG.info("  Loaded " + r.getURL());
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error while loading hibernate mappings!", ex);
        }

        LOG.info("Done loading custom mappings. ");       
              
        return ret;      
    }
    
    /**
     * 
     * Saves a LexicalResource complete with all the lexicons, synsets, etc into
     * a database. This method is suitable only for small lexical resources and
     * generally for testing purposes. If you have a big resource, stream the
     * loading by providing your implementation of <a href=
     * "https://github.com/dkpro/dkpro-uby/blob/master/de.tudarmstadt.ukp.uby.persistence.transform-asl/src/main/java/de/tudarmstadt/ukp/lmf/transform/LMFDBTransformer.java"
     * target="_blank"> LMFDBTransformer</a> and then call {@code transform()} on it
     * instead.
     * 
     * @param lexicalResourceId
     *            todo don't know well the meaning
     * 
     * @throws DivException
     * @since 0.1
     */
    public static void saveLexicalResourceToDb(
            DBConfig dbConfig,
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
     * Returns true if provided relation is canonical
     * 
     * @since 0.1
     */
    public static boolean isCanonical(String relName) {
        Internals.checkNotEmpty(relName, "Invalid relation name!");
        return CANONICAL_RELATIONS.contains(relName);
    }

    /**
     * Returns the type of the provided relation.
     * 
     * @throws DivNotFoundException
     * @since 0.1
     */
    public static ERelTypeSemantics getCanonicalRelationType(String relName) {

        ERelTypeSemantics ret = CANONICAL_RELATION_TYPES.get(relName);

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
     * The list will contain only the canonical relations and not their inverse.
     * 
     * @since 0.1
     */
    public static List<String> getCanonicalRelations() {
        return CANONICAL_RELATIONS;
    }

    /**
     * Returns true if provided database configuration points to an 
     * existing database with all needed tables.
     * 
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
        } catch ( org.hibernate.exception.SQLGrammarException ex){
            return false;  
        } finally {
            try {
                session.close();
            } catch (Exception ex){
                LOG.error("Couldn't close session properly!", ex);
            }
        }
        
        
    }


}
