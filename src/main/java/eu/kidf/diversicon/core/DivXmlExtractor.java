package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.Namespace;

import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.exceptions.DivException;

/**
 * Extracts stuff from a LexicalResource XML structured according to Diversicon
 * schema
 * 
 * @since 0.1.0
 */
class DivXmlExtractor implements ElementHandler {
    

    /**
     * String contained in exception messages when the handler signals to stop
     * the parse.
     * 
     * @since 0.1.0
     */
    public static final String FOUND = "** FOUND LEXICAL RESOURCE **";

    private LexResPackage divRes;
    private boolean foundLexRes = false;

    /**
     * @since 0.1.0
     */
    public DivXmlExtractor(LexResPackage divRes) {        
        checkNotNull(divRes);
        this.divRes = divRes;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */    
    @Override
    public void onStart(ElementPath elementPath) {
        Element el = elementPath.getCurrent();
        String elName = el.getName();

        if ("LexicalResource".equals(elName)) {

            foundLexRes = true;

            String candName = el.attributeValue("name");
            if (candName != null) {
                divRes.setName(candName);
            }

            String candPrefix = el.attributeValue("prefix");
            if (candPrefix != null) {
                divRes.setPrefix(candPrefix);
            }

            for (Object obj : el.content()) {
                if (obj instanceof Namespace) {
                    Namespace ns = (Namespace) obj;
                    if (!ns.getPrefix().isEmpty()){ // happens for default namespace declaration xmlns="blabla"
                        divRes.putNamespace(ns.getPrefix(), ns.getURI());    
                    }
                    
                }

            }

        } else if ("GlobalInformation".equals(elName)) {

            if (!foundLexRes) {
                throw new DivException("Found GlobalInformation before LexicalResource!");
            }

            String candLabel = el.attributeValue("label");
            if (candLabel != null) {
                divRes.setLabel(candLabel);
            }

            throw new RuntimeException(FOUND);

        } else {
            if (foundLexRes){
               throw new DivException("Expected GlobalInformation as first child of LexicalResource, found instead: " + elName);
            } else {
                throw new DivException("Couldn't find LexicalResource element!");    
            }
            
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @Override
    public void onEnd(ElementPath elementPath) {
        // nothing to do
    }

}
