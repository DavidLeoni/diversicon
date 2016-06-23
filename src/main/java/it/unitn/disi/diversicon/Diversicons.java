package it.unitn.disi.diversicon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;
import org.h2.tools.RunScript;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;

import de.tudarmstadt.ukp.lmf.hibernate.HibernateConnect;
import de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;

import static de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics.*;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
//import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.StringUtils;
import it.unitn.disi.diversicon.internal.Internals;
import it.unitn.disi.diversicon.internal.Internals.ExtractedStream;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

/**
 * Utility class for S-match Uby
 * 
 * @since 0.1
 */
public final class Diversicons {

    /**
     * Supported compression formats for IO operations. It's a superset of {@link #SUPPORTED_ARCHIVE_FORMATS}
     * 
     * @since 0.1
     */
    public static final String[] SUPPORTED_COMPRESSION_FORMATS =   {
            "ar", "arj", "cpio", 
            "dump", "tar",  "zip", "lzma", "z", "snappy",
            "bzip2",  "xz", "gzip", "tar"};
 
    /**
     * A subset of {@link #SUPPORTED_COMPRESSION_FORMATS} holding more information 
     * about archive entries.
     * 
     * @since 0.1
     */
    public static final String[] SUPPORTED_ARCHIVE_FORMATS = {ArchiveStreamFactory.AR,
            ArchiveStreamFactory.ARJ,
            ArchiveStreamFactory.CPIO,
            ArchiveStreamFactory.DUMP,
            ArchiveStreamFactory.JAR,
            // ArchiveStreamFactory.SEVEN_Z, 
            ArchiveStreamFactory.TAR,
            ArchiveStreamFactory.ZIP};

    
    
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

    private static final String DEFAULT_H2_DB_NAME = "default-db";

    public static final String WORDNET_DIV_DB_RESOURCE_URI = "classpath:/it/unitn/disi/diversicon/data/div-wn30.sql.zip";


    public static final String WORDNET_UBY_XML_RESOURCE_URI = "classpath:/it/unitn/disi/diversicon/data/uby-wn30.xml.xz";

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
     * Sets {@code relNameA} as {@code relNameB}'s symmetric relation, and vice
     * versa.
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

        if (transitive) {
            transitiveRelations.add(relNameA);
            canonicalTransitiveRelations.add(relNameA);
            transitiveRelations.add(relNameB);
        }

        if (partof) {
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
    public static void dropCreateTables(
            DBConfig dbConfig) {

        LOG.info("Creating database " + dbConfig.getJdbc_url() + " ...");

        Configuration hcfg = getHibernateConfig(dbConfig, false);

        Session session = openSession(dbConfig, false);
        Transaction tx = null;

        SchemaExport se = new SchemaExport(hcfg);
        se.create(true, true);
        try {
            tx = session.beginTransaction();
            DbInfo dbInfo = new DbInfo();
            session.save(dbInfo);
            tx.commit();
        } catch (Exception ex) {
            LOG.error("Error while saving DbInfo! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivException("Error while while saving DbInfo!", ex);
        }
        session.flush();
        session.close();
        LOG.info("Done creating database " + dbConfig.getJdbc_url() + "  .");

    }

    /**
     * Creates a database based on the hibernate mappings. 
     * 
     * (adapted from LMFDBUtils.createTables(dbConfig) )
     * 
     * @since 0.1
     */
    public static void createTables(
            DBConfig dbConfig) {

        LOG.info("Creating database " + dbConfig.getJdbc_url() + " ...");

        Configuration hcfg = getHibernateConfig(dbConfig, false);

        Session session = openSession(dbConfig, false);
        Transaction tx = null;

        SchemaExport se = new SchemaExport(hcfg);
        se.create(true, true);
        try {
            tx = session.beginTransaction();
            DbInfo dbInfo = new DbInfo();
            session.save(dbInfo);
            tx.commit();
        } catch (Exception ex) {
            LOG.error("Error while saving DbInfo! Rolling back!");
            if (tx != null) {
                tx.rollback();
            }
            throw new DivException("Error while while saving DbInfo!", ex);
        }
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

        // to avoid   Caused by: org.hibernate.NonUniqueObjectException: a different object with the same identifier value was already associated with the session: [it.unitn.disi.diversicon.DivSynsetRelation#20]
        // when computing transitive closure 
        // See http://stackoverflow.com/a/32311508
        ret.setProperty("hibernate.id.new_generator_mappings", "true");
        
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
                    LOG.info("Skipping class customized by Diversicon: " + mapping.getDescription());
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

            if (resources.length == 0) {
                // div dirty
                String mydir = "file:///home/da/Da/prj/diversicon/prj/src/main/resources/hybernatemap/access/**/*.hbm.xml";
                LOG.info("Can't find resources, looking in " + mydir
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
     * Returns true if provided relation is canonical, that is, is privileged
     * wrt
     * the inverse it might have (example: since hypernymy is considered as
     * canonical, transitive
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
    public static String toString(@Nullable SynsetRelation sr) {

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
     * The list will contain only the {@link #isCanonicalRelation(String)
     * canonical} relations
     * and not their inverses.
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
     * 
     * @param dbConfig
     * 
     * @throws InvalidSchemaException 
     */
    public static Configuration checkSchema(DBConfig dbConfig){
        
        Configuration cfg = Diversicons.getHibernateConfig(dbConfig, true);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        SessionFactory sessionFactory;
        try {
            sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        } catch (HibernateException ex){
            throw new InvalidSchemaException("Failed validation by hibernate! DbConfig is " + Diversicons.toString(dbConfig, false), ex);
        }
        Session session = sessionFactory.openSession();

        // dirty but might work
        try {
            session.get(DbInfo.class, 0L);                        
        } catch (org.hibernate.exception.SQLGrammarException ex) {
            throw new InvalidSchemaException("Couldn't find DBInfo record! DbConfig is " + Diversicons.toString(dbConfig, false), ex);
        } finally {
            try {
                session.close();
            } catch (Exception ex) {
                LOG.error("Couldn't close session properly! DbConfig is " + Diversicons.toString(dbConfig, false), ex);
            }
        }
        
        return cfg;

    }

    /**
     * Returns true if provided database configuration points to an
     * existing database with all needed tables.
     * 
     * @since 0.1
     */
    public static boolean isSchemaValid(DBConfig dbConfig) {

        try {
           checkSchema(dbConfig);
           return true;
        } catch (InvalidSchemaException ex){
            return false;
        }
        
    }

    /**
     * Returns a string description of provided {@code dbConfig}
     * 
     * @since 0.1
     */
    public static String toString(DBConfig dbConfig, boolean showPassword) {
        checkNotNull(dbConfig);

        String pwd;
        if (showPassword) {
            pwd = dbConfig.getPassword();
        } else {
            pwd = "***REDACTED***";
        }
        return "DBConfig [host=" + dbConfig.getHost() + ", jdbc_driver_class=" + dbConfig.getJdbc_driver_class()
                + ", db_vendor=" + dbConfig.getDb_vendor()
                + ", jdbc_url=" + dbConfig.getJdbc_url() + ", user=" + dbConfig.getUser() + ", password=" + pwd + "]";
    }

    /**
     * Returns true if {@code relName} is known to be transitive.
     */
    public static boolean isTransitive(String relName) {
        checkNotEmpty(relName, "Invalid relation name!");
        return transitiveRelations.contains(relName);
    }

    /**
     * Returns the {@link #isCanonicalRelation(String) canonical} transitive
     * relations (thus inverses are not included).
     */
    public static List<String> getCanonicalTransitiveRelations() {
        return new ArrayList(canonicalTransitiveRelations);
    }

    /**
     * Returns all the transitive relations (inverses included).
     */
    public static List<String> getTransitiveRelations() {
        return new ArrayList(transitiveRelations);
    }

    /**
     * Returns all the {@link #isCanonicalRelation(String) canonical}
     * {@code partof}
     * relations (thus inverses are not included).
     */
    public static List<String> getCanonicalPartOfRelations() {
        return new ArrayList(canonicalPartOfRelations);
    }

    /**
     * Returns all the {@code partof} relations (inverses included).
     */
    public static List<String> getPartOfRelations() {
        return new ArrayList(partOfRelations);
    }

    public static boolean isPartOf(String relName) {
        checkNotEmpty(relName, "Invalid relation name!");
        return partOfRelations.contains(relName);
    }

    /**
     * Extracts a lexical resource name from an xml file
     * 
     * @throws DivNotFoundException
     */
    // implementation is unholy
    public static String extractNameFromLexicalResource(final String  lexResUrl) {
        SAXReader reader = new SAXReader(false);
        
        ExtractedStream es = Internals.readData(lexResUrl, true);
        
        reader.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException, IOException {
                if (systemId.endsWith(".dtd")) {
                    return new InputSource(new StringReader(""));
                }
                return null;
            }
        });
        reader.setDefaultHandler(new LexicalResourceNameHandler());
        try {
            reader.read(es.stream());
        } catch (DocumentException e) {

            if (e.getMessage()
                 .contains(LexicalResourceNameHandler.FOUND_NAME)) {

                return e.getMessage()
                        .substring(0, e.getMessage()
                                       .indexOf(LexicalResourceNameHandler.FOUND_NAME));
            } else {
                throw new DivException("Error while extracting lexical resource name from "
                        + lexResUrl + "!", e);
            }
        }
        throw new DivNotFoundException("Couldn't find attribute name in lexical resource "
                + lexResUrl + "  !");
    }

    /**
     * div dirty - What a horrible class
     */
    private static class LexicalResourceNameHandler implements ElementHandler {

        private static final String FOUND_NAME = "<--FOUNDNAME";

        @Override
        public void onStart(ElementPath elementPath) {
            Element el = elementPath.getCurrent();
            String elName = el.getName();

            // Remove empty attributes and invalid characters.
            Iterator<?> attrIter = el.attributeIterator();
            while (attrIter.hasNext()) {
                Attribute attr = (Attribute) attrIter.next();
                if ("NULL".equals(attr.getStringValue())) {
                    attrIter.remove();
                } else {
                    attr.setValue(StringUtils.replaceNonUtf8(attr.getValue()));
                }
            }

            if ("LexicalResource".equals(elName)) {
                // I know this is horrible, can't find better method :P
                String ret = el.attributeValue("name");
                if (ret == null) {
                    throw new DivNotFoundException("Couldn't find attribute 'name' in lexical resource!");
                } else {
                    throw new RuntimeException(ret + FOUND_NAME);
                }
            }

        }

        @Override
        public void onEnd(ElementPath elementPath) {
        }

    }

    /**
     * 
     * @param filePath
     *            the path to the database, which must end with just the
     *            database name
     *            (so without the {@code .h2.db})
     */
    public static DBConfig makeDefaultH2FileDbConfig(String filePath) {
        checkNotEmpty(filePath, "Invalid file path!");
        checkArgument(!filePath.endsWith(".db"), "File path must end just with the databaset name, "
                + "without the '.h2.db'! Found instead: " + filePath);

        DBConfig ret = new DBConfig();
        ret.setDb_vendor("de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect");
        ret.setJdbc_driver_class("org.h2.Driver");
        ret.setJdbc_url("jdbc:h2:file:" + filePath);
        ret.setUser("root");
        ret.setPassword("pass");
        return ret;
    }

    /**
     * 
     * @param dbName
     *            Uniquely identifies the db among all in-memory dbs.
     * @param if compressed db is compressed, so occupies less space but has slower access time
     */
    public static DBConfig makeDefaultH2InMemoryDbConfig(String dbName, boolean compressed) {
        checkNotEmpty(dbName, "Invalid db name!");

        String mem;
        if (compressed){
            mem = "nioMemLZF";
        } else {
            mem = "mem";
        }
        
        DBConfig ret = new DBConfig();
        ret.setDb_vendor("de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect");
        ret.setJdbc_driver_class("org.h2.Driver");
        ret.setJdbc_url("jdbc:h2:" + mem  + ":" + dbName + ";DB_CLOSE_DELAY=-1");
        ret.setUser("root");
        ret.setPassword("pass");
        return ret;
    }

    /**
     * Restores an h2 database from a sql dump 
     * (possibly compressed in one of {@link #SUPPORTED_COMPRESSION_FORMATS}).
     * {@code dbConfig} MUST point to a non-existing database, otherwise
     * behaviour is unspecified.
     *
     * @param dumpUrl
     *            For Wordnet 3.0 packaged dump, you can use
     *            {@link Diversicons#WORDNET_RESOURCE_URL}
     * @throws DivIoException
     *             if an IO error occurs
     * 
     * @since 0.1
     */
    public static void restoreH2Dump(String dumpUrl, DBConfig dbConfig) {
        Internals.checkNotBlank(dumpUrl, "invalid sql/archive resource path!");
        checkH2Db(dbConfig);

        Date start = new Date();

        LOG.info("Restoring database " + dbConfig.getJdbc_url() + "  ...");
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ex) {
            throw new DivIoException("Error while loading h2 driver!", ex);
        }
        ExtractedStream extractedStream = Internals.readData(dumpUrl, true);
               
        Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;
        try {
            /**
             * from
             * http://www.h2database.com/html/performance.html#fast_import
             * Made some tests, performance gain seems < 1 s :-(
             */
            String saveVars = ""
                    + "  SET @DIV_SAVED_LOG @LOG;"
                    + "  SET @DIV_SAVED_CACHE_SIZE @CACHE_SIZE;"
                    + "  SET @DIV_SAVED_LOCK_MODE @LOCK_MODE;"
                    + "  SET @DIV_SAVED_UNDO_LOG @UNDO_LOG;";

            String setFastOptions = "    SET @LOG 0;"
                    + "  SET @CACHE_SIZE 65536;"
                    + "  SET @LOCK_MODE 0;"
                    + "  SET @UNDO_LOG 0;";

            String restoreSavedVars = ""
                    + "  SET @LOG @DIV_SAVED_LOG;"
                    + "  SET @CACHE_SIZE @DIV_SAVED_CACHE_SIZE;"
                    + "  SET @LOCK_MODE @DIV_SAVED_LOCK_MODE;"
                    + "  SET @UNDO_LOG @DIV_SAVED_UNDO_LOG;";

            // todo need to improve connection with dbConfig params

            conn = DriverManager.getConnection(
                    dbConfig.getJdbc_url(),
                    dbConfig.getUser(),
                    dbConfig.getPassword());

            stat = conn.createStatement();
            stat.execute(saveVars);
            stat.execute(setFastOptions);
            RunScript.execute(conn, new InputStreamReader(extractedStream.stream()));
            stat.execute(restoreSavedVars);
            conn.commit();
            Date end = new Date();
            LOG.info("Done restoring database " + dbConfig.getJdbc_url());
            LOG.info("Elapsed time: " + Math.ceil(((end.getTime() - start.getTime()) / 1000)) + "s");

            // TODO: here it should automatically fix mixing schema parts...
            if (!Diversicons.isSchemaValid(dbConfig)) {
                throw new InvalidSchemaException("Restored db but found invalid schema!");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while restoring h2 db!", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    LOG.error("Error while closing result set", ex);
                }
            }
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException ex) {
                    LOG.error("Error while closing Statement", ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOG.error("Error while closing connection", ex);
                }
            }

        }

    }

    /**
     * @since 0.1
     */
    public static boolean exists(DBConfig dbConfig) {

        Configuration cfg = Diversicons.getHibernateConfig(dbConfig, false);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        Session session = sessionFactory.openSession();

        // dude, this is crude
        return session.createQuery("from java.lang.Object")
                      .iterate()
                      .hasNext();

    }

    /**
     * 
     * Checks if provided db configuration points to an empty database.
     * @since 0.1
     */
    public static boolean isEmpty(DBConfig dbConfig){
        checkH2Db(dbConfig);
        
        Connection conn = null;
        try {
            
            conn = getH2Connection(dbConfig);
            Statement stat = conn.createStatement();            
            ResultSet rs = stat.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' ");            
            return !rs.next();
            
        } catch (SQLException ex) {            
            throw new DivIoException("Something went wrong!", ex);
        } finally{
            if (conn != null){
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.error("Couldn't close connection, db config is " + toString(dbConfig, false), e);
                }    
            }
                
        }
    }
    
    /**
     * 

     * @throws DivIoConnection
     */
    public static Connection getH2Connection(DBConfig dbConfig){
        
        try {
            Class.forName("org.h2.Driver");
               
            Connection conn;

            conn = DriverManager.getConnection(
                    dbConfig.getJdbc_url(),
                    dbConfig.getUser(),
                    dbConfig.getPassword());
            return conn;            
        } catch (SQLException | ClassNotFoundException e) {            
            throw new DivIoException("Error while connecting to H2 db! db config is " + toString(dbConfig, false), e);
        }
        

    }
    
    public static boolean isH2Db(DBConfig dbConfig) {
        checkNotNull(dbConfig);
        return dbConfig.getJdbc_driver_class()
                .contains("h2");
    }
    
    /**
     * Checks provided {@code dbConfig} points to an H2 database.
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1
     */
    public static void checkH2Db(DBConfig dbConfig) {
        checkNotNull(dbConfig);
        if (!isH2Db(dbConfig)) {
            throw new IllegalArgumentException("Only H2 database is supported for now! Found instead "
                    + Diversicons.toString(dbConfig, false));
        }
    }


}
