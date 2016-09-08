package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

/**  
 * 
 * @since 0.1.0
 */
public class Namespace {
        
    private String prefix;
    private String url;
    private String name;

    /**
     * @since 0.1.0
     */    
    public Namespace(){
        this.prefix = "";
        this.url = "";
        this.name = "";
    }

    /**
     * @since 0.1.0
     */
    public Namespace(
            String name, 
            String prefix, 
            String url) {
        this.name = name;
        this.prefix = prefix;
        this.url = url;       
    }


    /**
     * @since 0.1.0
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the id of the LexicalResource this 
     * 
     * @since 0.1.0
     */
    public void setName(String name) {
        checkNotNull(name);
        this.name = name;
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
        checkNotNull(url);
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
