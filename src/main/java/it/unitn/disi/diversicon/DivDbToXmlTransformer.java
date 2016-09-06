package it.unitn.disi.diversicon;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.DBToXMLTransformer;
import it.unitn.disi.diversicon.internal.Internals;

class DivDbToXmlTransformer extends DBToXMLTransformer {

    private Map<String, String> namespaces;

    /**
     * See {@link DBToXMLTransformer#DBToXMLTransformer(DBConfig, OutputStream, String)
     *      super constructor}
     *      
     * @param namespaces Diversicon namespaces
     * 
     * @since 0.1.0
     */
    public DivDbToXmlTransformer(DBConfig dbConfig, 
            OutputStream outputStream, 
            @Nullable String dtdPath,
            Map<String, String> namespaces) throws SAXException {
        super(dbConfig, outputStream, dtdPath);
        this.namespaces = Diversicons.checkNamespaces(namespaces);
    }
       
    /**
     * 
     * {@inheritDoc}
     * 
     * <p>
     * <strong>
     * DIVERSICON NOTE: this function must be <i>the same</i> as 
     * {@link DivXmlWriter#doWriteElement(Object, boolean)}<br/>
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

        AttributesImpl atts = new AttributesImpl();
        List<Object> children = new ArrayList<>();        
        String elementName = Internals.prepareXmlElement(lmfObject,
                closeTag,
                namespaces,
                getClassMetadata(lmfObject.getClass()),
                atts, 
                children);
        
        th.startElement("", "", elementName, atts);
        for (Object child : children) {
            doWriteElement(child, true);
        }
        if (closeTag) 
            th.endElement("", "", elementName);
    }


}
