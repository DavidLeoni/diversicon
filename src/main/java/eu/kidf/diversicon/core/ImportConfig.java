package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkArgument;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;
import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import eu.kidf.diversicon.core.XmlValidationConfig.Builder;

/**
 * Configuration for a whole {@link ImportJob} process.
 * 
 * @since 0.1.0
 * @see ImportJob
 */
// TODO: make it immutable https://github.com/diversicon-kb/diversicon-core/issues/35
public class ImportConfig {

    private List<String> fileUrls;
    private String author;
    private String description;
    private boolean skipAugment;
    private int logLimit;   
    private boolean force;
    private boolean dryRun;

    /**
     * Default constructor.
     * 
     * By default augments graph after import and fails on missing dependencies.
     * 
     * @since 0.1.0
     */
    public ImportConfig() {
        this.fileUrls = new ArrayList<>();
        this.author = "";
        this.description = "";
        this.skipAugment = false;
        this.logLimit = Diversicons.DEFAULT_LOG_LIMIT;
        this.force = false;
        this.dryRun = false;
    }

    /**
     * The URL of the files to import. For supported URL formats see
     * {@link eu.kidf.diversicon.core.internal.Internals#readData(String, boolean)
     * Internals.readData}
     * 
     * @since 0.1.0
     */
    public List<String> getFileUrls() {
        return fileUrls;
    }

    /**
     * See {@link #getFileUrls()}
     * 
     * @since 0.1.0
     */
    public void setFileUrls(List<String> fileUrls) {
        checkNotNull(fileUrls);
        this.fileUrls = fileUrls;
    }

    /**
     * The name of the author of the import
     * 
     * @since 0.1.0
     */
    public String getAuthor() {
        return author;
    }

    /**
     * See {@link #getAuthor()}
     * 
     * @since 0.1.0
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
     * @since 0.1.0
     */
    public String getDescription() {
        return description;
    }

    /**
     * See {@link #getDescription()}
     * 
     * @since 0.1.0
     */
    public void setDescription(String description) {
        checkNotNull(description);
        this.description = description;
    }

    /**
     * 
     * Flag to skip graph augmentation after the import (i.e. to calculate
     * transitive closures)
     * 
     * @since 0.1.0
     */
    public boolean isSkipAugment() {
        return skipAugment;
    }

    /**
     * See {@link #isSkipAugment()}
     * 
     * @since 0.1.0
     */
    public void setSkipAugment(boolean skipAugment) {
        this.skipAugment = skipAugment;
    }

    /**
     * 
     * @since 0.1.0
     */
    public void addLexResFileUrl(String fileUrl) {
        checkNotEmpty(fileUrl, "Invalid lexical resource file URL!");
        this.fileUrls.add(fileUrl);
    }

    /**
     * 
     * @since 0.1.0
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ImportConfig:\n");
        sb.append("  author      = " + author + "\n");
        sb.append("  description = " + description + "\n");
        sb.append("  skipAugment = " + skipAugment + "\n");
        sb.append("  fileUrls    = ");

        if (fileUrls.isEmpty()) {
            sb.append("[]\n");
        } else {
            for (String s : fileUrls) {
                sb.append("    " + s + "\n");
            }
        }

        return sb.toString();
    }

    /**
     * A limit on the maximum number of logs occurring during the import.
     * This number is merely an indication to Diversicon, actual output lines
     * may vary.
     * 
     * @since 0.1.0
     */
    public int getLogLimit() {
        return logLimit;
    }

    /**
     * Sets the log limit, see {@link #getLogLimit()}
     * 
     * @since 0.1.0
     */
    public void setLogLimit(int logLimit) {
        checkArgument(logLimit >= -1, "Log limit must be >= -1, found instead %s", logLimit);
        this.logLimit = logLimit;
    }

    /**
     * Force import even on missing external references.  
     * 
     * @since 0.1.0
     */
    public boolean isForce() {
        return force;
    }
    
    /**
     * See {@link #isForce()}
     * 
     * @since 0.1.0
     */
    public void setForce(boolean force) {        
        this.force = force;
    }
    
    /**
     * A dry run simply simulates the import without writing anything into the database.
     * 
     * @since 0.1.0
     */
    public boolean isDryRun() {
        return dryRun;
    }
    
    /**
     * See {@link #isDryRun()}
     * 
     * @since 0.1.0
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
