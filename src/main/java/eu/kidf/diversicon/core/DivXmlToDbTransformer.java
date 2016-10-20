package eu.kidf.diversicon.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import eu.kidf.diversicon.core.exceptions.InterruptedImportException;
import eu.kidf.diversicon.core.internal.Internals;

import java.io.StringReader;
/*from   w  ww.  ja  v a2  s . c  o  m*/
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



/**
 * @since 0.1.0
 */
class DivXmlToDbTransformer extends XMLToDBTransformer {      

    /**
     * @since 0.1.0
     */
    DivXmlToDbTransformer(Diversicon div) {
        super(div.getDbConfig());        
        sessionFactory.close();  // div dirty but needed...       
        sessionFactory = div.getSessionFactory();       
    }      

}
