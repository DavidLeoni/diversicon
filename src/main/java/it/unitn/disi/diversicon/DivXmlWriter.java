package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.transform.OutputKeys;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;
import de.tudarmstadt.ukp.lmf.transform.UBYXMLTransformer;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * 
 * Needed because {@link LMFXmlWriter} writes {@code <DivSynsetRelation>} tags
 * instead of just {@code <SynsetRelation>}
 * 
 * @since 0.1.0
 */
class DivXmlWriter extends LMFXmlWriter {

    private LexResPackage lexResPackage;


    /**
     * Constructs a LMFXmlWriter, XML will be saved to OutputStream out.   
     * 
     * @since 0.1.0
     */
    public DivXmlWriter(OutputStream outputStream,
            @Nullable String dtdPath,
            LexResPackage lexResPackage) throws SAXException {
        super(outputStream, dtdPath);        
        this.lexResPackage = checkNotNull(lexResPackage); 
        
    }

    /**
     * 
     * Constructs a LMFXmlWriter, XML will be saved to file in outputPath
     * 
     * @param outputPath
     * @param dtdPath
     *            Path of the dtd-File
     * @throws FileNotFoundException
     *             if the writer can not to the specified outputPath
     * @since 0.1.0
     */
    public DivXmlWriter(String outputPath, String dtdPath) throws FileNotFoundException, SAXException {
        super(outputPath, dtdPath);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * <p>
     * <strong>
     * DIVERSICON NOTE: this function MUST be <i>the same</i> as 
     * {@link DivDbToXmlTransformer#doWriteElement(Object, boolean)}<br/>
     * 
     * For an explanation, see {@link Internals#prepareXmlElement(Object, boolean, Map, de.tudarmstadt.ukp.lmf.transform.UBYLMFClassMetadata, AttributesImpl, List)
     * Internals#prepareXmlElement} 
     * 
     * <strong>
     * </p>
     * 
     * @since 0.1.0
     * 
     */
    @Override
    protected void doWriteElement(Object lmfObject, boolean closeTag) throws SAXException {
        /*  
        th.getTransformer().setParameter("http://xml.org/sax/features/namespaces"
                , false);
        th.getTransformer().setParameter("http://xml.org/sax/features/namespace-prefixes"
                , false);
        */
        
        AttributesImpl atts = new AttributesImpl();
        List<Object> children = new ArrayList<>();
        
        @Nullable
        String elementName = Internals.prepareXmlElement(lmfObject,
                closeTag,
                lexResPackage,
                getClassMetadata(lmfObject.getClass()),
                atts, 
                children);
        
        if (elementName == null){
            return;
        }
        
        th.startElement("", "", elementName, atts);
        for (Object child : children) {
            doWriteElement(child, true);
        }
        if (closeTag) 
            th.endElement("", "", elementName);
    }

}
