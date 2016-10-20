package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

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
     * The name of the LexicalResource.
     * 
     * @since 0.1.0
     */
    public String getName() {
        return name;
    }

    /**
     *  
     * See {@link #getName()}
     * @since 0.1.0
     */
    public void setName(String name) {
        checkNotNull(name);
        this.name = name;
    }


    /**
     * The url of the LexicalResource.
     * 
     * @since 0.1.0
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * See {@link #getUrl()}
     * 
     * @since 0.1.0
     */    
    public void setUrl(String url) {
        checkNotNull(url);
        this.url = url;
    }
    
    /**
     * The prefix associated to the LexicalResource.
     * 
     * @since 0.1.0
     */    
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * See {@link #getPrefix()}
     * 
     * @since 0.1.0
     */    
    public void setPrefix(String prefix) {
        checkNotNull(prefix);
        this.prefix = prefix;
    }
        
}
