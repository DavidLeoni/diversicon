package it.unitn.disi.diversicon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.ElementPath;
import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;
import it.unitn.disi.diversicon.exceptions.InterruptedImportException;
import it.unitn.disi.diversicon.internal.Internals;

/**
 * @since 0.1.0
 */
class DivXmlToDbTransformer extends XMLToDBTransformer {

    private boolean skipNamespaceChecking;

    private Map<String, String> namespaces;

    /**
     * @since 0.1.0
     */
    DivXmlToDbTransformer(Diversicon div, boolean skipNamespaceChecking) {
        // silly but needed, can't call super() :-/
        super(Diversicons.makeDefaultH2InMemoryDbConfig("tmp" + UUID.randomUUID(), false));
        session.close();
        sessionFactory = div.getSessionFactory();
        session = div.getSession();
        this.skipNamespaceChecking = skipNamespaceChecking;
        this.namespaces = new HashMap<>();

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

            for (Attribute attr : (List<Attribute>) el.attributes()) {
                if ("xmlns".equals(attr.getNamespacePrefix())) {
                    namespaces.put(attr.getName(), attr.getValue());
                }
            }

            Internals.checkLexicalResource(name, namespaces, skipNamespaceChecking);
        } else {
            if (!skipNamespaceChecking && lexicalResource != null) {
                // Would have liked to get it from class, but if we 
                // assume xml schema pass was done before it should still be ok
                
                @Nullable
                String id = el.attributeValue("id");
                if (id != null) {
                    Internals.checkNotBlank(id, "Found invalid id!");

                    if (!id.startsWith(lexicalResource.getName())) {
                        throw new InterruptedImportException("Found " + n + " id " + id
                                + " not starting with LexicalResource name " + lexicalResource.getName());
                    }
                }
            }
        }

        // note super.onStart discards invalid attributes (like ones containing
        // 'NULL')
        super.onStart(epath);

    }

}
