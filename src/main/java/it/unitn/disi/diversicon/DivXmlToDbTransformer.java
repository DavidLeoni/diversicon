package it.unitn.disi.diversicon;

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
import it.unitn.disi.diversicon.exceptions.InterruptedImportException;
import it.unitn.disi.diversicon.internal.Internals;


import java.io.StringReader;
/*from   w  ww.  ja  v a2  s . c  o  m*/
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class Main {
  public static void main(String[] argv) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(true);
    SAXParser parser = factory.newSAXParser();
    SampleOfXmlLocator handler = new SampleOfXmlLocator();
    //parser.parse("xmlFileName.xml", handler);
    
    StringReader sr = new StringReader("<folks></folks>");
    InputSource is = new InputSource(sr);
    parser.parse(is, handler);
    
  }
}

// from here http://www.java2s.com/Tutorials/Java/XML/SAX/Output_line_number_for_SAX_parser_event_handler_in_Java.htm
class SampleOfXmlLocator extends DefaultHandler {
  private Locator locator;
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }
  public void startElement(String uri, String localName, String qName, Attributes attrs)
      throws SAXException {
    if (qName.equals("order")) {
      System.out.println("here process element start");
    } else {
      String location = "";
      if (locator != null) {
        location = locator.getSystemId(); // XML-document name;
        location += " line " + locator.getLineNumber();
        location += ", column " + locator.getColumnNumber();
        location += ": ";
      }
      throw new SAXException(location + "Illegal element");
    }
  }
  public static void main(String[] args) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(true);
    SAXParser parser = factory.newSAXParser();
    parser.parse("sample.xml", new SampleOfXmlLocator());
  }
}

/**
 * @since 0.1.0
 */
class DivXmlValidator implements ElementHandler {
        
    private LexResPackage pack;
    private boolean skipNamespaceChecking;
    
    
    
    /**
     * @since 0.1.0
     */
    public DivXmlValidator(boolean skipNamespaceChecking){
        this.pack = new LexResPackage();
        this.skipNamespaceChecking = false;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onStart(ElementPath epath) {
        
        Element el = epath.getCurrent();
       
        String n = el.getName();

        if ("LexicalResource".equals(n)) {
            @Nullable
            String name = el.attributeValue("name");
            Internals.checkNotBlank(name, "Invalid LexicalResource name!");
            pack.setName(name);
            
            for (Attribute attr : (List<Attribute>) el.attributes()) {
                if ("xmlns".equals(attr.getNamespacePrefix())) {
                    pack.putNamespace(attr.getName(), attr.getValue());
                }
            }
            
        } else {
            if (!skipNamespaceChecking ) {
                // Would have liked to get it from class, but if we 
                // assume xml schema pass was done before it should still be ok
                
                @Nullable
                String id = el.attributeValue("id");
                if (id != null) {
                    Internals.checkNotBlank(id, "Found invalid id!");

                    if (!id.startsWith(pack.getPrefix())) {
                        throw new InterruptedImportException("Found " + n + " id " + id
                                + " not starting with LexicalResource prefix " + pack.getPrefix());
                    }
                }
            }
        }       
        

    }

    @Override
    public void onEnd(ElementPath elementPath) {
        // do nothing
    }

}

/**
 * @since 0.1.0
 */
class DivXmlToDbTransformer extends XMLToDBTransformer {      

    /**
     * @since 0.1.0
     */
    DivXmlToDbTransformer(Diversicon div, boolean skipNamespaceChecking) {
        super(div.getDbConfig());        
        sessionFactory.close();  // div dirty but needed...       
        sessionFactory = div.getSessionFactory();       
    }      

}
