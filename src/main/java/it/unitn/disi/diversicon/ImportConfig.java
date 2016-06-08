package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a whole {@link ImportTodo} process.
 * 
 * @since 0.1
 * @see ImportJob
 */
public class ImportConfig {
    private List<String> fileUrls;
    private List<String> lexicalResourceNames;
    private String author;
    private String description;
    private boolean skipAugment;

    /**
     * Config default constructor.
     * 
     * By default augments graph after import.
     * 
     * @since 0.1
     */
    public ImportConfig() {
        this.fileUrls = new ArrayList();
        this.lexicalResourceNames = new ArrayList();
        this.author = "";
        this.description = "";
        this.skipAugment = false;
    }

    /**
     * The URL of the file that was imported
     * 
     * @since 0.1
     */
    public List<String> getFileUrls() {
        return fileUrls;
    }

    /**
     * See {@link #getFileUrls()}
     * 
     * @since 0.1
     */
    public void setFileUrls(List<String> fileUrls) {
        checkNotNull(fileUrls);
        this.fileUrls = fileUrls;
    }

    /**
     * The name of the author of the import
     * 
     * @since 0.1
     */
    public String getAuthor() {
        return author;
    }

    /**
     * See {@link #getAuthor()}
     * 
     * @since 0.1
     */
    public void setAuthor(String author) {
        checkNotNull(author);
        this.author = author;
    }

    /**
     * A description of the import.
     * 
     * Why you did the import? For which project was intended? Which problems
     * did you find during the import?
     * 
     * @since 0.1
     */
    public String getDescription() {
        return description;
    }

    /**
     * see {}@link {@link #getDescription()}
     * 
     * @since 0.1
     */
    public void setDescription(String description) {
        checkNotNull(description);
        this.description = description;
    }

    /**
     * The names of the lexical resources to import.
     * 
     * @since 0.1
     */
    // todo what about merge / delete?
    public List<String> getLexicalResourceNames() {
        return lexicalResourceNames;
    }

    /**
     * See {@link #getLexicalResourceNames()}
     * 
     * @since 0.1
     */
    public void setLexicalResourceNames(List<String> lexicalResourceNames) {
        checkNotNull(lexicalResourceNames);
        this.lexicalResourceNames = lexicalResourceNames;
    }

    /**
     * 
     * Flag to skip graph augmentation after the import (i.e. to calculate transitive closures)
     * 
     * @since 0.1
     */
    public boolean isSkipAugment() {
        return skipAugment;
    }

    /**
     * See {@link #isSkipAugment()}
     * 
     * @since 0.1
     */
    public void setSkipAugment(boolean skipAugment) {
        this.skipAugment = skipAugment;
    }

    /**
     * 
     * @since 0.1
     */     
    public void addLexicalResource(String fileUrl, String resourceName) {
        checkNotEmpty(fileUrl, "Invalid lexical resource file URL!");
        checkNotEmpty(resourceName, "Invalid lexical resource name!");
        this.fileUrls.add(fileUrl);
        this.lexicalResourceNames.add(resourceName);
    }

}


