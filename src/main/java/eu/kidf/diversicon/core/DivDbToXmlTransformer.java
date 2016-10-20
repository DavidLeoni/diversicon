package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import de.tudarmstadt.ukp.lmf.transform.DBToXMLTransformer;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.internal.Internals;

/**
 * @since 0.1.0
 *
 */
class DivDbToXmlTransformer extends DBToXMLTransformer {

    private LexResPackage lexResPackage;

    /**
     * See {@link DBToXMLTransformer#DBToXMLTransformer(DBConfig, OutputStream, String)
     *      super constructor}
     *      
     * @param lexResPackage Additional info about the lexical resource
     * 
     * @since 0.1.0
     */
    public DivDbToXmlTransformer(Diversicon div, 
            OutputStream outputStream, 
            @Nullable String dtdPath,
            LexResPackage lexResPackage) throws SAXException {
        super(div.getDbConfig(), outputStream, dtdPath);
        dbConfig = div.getDbConfig();
        sessionFactory = div.getSessionFactory();        
        checkNotNull(lexResPackage);
        this.lexResPackage = lexResPackage;
    }
       
    /**
     * 
     * {@inheritDoc}
     * 
     * <p>
     * <strong>
     * DIVERSICON NOTE: this function MUST be <i>the same</i> as 
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
