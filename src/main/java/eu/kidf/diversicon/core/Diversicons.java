package eu.kidf.diversicon.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.xerces.impl.Constants;
import org.basex.core.Context;
import org.basex.io.serial.Serializer;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.h2.tools.RunScript;
import org.hibernate.HibernateException;
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
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import de.tudarmstadt.ukp.lmf.hibernate.HibernateConnect;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;

import static de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics.*;
import static eu.kidf.diversicon.core.internal.Internals.checkArgument;
import static eu.kidf.diversicon.core.internal.Internals.checkNotBlank;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;
import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import eu.kidf.diversicon.core.BuildInfo;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.exceptions.DivIoException;
import eu.kidf.diversicon.core.exceptions.DivNotFoundException;
import eu.kidf.diversicon.core.exceptions.InvalidSchemaException;
import eu.kidf.diversicon.core.exceptions.InvalidXmlException;
import eu.kidf.diversicon.core.internal.DivXmlValidator;
import eu.kidf.diversicon.core.internal.ExtractedStream;
import eu.kidf.diversicon.core.internal.Internals;
import eu.kidf.diversicon.data.DivWn31;

import org.apache.xerces.dom.DOMInputImpl;

/**
 * Utility class for {@link Diversicon}
 * 
 * @since 0.1.0
 */
public final class Diversicons {

    /**
     * 
     * @since 0.1.0
     */
    public static final String SCHEMA_VERSION_1 = "1";

    /**
     * 
     * @since 0.1.0
     */
    public static final String SCHEMA_VERSION_1_0 = "1.0";

    /**
     * @since 0.1.0
     */
    public static final String CLASSPATH_WEBSITE = "classpath:/website/";

    /**
     * @since 0.1.0
     */
    public static final String DTD_FILENAME = "diversicon.dtd";

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_FRAGMENT = "schema/1";

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_0_FRAGMENT = SCHEMA_1_FRAGMENT + ".0";

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_NAMESPACE = BuildInfo.of(Diversicons.class)
                                                             .getServer()
            + "/" + SCHEMA_1_FRAGMENT;

    /**
     * Remember x.y namespaces should only be used to locate files!
     * 
     * @since 0.1.0
     */
    public static final String SCHEMA_1_0_NAMESPACE = BuildInfo.of(Diversicons.class)
                                                               .getServer()
            + "/" + SCHEMA_1_0_FRAGMENT;

    /**
     * @since 0.1.0
     */
    public static final String DTD_1_PUBLIC_URL = SCHEMA_1_NAMESPACE + SCHEMA_1_FRAGMENT + "/" + DTD_FILENAME;

    /**
     * @since 0.1.0
     */
    public static final String DTD_1_0_PUBLIC_URL = SCHEMA_1_0_NAMESPACE + "/" + DTD_FILENAME;

    /**
     * Maximum number of import issues printed to screen
     * 
     * @since 0.1.0
     */
    public static final int DEFAULT_LOG_LIMIT = 20;

    /**
     * @since 0.1.0
     */
    public static final String DTD_1_0_CLASSPATH_URL = CLASSPATH_WEBSITE
            + SCHEMA_1_0_FRAGMENT + "/" + DTD_FILENAME;

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_FILENAME = "diversicon.xsd";

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_0_CLASSPATH_URL = CLASSPATH_WEBSITE
            + SCHEMA_1_0_FRAGMENT
            + "/" + SCHEMA_FILENAME;

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_PUBLIC_URL = SCHEMA_1_NAMESPACE + "/" + SCHEMA_FILENAME;

    /**
     * @since 0.1.0
     */
    public static final String SCHEMA_1_0_PUBLIC_URL = SCHEMA_1_0_NAMESPACE + "/" + SCHEMA_FILENAME;

    /**
     * If you set this system property, temporary files won't be deleted at JVM
     * shutdown.
     * 
     * @since 0.1.0
     */
    public static final String PROPERTY_DEBUG_KEEP_TEMP_FILES = "diversicon.debug.keep-temp-files";

    /**
     * 
     * @since 0.1.0
     */
    public static final String DEFAULT_AUTHOR = "Default author";

    /**
     * The url protocol for lexical resources loaded from memory.
     */
    public static final String MEMORY_PROTOCOL = "memory";

    /**
     * @since 0.1.0
     */
    public static final String XQUERY_IN_FILE_DECLARATION = "declare variable $in-file external;\n";

    /**
     * @since 0.1.0
     */
    public static final String XQUERY_ROOT_VAR = "$root";

    /**
     * @since 0.1.0
     */
    public static final String XQUERY_UPDATE_PROLOGUE = XQUERY_IN_FILE_DECLARATION
            + "    copy " + XQUERY_ROOT_VAR + " := doc($in-file) "
            + "\n    modify (   ";

    /**
     * @since 0.1.0
     */
    public static final String XQUERY_UPDATE_END = ") return $root";

    /**
     * @since 0.1.0
     */
    public static final String UBY_DTD_TO_SCHEMA_XQUERY_CLASSPATH_URL = "classpath://uby-dtd-to-div-schema.xql";

    /**
     * Suggested max length for lexical resource names, which are also prefixes
     * like {@code wn31}, or {@code sm}
     * 
     * @since 0.1.0
     */
    public static final int LEXICAL_RESOURCE_PREFIX_SUGGESTED_LENGTH = 5;

    /**
     * @since 0.1.0
     */
    public static final String NAMESPACE_PREFIX_PATTERN_DESCRIPTION = "A diversicon prefix"
            + " is an XML NCName with the further restriction"
            + " of having a prefix (like 'wn31') followed by an underscore '_' and a word"
            + " beginning with a unicode letter. The rest of the word may only contain"
            + " unicode letters, dots '.', dashes '-', underscores '_'. Spaces "
            + " or are not allowed.";

    /**
     * {@value #NAMESPACE_PREFIX_PATTERN_DESCRIPTION}
     * 
     * @since 0.1.0
     */
    public static final Pattern NAMESPACE_PREFIX_PATTERN = Pattern.compile("\\p{L}(\\w|-|\\.)*", Pattern.UNICODE_CASE);

    /**
     * @since 0.1.0
     */
    public static final String NAMESPACE_SEPARATOR = "_";

    /**
     * 
     * 
     * @since 0.1.0
     */
    public static final String NAMESPACE_ID_PATTERN_DESCRIPTION = "A diversicon ID "
            + " should look something like 'wn31_blabla', with the first part being a prefix (i.e. 'wn31'),"
            + " followed by an underscore '_'. More technically, it should be"
            + " an XML NCName with the further restriction"
            + " of having a prefix followed by an underscore and a word"
            + " beginning with a unicode letter. The rest of the word may contain"
            + " unicode letters, dots '.', dashes '-', underscores '_', but no spaces.";

    /**
     * {@value #NAMESPACE_ID_PATTERN_DESCRIPTION}
     * 
     * @since 0.1.0
     */
    public static final Pattern ID_PATTERN = Pattern.compile(
            NAMESPACE_PREFIX_PATTERN.toString()
                    + NAMESPACE_SEPARATOR
                    + "\\p{L}(\\w|-|_|\\.)*");

    /**
     * 
     * Mnemonic shorthand for H2 database
     * 
     * @since 0.1.0
     */
    public static final String H2_IDENTIFIER = "h2";

    /**
     * Maps a lowercased db mnemonic (like 'h2') to its driver name (like
     * 'org.h2.Driver')
     * 
     * @since 0.1.0
     */
    private static final Map<String, String> DATABASE_DRIVERS = Collections.unmodifiableMap(
            Internals.newMap(H2_IDENTIFIER, "org.h2.Driver"));

    public static final String BUILD_PROPERTIES_PATH = "tod.commons.build.properties";

    /**
     * Path relative to user home of the file cache of Diverscon.
     * 
     * @since 0.1.0
     */
    public static final String CACHE_PATH = ".config/diversicon/cache/";

    /**
     * Supported compression formats for I/O operations. It's a superset of
     * {@link #SUPPORTED_ARCHIVE_FORMATS}
     * 
     * @since 0.1.0
     */
    public static final String[] SUPPORTED_COMPRESSION_FORMATS = {
            "ar", "arj", "cpio",
            "dump", "tar", "zip", "lzma", "z", "snappy",
            "bzip2", "xz", "gzip", "tar" };

    /**
     * A subset of {@link #SUPPORTED_COMPRESSION_FORMATS} holding more
     * information
     * about archive entries.
     * 
     * @since 0.1.0
     */
    public static final String[] SUPPORTED_ARCHIVE_FORMATS = { ArchiveStreamFactory.AR,
            ArchiveStreamFactory.ARJ,
            ArchiveStreamFactory.CPIO,
            ArchiveStreamFactory.DUMP,
            ArchiveStreamFactory.JAR,
            // ArchiveStreamFactory.SEVEN_Z,
            ArchiveStreamFactory.TAR,
            ArchiveStreamFactory.ZIP };
    
    /**
     * Default user for databases.
     * 
     * @since 0.1.0
     */
    public static final String DEFAULT_USER = "root";

    /**
     * Default password for databases.
     * 
     * @since 0.1.0
     */    
    public static final String DEFAULT_PASSWORD = "pass";    

    private static final Logger LOG = LoggerFactory.getLogger(Diversicons.class);

    /**
     * List of known relations, excluding the inverses.
     */
    private static final Set<String> canonicalRelations = new LinkedHashSet<>();

    /**
     * List of known relations, (including the inverses)
     */
    private static final Map<String, ERelTypeSemantics> relationTypes = new LinkedHashMap<String, ERelTypeSemantics>();

    private static final LinkedHashSet<String> transitiveRelations = new LinkedHashSet<String>();
    private static final LinkedHashSet<String> canonicalTransitiveRelations = new LinkedHashSet<String>();

    private static final LinkedHashSet<String> partOfRelations = new LinkedHashSet<String>();
    private static final LinkedHashSet<String> canonicalPartOfRelations = new LinkedHashSet<String>();


    /**
     * 
     * Couldn't find a smarter default way to provide input files in scripts.
     * 
     * @since 0.1.0
     */
    private static final String XQUERY_IN_FILE_VAR = "in-file";

    private static Map<String, String> inverseRelations = new HashMap<>();

    /**
     * Mappings from Uby classes to out own custom ones.
     * 
     */
    private static LinkedHashMap<String, String> customClassMappings;

    static {
        putRelations(HYPERNYM, HYPONYM, ERelTypeSemantics.taxonomic, true, false);
        putRelations(HYPERNYMINSTANCE, HYPONYMINSTANCE, ERelTypeSemantics.taxonomic, false, false);
        putRelations(HOLONYM, MERONYM, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMCOMPONENT, MERONYMCOMPONENT, ERelTypeSemantics.partWhole, false, true); // todo
                                                                                                    // is
                                                                                                    // it
                                                                                                    // transitive?
        putRelations(HOLONYMMEMBER, MERONYMMEMBER, ERelTypeSemantics.partWhole, false, true);
        putRelations(HOLONYMPART, MERONYMPART, ERelTypeSemantics.partWhole, true, true);
        putRelations(HOLONYMPORTION, MERONYMPORTION, ERelTypeSemantics.partWhole, false, true); // todo
                                                                                                // is
                                                                                                // it
                                                                                                // transitive?
        putRelations(HOLONYMSUBSTANCE, MERONYMSUBSTANCE, ERelTypeSemantics.partWhole, false, true);
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * (adapted from
     * {@link de.tudarmstadt.ukp.lmf.transform.LMFDBUtils#createTables(DBConfig)
     * LMFDBUtils.createTables} )
     * 
     * If database doesn't exist no exception is thrown.
     * 
     * @throws DivException
     * @since 0.1.0
     */
    public static void dropCreateTables(DBConfig dbConfig) {

        LOG.info("Recreating tables in database  " + dbConfig.getJdbc_url() + "    ...");

        Configuration hcfg = getHibernateConfig(dbConfig, false);

        Session session = openSession(dbConfig, false);
        Transaction tx = null;

        SchemaExport se = new SchemaExport(hcfg);
        se.create(false, true);
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
        LOG.info("Done recreating tables in database:  " + dbConfig.getJdbc_url());

    }

    /**
     * Creates a database based on the hibernate mappings
     * 
     * (adapted from
     * {@link de.tudarmstadt.ukp.lmf.transform.LMFDBUtils#createTables(DBConfig)
     * LMFDBUtils.createTables} )
     * 
     * @since 0.1.0
     */
    public static void createTables(DBConfig dbConfig) {

        LOG.info("Creating tables in database  " + dbConfig.getJdbc_url() + " ...");

        Configuration hcfg = getHibernateConfig(dbConfig, false);

        Session session = openSession(dbConfig, false);
        Transaction tx = null;

        SchemaExport se = new SchemaExport(hcfg);
        se.create(false, true);
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
        LOG.info("Done creating tables in database  " + dbConfig.getJdbc_url());

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
     * @since 0.1.0
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
     * <p>
     * NOTE: returned configuration will not do any change to an already
     * present database, nor it will create a new one if none is present.
     * </p>
     * 
     * @param validate
     *            if true database schema is validated upon first connection.
     * 
     * @since 0.1.0
     */
    public static Configuration getHibernateConfig(DBConfig dbConfig, boolean validate) {

        Configuration ret = new Configuration()
                                               .addProperties(HibernateConnect.getProperties(dbConfig.getJdbc_url(),
                                                       dbConfig.getJdbc_driver_class(),
                                                       dbConfig.getDb_vendor(), dbConfig.getUser(),
                                                       dbConfig.getPassword(), dbConfig.isShowSQL()));

        // to avoid Caused by: org.hibernate.NonUniqueObjectException: a
        // different object with the same identifier value was already
        // associated with the session:
        // [eu.kidf.diversicon.DivSynsetRelation#20]
        // when computing transitive closure
        // See http://stackoverflow.com/a/32311508
        ret.setProperty("hibernate.id.new_generator_mappings", "true");

        // fix for https://github.com/diversicon-kb/diversicon/issues/13
        ret.setProperty("acquireRetryAttempts", "1");

        if (validate) {
            ret.setProperty("hibernate.hbm2ddl.auto", "validate");
        } else {
            ret.setProperty("hibernate.hbm2ddl.auto", "none");
        }

        LOG.debug("Going to load UBY hibernate mappings...");

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
                    LOG.debug("Skipping class customized by Diversicon: " + mapping.getDescription());
                } else {
                    loadHibernateXml(ret, mapping);

                }
            }

        } catch (IOException e) {
            throw new DivException("Error while loading hibernate mappings!", e);
        }
        LOG.debug("Done loading UBY hibernate mappings...");

        LOG.debug("Loading custom Diversicon hibernate mappings... ");

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
                LOG.debug("  Loaded " + r.getURL());
            }

        } catch (Exception ex) {
            throw new RuntimeException("Error while loading hibernate mappings!", ex);
        }

        LOG.debug("Done loading Diversicon custom mappings. ");

        return ret;
    }

    /**
     * Returns true if provided relation is canonical, that is, is privileged
     * wrt
     * the inverse it might have (example: since hypernymy is considered as
     * canonical, transitive
     * closure graph is computed only for hypernym, not hyponym)
     * 
     * @since 0.1.0
     */
    public static boolean isCanonicalRelation(String relName) {
        Internals.checkNotEmpty(relName, "Invalid relation name!");
        return canonicalRelations.contains(relName);
    }

    /**
     * Returns the type of the provided relation.
     * 
     * @throws DivNotFoundException
     * @since 0.1.0
     */
    public static ERelTypeSemantics getRelationType(String relName) {

        ERelTypeSemantics ret = relationTypes.get(relName);

        if (ret == null) {
            throw new DivNotFoundException("There is no relation type associated to relation " + relName);
        }
        return ret;
    }

    /**
     * 
     * @since 0.1.0
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
     * @since 0.1.0
     */
    public static List<String> getCanonicalRelations() {
        return new ArrayList(canonicalRelations);
    }

    /**
     * Returns a list of all relations used by Diversicon, in
     * {@link de.tudarmstadt.ukp.uby.lmf.model.ERelNameSemantics Uby format}
     * (including the inverses)
     * 
     * @since 0.1.0
     */
    public static List<String> getRelations() {
        return new ArrayList(relationTypes.keySet());
    }

    /**
     * 
     * @throws InvalidSchemaException
     * 
     * @since 0.1.0
     */
    public static Configuration checkSchema(DBConfig dbConfig) {

        Configuration cfg = Diversicons.getHibernateConfig(dbConfig, true);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(cfg.getProperties());
        SessionFactory sessionFactory;
        try {
            sessionFactory = cfg.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry());
        } catch (HibernateException ex) {
            throw new InvalidSchemaException(
                    "Failed validation by hibernate! DbConfig is " + Diversicons.toString(dbConfig, false), ex);
        }
        Session session = sessionFactory.openSession();

        // dirty but might work
        try {
            session.get(DbInfo.class, 0L);
        } catch (org.hibernate.exception.SQLGrammarException ex) {
            throw new InvalidSchemaException(
                    "Couldn't find DBInfo record! DbConfig is " + Diversicons.toString(dbConfig, false), ex);
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
     * @since 0.1.0
     */
    public static boolean isSchemaValid(DBConfig dbConfig) {

        try {
            checkSchema(dbConfig);
            return true;
        } catch (InvalidSchemaException ex) {
            return false;
        }

    }

    /**
     * Returns a string description of provided {@code dbConfig}
     * 
     * @since 0.1.0
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
     * 
     * @since 0.1.0
     */
    public static boolean isTransitive(String relName) {
        checkNotEmpty(relName, "Invalid relation name!");
        return transitiveRelations.contains(relName);
    }

    /**
     * Returns the {@link #isCanonicalRelation(String) canonical} transitive
     * relations (thus inverses are not included).
     * 
     * @since 0.1.0
     */
    public static List<String> getCanonicalTransitiveRelations() {
        return new ArrayList(canonicalTransitiveRelations);
    }

    /**
     * Returns all the transitive relations (inverses included).
     * 
     * @since 0.1.0
     */
    public static List<String> getTransitiveRelations() {
        return new ArrayList(transitiveRelations);
    }

    /**
     * Returns all the {@link #isCanonicalRelation(String) canonical}
     * {@code partof}
     * relations (thus inverses are not included).
     * 
     * @since 0.1.0
     */
    public static List<String> getCanonicalPartOfRelations() {
        return new ArrayList(canonicalPartOfRelations);
    }

    /**
     * Returns all the {@code partof} relations (inverses included).
     * 
     * @since 0.1.0
     */
    public static List<String> getPartOfRelations() {
        return new ArrayList(partOfRelations);
    }

    /**
     * @since 0.1.0
     */
    public static boolean isPartOf(String relName) {
        checkNotEmpty(relName, "Invalid relation name!");
        return partOfRelations.contains(relName);
    }

    /**
     * Creates the default configuration to access a file H2 database. NOTE: if
     * database does not exist it is created (but tables are not!)
     * 
     * @param filePath
     *            the path to a database, which must end with just the
     *            database name
     *            (so without the {@code .h2.db}).
     * 
     * @since 0.1.0
     */
    public static DBConfig h2MakeDefaultFileDbConfig(String filePath, boolean readOnly) {
        checkNotEmpty(filePath, "Invalid file path!");
        checkArgument(!filePath.endsWith(".db"), "File path must end just with the databaset name, "
                + "without the '.h2.db'! Found instead: " + filePath);

        String readOnlyString;
        if (readOnly) {
            readOnlyString = ";ACCESS_MODE_DATA=r";
        } else {
            readOnlyString = "";
        }

        DBConfig ret = h2MakeDefaultCommonDbConfig();
        ret.setJdbc_url("jdbc:h2:file:" + filePath + readOnlyString);

        return ret;
    }

    /**
     * 
     * Creates the default configuration to access an in-memory H2 database.
     * NOTE: if database does not exist it is created (but tables are not!)
     * 
     * @param dbName
     *            Uniquely identifies the db among all in-memory dbs.
     * @param compressed
     *            if db is compressed, occupies less space but has
     *            slower access time
     * @since 0.1.0
     */
    public static DBConfig h2MakeDefaultInMemoryDbConfig(String dbName, boolean compressed) {
        checkNotEmpty(dbName, "Invalid db name!");

        String mem;
        if (compressed) {
            mem = "nioMemLZF";
            throw new UnsupportedOperationException(
                    "Compressed H2 db is currently not supported, see https://github.com/diversicon-kb/diversicon/issues/11");
        } else {
            mem = "mem";
        }

        DBConfig ret = h2MakeDefaultCommonDbConfig();

        ret.setJdbc_url("jdbc:h2:" + mem + ":" + dbName + ";DB_CLOSE_DELAY=-1");

        return ret;
    }

    /**
     * @since 0.1.0
     */
    private static DBConfig h2MakeDefaultCommonDbConfig() {

        DBConfig ret = new DBConfig();
        ret.setDb_vendor("de.tudarmstadt.ukp.lmf.hibernate.UBYH2Dialect");
        ret.setJdbc_driver_class("org.h2.Driver");
        ret.setUser(DEFAULT_USER); // same as UBY
        ret.setPassword(DEFAULT_PASSWORD); // same as UBY
        return ret;
    }

    /**
     * @deprecated TODO in progress
     * @since 0.1.0
     */
    public static void turnH2InsertionModeOn(DBConfig dbConfig) {

        /**
         * from
         * http://www.h2database.com/html/performance.html#fast_import
         */
        String saveVars = ""
                + "  SET @DIV_SAVED_LOG @LOG;"
                + "  SET @DIV_SAVED_CACHE_SIZE @CACHE_SIZE;"
                + "  SET @DIV_SAVED_LOCK_MODE @LOCK_MODE;"
                + "  SET @DIV_SAVED_UNDO_LOG @UNDO_LOG;";

        String setFastOptions = "\n"
                + "  SET LOG 0;"
                + "  SET CACHE_SIZE 65536;"
                + "  SET LOCK_MODE 0;"
                + "  SET UNDO_LOG 0;";

        Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;

        try {
            // todo need to improve connection with dbConfig params

            conn = DriverManager.getConnection(
                    dbConfig.getJdbc_url(),
                    dbConfig.getUser(),
                    dbConfig.getPassword());

            stat = conn.createStatement();
            stat.execute(saveVars + setFastOptions);
        } catch (SQLException ex) {
            throw new DivIoException("Error while turning h2 insertion mode on !", ex);
        }
        throw new UnsupportedOperationException("Developer forgot to implement method!");
    }

    /**
     * @deprecated // TODO in progress
     * @since 0.1.0
     */
    public static void h2TurnInsertionModOff() {
        String restoreSavedVars = ""
                + "  SET LOG @DIV_SAVED_LOG;"
                + "  SET CACHE_SIZE @DIV_SAVED_CACHE_SIZE;"
                + "  SET LOCK_MODE @DIV_SAVED_LOCK_MODE;"
                + "  SET UNDO_LOG @DIV_SAVED_UNDO_LOG;";
        throw new UnsupportedOperationException("Developer forgot to implement method!");

    }

    /**
     * Restores an h2 database from a sql dump
     * (possibly compressed in one of {@link #SUPPORTED_COMPRESSION_FORMATS}).
     * {@code dbConfig} MUST point to a non-existing database, otherwise
     * behaviour is unspecified.
     *
     * @param dumpUrl
     *            For Wordnet 3.1 packaged dump, you can use
     *            {@link eu.kidf.diversicon.data.DivWn31#WORDNET_DIV_SQL_RESOURCE_URI}
     * @throws DivIoException
     *             if an IO error occurs
     * 
     * @since 0.1.0
     */
    public static void h2RestoreSql(String dumpUrl, DBConfig dbConfig) {
        Internals.checkNotBlank(dumpUrl, "invalid sql/archive resource path!");
        checkH2Db(dbConfig);

        Date start = new Date();

        LOG.info("Restoring database " + dbConfig.getJdbc_url() + " (may require a long time to perform) ...");
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
             * Made some tests, performance gain seems < 4 s
             */
            String saveVars = ""
                    + "  SET @DIV_SAVED_LOG @LOG;"
                    + "  SET @DIV_SAVED_CACHE_SIZE @CACHE_SIZE;"
                    + "  SET @DIV_SAVED_LOCK_MODE @LOCK_MODE;"
                    + "  SET @DIV_SAVED_UNDO_LOG @UNDO_LOG;";

            String setFastOptions = ""
                    + "  SET LOG 0;"
                    + "  SET CACHE_SIZE 65536;"
                    + "  SET LOCK_MODE 0;"
                    + "  SET UNDO_LOG 0;";

            String restoreSavedVars = ""
                    + "  SET LOG @DIV_SAVED_LOG;"
                    + "  SET CACHE_SIZE @DIV_SAVED_CACHE_SIZE;"
                    + "  SET LOCK_MODE @DIV_SAVED_LOCK_MODE;"
                    + "  SET UNDO_LOG @DIV_SAVED_UNDO_LOG;";

            // todo need to improve connection with dbConfig params

            conn = getH2Connection(dbConfig);

            stat = conn.createStatement();
            stat.execute(saveVars);
            stat.execute(setFastOptions);
            RunScript.execute(conn, new InputStreamReader(extractedStream.stream()));
            stat.execute(restoreSavedVars);
            conn.commit();

            LOG.info("Done restoring database " + dbConfig.getJdbc_url());
            LOG.info("Elapsed time: " + Internals.formatInterval(start, new Date()));

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
     * Executes sql statement on H2 dbs, handy for special
     * commands like "SHUTDOWN COMPACT"
     * 
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    public static void h2Execute(String sql, DBConfig dbConfig) {       

        Connection conn = getH2Connection(dbConfig);

        try {
            RunScript.execute(conn, new StringReader(sql));
        } catch (SQLException e) {
            throw new DivIoException(e);
        }
    }

    /**
     * 
     * Restores a packaged H2 db to file system in user's home under
     * {@link #CACHE_PATH}. The database is intended
     * to be accessed only in read-only mode and if
     * already present no fetch is performed. The database may be fetched from
     * the
     * internet or directly taken from a jar if on the classpath.
     *
     * @param id
     *            the worldwide unique identifier for the resource, in a format
     *            like {@link eu.kidf.diversicon.data.DivWn31#NAME}
     * @param version
     *            the version of the resource, in X.Y.Z-SOMETHING format a la
     *            Maven.
     * @param cacheRoot
     *            the path to the root of the cache.
     * 
     * @return The db configuration to access the DB in read-only mode.
     * 
     * @since 0.1.0
     * 
     */
    // todo should throw if db is already accessed in non readonly mode
    public static DBConfig fetchH2Db(File cacheRoot, String id, String version) {
        checkNotNull(cacheRoot, "Invalid resource id!");
        checkNotBlank(id, "Invalid resource id!");
        checkNotBlank(id, "Invalid version!");

        checkArgument(DivWn31.NAME.equals(id), "Currently only supported id is "
                + DivWn31.NAME + ", found instead " + id + "  !");
        checkArgument(DivWn31.of()
                             .getVersion()
                             .replace("-SNAPSHOT", "")
                             .equals(version.replace("-SNAPSHOT", "")),
                "Currently only supported version is " + DivWn31.of()
                                                                .getVersion()
                        + ", found instead " + version + "  !");

        String filepath = getCachedH2DbDir(cacheRoot, id, version).getAbsolutePath() + File.separator + id;

        if (!new File(filepath + ".h2.db").exists()) {
            restoreH2Db(DivWn31.of()
                               .getH2DbUri(),
                    filepath);
        }
        return h2MakeDefaultFileDbConfig(filepath, true);
    }

    /**
     * @since 0.1.0
     */
    public static File getCachedH2DbDir(File cacheRoot, String id, String version) {
        checkNotBlank(id, "Invalid id!");
        checkNotBlank(version, "Invalid version!");
        return new File(cacheRoot, File.separator + id + File.separator + version);
    }

    /**
     * Restores an h2 database from an h2 db dump
     * (possibly compressed in one of {@link #SUPPORTED_COMPRESSION_FORMATS}).
     *
     * @param dumpUrl
     *            For Wordnet 3.1 packaged dump, you can use
     *            {@link eu.kidf.diversicon.data.DivWn31#WORDNET_DIV_SQL_RESOURCE_URI}
     * @param targetPath
     *            the target path where to restore the db, ending with the db
     *            name. Must NOT end with .h2.db
     * 
     * @throws DivIoException
     *             if an IO error occurs
     * 
     * @since 0.1.0
     */
    public static void restoreH2Db(String dumpUrl, String targetPath) {

        Internals.checkNotBlank(dumpUrl, "invalid h2 db dump!");
        Internals.checkNotBlank(targetPath, "invalid h2 db target path!");

        if (targetPath.endsWith(".db")) {
            throw new DivIoException("Target path must NOT end with '.h2.db' ! Found instead " + targetPath);
        }

        File target = new File(targetPath + ".h2.db");

        if (target.exists()) {
            throw new DivIoException("Target path already exists: " + target.getAbsolutePath() + "  !");
        }

        Date start = new Date();

        LOG.info("Restoring database:   " + dumpUrl);
        LOG.info("                to:   " + target.getAbsolutePath() + "  ...");

        ExtractedStream extractedStream = Internals.readData(dumpUrl, true);

        try {
            FileUtils.copyInputStreamToFile(extractedStream.stream(), target);
        } catch (IOException e) {
            throw new DivIoException("Something went wrong!", e);
        }

        LOG.info("Database created in " + Internals.formatInterval(start, new Date()));

    }

    /**
     * @since 0.1.0
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
     * 
     * @since 0.1.0
     */
    public static boolean isEmpty(DBConfig dbConfig) {
        checkH2Db(dbConfig);

        Connection conn = null;
        try {

            conn = getH2Connection(dbConfig);
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' ");
            return !rs.next();

        } catch (SQLException ex) {
            throw new DivIoException("Something went wrong!", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.error("Couldn't close connection, db config is " + toString(dbConfig, false), e);
                }
            }

        }
    }

    /**
     * Returns a java.sql.Connection to an H2 database from a UBY DBConfig.
     * 
     * @throws DivIoConnection
     * 
     * @since 0.1.0
     */
    public static Connection getH2Connection(DBConfig dbConfig) {

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

    /**
     * @since 0.1.0
     */
    public static boolean isH2Db(DBConfig dbConfig) {
        checkNotNull(dbConfig);
        return dbConfig.getJdbc_driver_class()
                       .contains("h2");
    }

    /**
     * @since 0.1.0
     */
    public static boolean isH2FileDb(DBConfig dbConfig) {
        return isH2Db(dbConfig) && dbConfig.getJdbc_driver_class()
                                           .contains(":file:");
    }

    /**
     * Checks provided {@code dbConfig} is for an H2 database.
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static void checkH2Db(DBConfig dbConfig) {
        checkNotNull(dbConfig);
        if (!isH2Db(dbConfig)) {
            throw new IllegalArgumentException("Only H2 database is supported for now! Found instead "
                    + Diversicons.toString(dbConfig, false));
        }
    }

    /**
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static String extractH2DbFilepath(DBConfig dbConfig) {
        if (!isH2FileDb(dbConfig)) {
            throw new IllegalArgumentException(
                    "DBConfig doesn't appear to be an H2 File db: " + toString(dbConfig, false));
        }

        String url = dbConfig.getJdbc_url();

        String filePrefix = "file:";

        int i = url.indexOf(filePrefix);
        if (i == -1) {
            throw new IllegalArgumentException(
                    "DBConfig doesn't appear to be an H2 File db: " + toString(dbConfig, false));
        }

        int j = url.indexOf(";", i + 1);

        String filePath;
        if (j == -1) {
            filePath = url.substring(i + filePrefix.length());
        } else {
            filePath = url.substring(i + filePrefix.length(), j);
        }

        checkNotEmpty(filePath, "Found an invalid filepath!");
        return filePath;
    }

    /**
     * Returns true if provided configuration refers to a database which could
     * work
     * with Diversicon. In order to work two conditions need to be met:
     * 
     * 1) Db driver must be present in classpath
     * 2) Database must not be blacklisted
     * 
     * @throws ClassNotFoundExc
     * 
     * @since 0.1.0
     */
    // we should ban dbs that don't support recursive queries (i.e. mysql)
    // maybe Hibernate can tell us...
    public static boolean isDatabaseSupported(String driver) {
        checkNotNull(driver);

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            LOG.debug("Couldn't find database driver class: " + driver, ex);
            return false;
        }

        return driver.equals("org.h2.Driver");

    }

    /**
     * Returns the lowercased shorthand to identify a database (like 'h2')
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    // TODO Spring DatabaseType seems a good source for identifiers
    // https://github.com/spring-projects/spring-batch/blob/master/spring-batch-infrastructure/src/main/java/org/springframework/batch/support/DatabaseType.java
    // also see Jdbc DatabaseMetadata: http://stackoverflow.com/a/254220
    public static String getDatabaseId(DBConfig dbConfig) {
        checkH2Db(dbConfig);
        return H2_IDENTIFIER;
    }

    /**
     * 
     * @throws UnsupportedOperationException
     * 
     * @since 0.1.0
     */
    public static void checkSupportedDatabase(String databaseDriver) {
        checkNotNull(databaseDriver);
        if (!isDatabaseSupported(databaseDriver)) {
            throw new UnsupportedOperationException("Unsupported database!");
        }
    }

    /**
     * 
     * Extracts the namespace prefix from provided {@code id}
     * 
     * @param id
     *            an identifier in {@link #ID_PATTERN} format
     * @throws IllegalArgumentException
     *             when id has no valid prefix
     * 
     * @since 0.1.0
     */
    public static String namespacePrefixFromId(String id) {
        checkNotEmpty(id, "Invalid id!");

        int i = id.indexOf(NAMESPACE_SEPARATOR);
        if (i == -1) {
            throw new IllegalArgumentException(
                    "Tried to extract prefix but couldn't find separator '" + Diversicons.NAMESPACE_SEPARATOR
                            + "' in provided id: " + id);
        }

        String ret = id.substring(0, i);

        if (NAMESPACE_PREFIX_PATTERN.matcher(ret)
                                    .matches()) {
            return ret;
        } else {
            throw new IllegalArgumentException(
                    "Provided id: " + id + " has invalid prefix! It should match " + NAMESPACE_PREFIX_PATTERN);
        }

    }

    /**
     * 
     * @throws IllegalArgumentException
     * @since 0.1.0
     */
    public static Map<String, String> checkNamespaces(@Nullable Map<String, String> namespaces) {
        checkNotNull(namespaces);

        for (String prefix : namespaces.keySet()) {
            checkNotNull(prefix);
            if (!NAMESPACE_PREFIX_PATTERN.matcher(prefix)
                                         .matches()) {
                throw new IllegalArgumentException(
                        "Invalid prefix '" + prefix + "', it must match " + NAMESPACE_PREFIX_PATTERN.toString());
            }

            checkNotBlank(namespaces.get(prefix), "Invalid namespace url!");
        }
        return namespaces;
    }

    /**
     * 
     * See {@link #validateXml(File, XmlValidationConfig)}
     * 
     * @throws InvalidXmlException
     * 
     * @since 0.1.0
     * 
     */
    public static void validateXml(File xmlFile, Logger log) {
        validateXml(xmlFile, XmlValidationConfig.of(log));
    }

    /**
     * Gives back an XML resource. If fetch can't be performed and resource is
     * an the classpath, it will be taken from there, otherwise an exception is
     * thrown.
     * 
     * 
     * 
     * @param namespaceURI
     *            The namespace of the resource being resolved,
     *            e.g. the target namespace of the XML Schema [
     *            <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>
     *            XML Schema Part 1</a>]
     *            when resolving XML Schema resources.
     * @param systemId
     *            The system identifier, a URI reference [
     *            <a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC
     *            2396</a>], of the
     *            external resource being referenced, or <code>null</code> if no
     *            system identifier was supplied.
     * @return A <code>LSInput</code> object describing the new input
     *         source, or <code>null</code> to request that the parser open a
     *         regular URI connection to the resource.
     * 
     * @throws DivNotFoundException
     * 
     * @since 0.1.0
     */
    // TODO in theory there are many other parameters we should take into
    // consideration
    @Nullable
    private static LSInput resolveXmlResource(@Nullable String namespaceUri, @Nullable String systemId) {

        if (namespaceUri == null && systemId == null) {
            return null;
        }

        DOMInputImpl ret = new DOMInputImpl();
        ret.setSystemId(systemId);
        ret.setEncoding("UTF-8");

        try {
            ret.setByteStream(Internals.readData(systemId)
                                       .stream());
            return ret;
        } catch (Exception ex) {
            if ((Diversicons.SCHEMA_1_NAMESPACE.equals(namespaceUri)
                    || namespaceUri == null)
                    &&
                    (systemId != null
                            && systemId.contains(Diversicons.SCHEMA_FILENAME))) {
                String classpathUrl = Diversicons.SCHEMA_1_0_CLASSPATH_URL;

                LOG.debug("Defaulting to " + classpathUrl + " for unfetchable resource "
                        + " \n     systemId=" + systemId
                        + " \n namespaceUrl=" + namespaceUri);

                ret.setByteStream(Internals.readData(classpathUrl)
                                           .stream());
                return ret;
            }
        }
        throw new DivNotFoundException(
                "Couldn't find xml resource for namespace=" + namespaceUri + " and systemId=" + systemId);
    }

    /**
     * Validates an xml file. You can pass schema overrides in the provided
     * config.
     * 
     * @throws DivException
     * @throws InvalidXmlException
     * 
     * @since 0.1.0
     */
    public static void validateXml(File xmlFile, XmlValidationConfig config) {

        checkNotNull(xmlFile);
        checkNotNull(config);

        // if editor can't find the Constants.W3C_XML_SCHEMA11_NS_URI constant
        // probably default xerces is being used instead of the one supporting
        // schema 1.1

        SchemaFactory factory = SchemaFactory.newInstance(Constants.W3C_XML_SCHEMA10_NS_URI);

        Schema schema;
        try {
            if (Internals.isBlank(config.getXsdUrl())) {
                schema = factory.newSchema();
            } else {
                File xsd = Internals.readData(config.getXsdUrl())
                                    .toTempFile();
                schema = factory.newSchema(xsd);
                LOG.debug("Validating against schema: " + config.getXsdUrl());
            }
        } catch (SAXException e) {
            throw new DivException(e);
        }

        Source source = new StreamSource(xmlFile);
        DivXmlHandler errorHandler = new DivXmlHandler(config, source.getSystemId());

        Validator validator = schema.newValidator();

        LSResourceResolver lsResResolver = new LSResourceResolver() {

            @Override
            public LSInput resolveResource(
                    String type,
                    String namespaceURI,
                    String publicId,
                    String systemId,
                    String baseURI) {

                LOG.debug("Received XML resource request for"
                        + "\n type         = " + type
                        + "\n namespaceURI = " + namespaceURI
                        + "\n publicId     = " + publicId
                        + "\n systemId     = " + systemId
                        + "\n baseURI      = " + baseURI);

                return resolveXmlResource(namespaceURI, systemId);
            }
        };

        validator.setResourceResolver(lsResResolver);

        validator.setErrorHandler(errorHandler);

        try {
            validator.validate(source);
        } catch (SAXException | IOException e) {
            throw new InvalidXmlException(errorHandler, "Fatal error while validating " + xmlFile.getAbsolutePath(), e);
        }

        DivXmlValidator divXmlValidator = new DivXmlValidator(new LexResPackage(), errorHandler);

        validateXmlJavaStep(xmlFile, errorHandler, divXmlValidator, lsResResolver);
        // need to steps!
        validateXmlJavaStep(xmlFile, errorHandler, divXmlValidator, lsResResolver);

        if (errorHandler.invalid()) {
            config.getLog()
                  .error("Invalid xml! " + errorHandler.summary() + " in " + xmlFile.getAbsolutePath());
            throw new InvalidXmlException(errorHandler,
                    "Invalid xml! " + errorHandler.summary() + " in " + xmlFile.getAbsolutePath()
                            + "\n" + errorHandler.firstIssueAsString());
        }

    }

    /**
     * Performs validation with custom Java code. Needed because current Xerces
     * Xml Schema 1.1 assert implemention has problems, see
     * https://github.com/diversicon-kb/diversicon/issues/21
     * 
     * @param file
     * @param errorHandler
     * 
     * @since 0.1.0
     */
    // TODO probably we could pass less parameters
    public static void validateXmlJavaStep(
            File file,
            DivXmlHandler errorHandler,
            DivXmlValidator divXmlValidator,
            LSResourceResolver resRes) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
            parser.getXMLReader()
                  .setErrorHandler(errorHandler);
            DivXmlValidator handler = divXmlValidator;

            InputSource is = new InputSource(new FileInputStream(file));

            parser.parse(is, handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new DivException(e);
        }

    }

    /**
     * Reads metadata about a given resource.
     * 
     * @param path
     *            A path to a file as specified by
     *            {@link Internals#readData(String, boolean)
     * 
     * @since 0.1.0
     */
    public static LexResPackage readPackageFromLexRes(String lexResUrl) {

        LexResPackage ret = new LexResPackage();

        SAXReader reader = new SAXReader(false);

        ExtractedStream es = Internals.readData(lexResUrl, true);
        DivXmlExtractor handler = new DivXmlExtractor(ret);
        reader.setDefaultHandler(handler);
        try {
            reader.read(es.stream());
        } catch (DocumentException e) {

            if (e.getMessage()
                 .contains(DivXmlExtractor.FOUND)) {
                return ret;
            }
        }
        throw new DivNotFoundException("Couldn't find required tags in "
                + lexResUrl + "  !");

    }

    /**
     * Creates an xml file out of the provided lexical resource. Written
     * lexical resource will include info from provided {@code lexResPackage}.
     * 
     * 
     * @since 0.1.0
     */
    public static File writeLexResToXml(
            LexicalResource lexRes,
            LexResPackage lexResPackage,
            File xmlFile) {

        checkNotNull(lexRes);

        Internals.checkLexResPackage(lexResPackage, lexRes);

        DivXmlWriter writer;
        try {
            writer = new DivXmlWriter(new FileOutputStream(
                    xmlFile),
                    null,
                    lexResPackage); // todo check if setting dtd means something

            writer.writeElement(lexRes);
            writer.writeEndDocument();

        } catch (FileNotFoundException | SAXException ex) {
            throw new DivException("Error while writing lexical resource to XML: " + xmlFile.getAbsolutePath(), ex);
        }

        return xmlFile;

    }

    /**
     * 
     * Checks a prefix is valid, throwing IllegalArgumentException otherwise
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static String checkPrefix(String prefix) {
        if (!NAMESPACE_PREFIX_PATTERN.matcher(prefix)
                                     .matches()) {
            throw new IllegalArgumentException("Invalid prefix!");
        }
        return prefix;
    }

    /**
     * 
     * Checks an id is valid, throwing IllegalArgumentException otherwise
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static void checkId(String id, @Nullable String prependedMsg) {
        if (!ID_PATTERN.matcher(id)
                       .matches()) {
            throw new IllegalArgumentException(String.valueOf(prependedMsg)
                    + " '" + id + "' doesn't match Diversicon ID pattern. "
                    + NAMESPACE_ID_PATTERN_DESCRIPTION.toString());
        }
    }

    /**
     * Executes an XQuery script on {@code inXml} and writes output to
     * {@code outXml}
     *
     * Currently the processing occurs completely in-memory.
     *
     * @throws DivException
     * 
     * @since 0.1.0
     */
    // todo make it work with large xmls
    public static void transformXml(
            String xquery,
            File inXml,
            File outXml) {

        checkNotBlank(xquery, "Invalid query!");
        checkNotNull(inXml);
        checkNotNull(outXml);
        checkArgument(!inXml.equals(outXml),
                "Target file must be different from input file, instead they point both to %s",
                inXml.getAbsolutePath());

        Context context = new Context();

        // Create a query processor
        try (QueryProcessor proc = new QueryProcessor(xquery, context)) {

            proc.bind(XQUERY_IN_FILE_VAR, inXml.getAbsolutePath());

            // Store the pointer to the result in an iterator:
            Iter iter;
            try {
                iter = proc.iter();
            } catch (QueryException ex) {
                throw new DivException(ex);
            }

            try (FileOutputStream ostream = new FileOutputStream(outXml);
                    Serializer ser = proc.getSerializer(ostream)) {

                // Iterate through all items and serialize contents
                for (Item item; (item = iter.next()) != null;) {

                    ser.serialize(item);
                }
            }
        } catch (IOException | QueryException ex) {
            throw new DivException(ex);
        }

    }

    /**
     * 
     * @param inXml
     * @param outXml
     * 
     * @since 0.1.0
     */
    public void transformOwa(File inXml, File outXml, int logLimit) {

        checkNotNull(inXml);
        checkNotNull(outXml);

        SAXReader reader = new SAXReader(false);

        DivXmlHandler handler = new DivXmlHandler(
                XmlValidationConfig.builder()
                                   .setLog(LOG)
                                   .setLogLimit(logLimit)
                                   .build(),
                "");
        reader.setErrorHandler(handler);
        Document document;

        try {
            document = reader.read(inXml);
        } catch (DocumentException ex) {
            throw new InvalidXmlException(handler, "Something went wrong!", ex);
        }

        String stylesheet = "src/main/resources/owa-to-uby.xslt";

        // load the transformer using JAXP
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        ErrorListener d;
        try {
            transformer = factory.newTransformer(
                    new StreamSource(stylesheet));
        } catch (TransformerConfigurationException ex) {
            throw new InvalidXmlException(handler, "Something went wrong!", ex);
        }

        transformer.setErrorListener(handler);

        // now lets style the given document
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();

        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new InvalidXmlException(handler, "Something went wrong!", ex);
        }

        Document transformedDoc = result.getDocument();

        OutputFormat format = OutputFormat.createPrettyPrint();
        /*
         * FileWriter fw = new FileWriter(outXml);
         * XMLWriter writer = new XMLWriter(fw, format);
         * try {
         * writer.write(transformedDoc);
         * } catch (IOException e) {
         * 
         * throw new RuntimeException("Something went wrong!", e);
         * }
         * 
         * //LOG.debug("\n" + sw.toString());
         */

        throw new UnsupportedOperationException("TODO IMPLEMENT ME!");
    }

}
