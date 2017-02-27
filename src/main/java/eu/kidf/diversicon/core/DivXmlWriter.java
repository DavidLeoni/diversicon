package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.internal.Internals;

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
            LexResPackage pack) throws SAXException {
        super(outputStream, dtdPath);        
        this.lexResPackage = checkNotNull(pack); 
        
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
     * For an explanation, see {@link Internals#prepareXmlElement(Object, boolean, LexResPackage, de.tudarmstadt.ukp.lmf.transform.UBYLMFClassMetadata, AttributesImpl, List)
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
