package eu.kidf.diversicon.core.test;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;
import static eu.kidf.diversicon.core.test.DivTester.tid;
import static eu.kidf.diversicon.core.test.LmfBuilder.lmf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.api.Uby;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.enums.ERelNameSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ERelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.LMFDBUtils;
import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.internal.Internals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests to show Uby corner cases or bugs to report.
 * 
 * @since 0.1.0
 *
 */
public class UbyTest {

    private static final Logger LOG = LoggerFactory.getLogger(UbyTest.class);

    /**
     * 2 verteces and 1 hypernym edge
     * 
     * @since 0.1.0
     */
    public static LexicalResource UBY_GRAPH_1_HYPERNYM = lmf()
                                                              .uby()
                                                              .lexicon()
                                                              .synset()
                                                              .lexicalEntry()
                                                              .synset()
                                                              .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                                              .build();

    private DBConfig dbConfig;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void beforeMethod() {
        dbConfig = DivTester.createNewDbConfig();
    }

    @After
    public void afterMethod() {
        dbConfig = null;
    }

    @Test
    public void testBuilderUby(){
        SynsetRelation sr = UBY_GRAPH_1_HYPERNYM.getLexicons()
                .get(0).getSynsets().get(1).getSynsetRelations().get(0);
                
        assertNotNull(sr);
        assertFalse(sr instanceof DivSynsetRelation);        
    }
    
    /**
     * Uby bug to report. Shows getSynsetById as of Uby 0.7 throws
     * IndexOutOfBoundsException
     * 
     * See https://github.com/diversicon-kb/diversicon/issues/7
     * 
     * @see #testGetSynsetIterator()
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetByNonExistingId() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig);
        Uby uby = new Uby(dbConfig);

        try {
            uby.getSynsetById("666");
            Assert.fail("Shouldn't arrive here!");
        } catch (IndexOutOfBoundsException ex) {

        }

        uby.getSession()
           .close();
    }

    /**
     * Seems like calling Uby.getSynsetIterator(null).next() always throws
     * NullPointerException:
     * 
     * See https://github.com/diversicon-kb/diversicon/issues/16
     * 
     * @see #testGetSynsetByNonExistingId()
     * @since 0.1.0
     */
    @Test
    public void testGetSynsetIterator() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig);

        LexicalResource lexRes = lmf()
                                      .uby()
                                      .lexicon()
                                      .synset()
                                      .lexicalEntry()
                                      .build();

        importIntoUby(lexRes);

        Uby uby = new Uby(dbConfig);
        try {
            Iterator<Synset> iter = uby.getSynsetIterator(null);
            iter.next();
            Assert.fail("Seems bug was solved!");

        } catch (NullPointerException ex) {

        }

        uby.getSession()
           .close();
    }

    /**
     * Transforms res into an XML and then imports it into UBY
     * 
     * @since 0.1.0
     */
    private File importIntoUby(LexicalResource lexRes) {
        File xml;
        try {
            xml = new File(DivTester.createTestDir()
                                    .toFile(),
                    "test.xml");

            LMFXmlWriter w = new LMFXmlWriter(new FileOutputStream(xml), null);
            w.writeElement(lexRes);
            w.writeEndDocument();

            new XMLToDBTransformer(dbConfig).transform(xml, null);
            return xml;
        } catch (Exception ex) {
            throw new DivException("Error while import file into Uby!", ex);
        }
    }

    /**
     * 
     * Demonstrates you can't import twice a lexicon with the same id with
     * uby 0.7.0 xml transformer, even if you explicitly select an existing
     * lexical resource, see https://github.com/diversicon-kb/diversicon/issues/6
     * 
     * See also {@link DiversiconTest#testImportCantMergeSameLexicon()}
     * 
     * @since 0.1.0
     */
    @Test
    public void testCantMergeSameLexicon() throws FileNotFoundException, IllegalArgumentException, DocumentException {

        LMFDBUtils.createTables(dbConfig);

        File xml = importIntoUby(UBY_GRAPH_1_HYPERNYM);

        try {
            new XMLToDBTransformer(dbConfig).transform(xml, UBY_GRAPH_1_HYPERNYM.getName());
        } catch (ConstraintViolationException ex) {
            assertTrue(ex.getMessage()
                         .contains(
                                 "Unique index or primary key violation: \"PRIMARY_KEY_2 ON PUBLIC.LEXICON(LEXICONID)"));
        }

    }

    /**
     * 
     * Shows that if you import twice a synset, the second one gets skipped,
     * showing on error to {@code System.err}. Notice the error is generated by
     * Hibernate,
     * *NOT* by XML validation, which is <b><a href=
     * "https://github.com/dkpro/dkpro-uby/blob/de.tudarmstadt.ukp.uby-0.7.0/de.tudarmstadt.ukp.uby.persistence.transform-asl/src/main/java/de/tudarmstadt/ukp/lmf/transform/XMLToDBTransformer.java#L88"
     * target="_blank">never performed </a> </b>
     * 
     * @since 0.1.0
     */
    @Test
    public void testSkipDuplicateSynsetId()
            throws IllegalArgumentException, DocumentException, IOException, SAXException {

        LMFDBUtils.createTables(dbConfig);

        LexicalResource lexRes = lmf().uby()
                                      .lexicon()
                                      .synset(1)
                                      .lexicalEntry()
                                      .synset(1)
                                      .build();

        importIntoUby(lexRes);

    }

    /**
     * Tries to import xml with myAttr=666
     * 
     * @since 0.1.0
     */
    @Test
    public void testInvalidXml() throws IOException, IllegalArgumentException, DocumentException {

        LMFDBUtils.createTables(dbConfig);

        final String INVALID = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<LexicalResource name=\"lexical resource 1\" myAttr=\"666\">   \n"
                + "     <Lexicon id=\"lexicon 1\">       \n"
                + "         <Synset id=\"synset 1\"/>    \n"
                + "     </Lexicon>                       \n"
                + "</LexicalResource>";

        File xml = new File(DivTester.createTestDir()
                                     .toFile()
                + "test.xml");
        FileUtils.writeStringToFile(xml, INVALID, "UTF-8");
        new XMLToDBTransformer(dbConfig).transform(xml, null);

    }

    /**
     * @since 0.1.0
     */
    @Test
    public void testOwaToUby() throws DocumentException, TransformerException, IOException {

        SAXReader reader = new SAXReader(false);
        Document document = reader.read("src/test/resources/owa-test.xml");
        String stylesheet = "src/main/resources/owa-to-uby.xslt";

        // load the transformer using JAXP
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(
                new StreamSource(stylesheet));

        // now lets style the given document
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);

        // return the transformed document
        Document transformedDoc = result.getDocument();

        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter sw = new StringWriter();
        XMLWriter writer = new XMLWriter(sw, format);
        writer.write(transformedDoc);

        LOG.debug("\n" + sw.toString());

    }

    /**
     * Shows you can import a synset relation pointing to a non-existing synset,
     * and
     * such synset is not automatically created in the db. Still when
     * in the API there is Synset to return, Hibernate creates a
     * Java object on the fly.
     *
     * @since 0.1.0
     */
    @Test
    public void testSynsetRelationNonExistingTarget() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig); 

        LexicalResource lexRes = lmf().uby()
                                      .lexicon()
                                      .synset()
                                      .lexicalEntry()
                                      .build();

        Synset syn = lexRes.getLexicons()
                           .get(0)
                           .getSynsets()
                           .get(0);

        SynsetRelation sr = new SynsetRelation();
        sr.setRelName("a");
        sr.setRelType(ERelTypeSemantics.label);
        sr.setSource(syn);

        Synset syn2 = new Synset();
        syn2.setId(tid("synset 2"));

        sr.setTarget(syn2);

        syn.setSynsetRelations(Internals.newArrayList(sr));

        importIntoUby(lexRes);

        Uby uby = new Uby(dbConfig);

        Synset retSyn1 = uby.getSynsetById(tid("synset-1"));

        // second synset is not created !
        assertEquals(1, uby.getLexicons()
                           .get(0)
                           .getSynsets()
                           .size());

        List<SynsetRelation> synRels = retSyn1.getSynsetRelations();
        checkNotNull(synRels);
        assertEquals(1, synRels.size());
        SynsetRelation retSynRel = synRels.get(0);
        Synset retSyn2 = retSynRel.getTarget();
        // shows Hibernate here does create the synset object
        assertEquals(tid("synset 2"), retSyn2.getId());

    }

    /**
     * Shows in UBY synset relations are not removed on cascade if target synset
     * is deleted.
     *
     * @since 0.1.0
     */
    @Test
    public void testSynsetConstraints() throws FileNotFoundException {

        LMFDBUtils.createTables(dbConfig); 

        LexicalResource lexRes = lmf()
                                      .uby()
                                      .lexicon()
                                      .synset()
                                      .lexicalEntry()
                                      .synset()
                                      .synsetRelation(ERelNameSemantics.HYPERNYM, 1)
                                      .build();

        importIntoUby(lexRes);

        Uby uby = new Uby(dbConfig);

        Synset syn = (Synset) uby.getSession()
                                 .get(Synset.class, tid("synset-1"));

        uby.getSession()
           .delete(syn);
        uby.getSession()
           .flush();

        List<Synset> synsets = uby.getLexiconById(tid("lexicon-1"))
                                  .getSynsets();
        assertEquals(1, synsets.size());
        Synset syn2 = synsets.get(0);
        assertEquals(1, syn2.getSynsetRelations()
                            .size());

    }

    /**
     * Test for https://github.com/diversicon-kb/diversicon/issues/17
     * 
     * MetaData can be an element of a LexicalResource. In UBY 0.7.0 DTD,
     * MetaData has lexicalResourceId as required field, but it's not necessary,
     * importer should automatically determine it (although currently doesn't).
     * 
     * @since 0.1.0
     */
    @Test
    public void testMetadataResource() throws IOException {

        LMFDBUtils.createTables(dbConfig);

        LexicalResource lr = LmfBuilder.lmf()
                                       .uby()
                                       .build();
        MetaData md = new MetaData();
        md.setId(tid("metadata-1"));
        lr.setMetaData(Internals.newArrayList(md));

        File xml = importIntoUby(lr);

        LOG.debug("xml = \n" + FileUtils.readFileToString(xml));

        Uby uby = new Uby(dbConfig);

        LexicalResource lr2 = uby.getLexicalResource(lr.getName());

        MetaData md2 = lr2.getMetaData()
                          .get(0);

        assertEquals(tid("metadata-1"), md2.getId());
        assertEquals(null, md2.getLexicalResource());

    }

}
