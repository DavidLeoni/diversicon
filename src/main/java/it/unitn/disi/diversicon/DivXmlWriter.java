package it.unitn.disi.diversicon;

import java.io.FileNotFoundException;
import java.io.OutputStream;

import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.lmf.transform.LMFXmlWriter;

/**
 * 
 * Needed because {@link LMFXmlWriter} writes {@code <DivSynsetRelation>} tags instead of just {@code <SynsetRelation>}
 * 
 * @since 0.1.0
 */
public class DivXmlWriter extends LMFXmlWriter {

    public DivXmlWriter(OutputStream outputStream, String dtdPath) throws SAXException {
        super(outputStream, dtdPath);
    }

    public DivXmlWriter(String outputPath, String dtdPath) throws FileNotFoundException, SAXException {
        super(outputPath, dtdPath);    
    }
    
    @Override
    protected void doWriteElement(Object lmfObject, boolean closeTag) throws SAXException{
            
        if (lmfObject.getClass().equals(DivSynsetRelation.class)){
            DivSynsetRelation dsr = (DivSynsetRelation) lmfObject;            
            super.doWriteElement(dsr.toSynsetRelation(), closeTag);
        } else {
            super.doWriteElement(lmfObject, closeTag);
        }
    }
    

}
