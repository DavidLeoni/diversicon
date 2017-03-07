package eu.kidf.diversicon.core;

import de.tudarmstadt.ukp.lmf.transform.XMLToDBTransformer;



/**
 * @since 0.1.0
 */
class DivXmlToDbTransformer extends XMLToDBTransformer {      

    /**
     * @since 0.1.0
     */
    @SuppressWarnings("deprecation")
    DivXmlToDbTransformer(Diversicon div) {
        super(div.getDbConfig());        
        sessionFactory.close();  // div dirty but needed...       
        sessionFactory = div.getSessionFactory();       
    }      

}
