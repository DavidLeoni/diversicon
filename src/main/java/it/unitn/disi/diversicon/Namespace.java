package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

/**  
 * 
 * @since 0.1.0
 *
 */
public class Namespace {
        
    private String prefix;
    private String url;

    /**
     * @since 0.1.0
     */    
    public Namespace(){
        this.prefix = "";
        this.url = "";
    }

        
    public Namespace(String prefix, String url) {
        this.prefix = prefix;
        this.url = url;
    }



    /**
     * @since 0.1.0
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * @since 0.1.0
     */    
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * @since 0.1.0
     */    
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * @since 0.1.0
     */    
    public void setPrefix(String prefix) {
        checkNotNull(prefix);
        this.prefix = prefix;
    }
        
}
