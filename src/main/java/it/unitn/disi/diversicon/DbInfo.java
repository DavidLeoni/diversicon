package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;


/**
 * Information about database status and creation.
 *
 * @since 0.1.0
 */
public class DbInfo {
    
    private String schemaVersion;
    private boolean toNormalize;
    private boolean toAugment;
    private String description;
    private String version;
    @Nullable
    private ImportJob currentImportJob;


    /**
     * @since 0.1.0
     */
    public DbInfo() {
        this.schemaVersion = Diversicon.DIVERSICON_SCHEMA_VERSION;
        this.version = "";
        this.toNormalize = false;
        this.toAugment = false;
        this.description = "";
    }


    /**
     * @since 0.1.0
     */
    @Nullable
    public ImportJob getCurrentImportJob() {
        return currentImportJob;
    }

    /**
     * @since 0.1.0
     */    
    public void setCurrentImportJob(@Nullable ImportJob currentImportJob) {
        this.currentImportJob = currentImportJob;
    }    
    
    /**
     * Flag to state if database is yet to be normalized
     * @since 0.1.0
     */
    public boolean isToNormalize() {
        return toNormalize;
    }

    /**
     * See {@link #isToNormalize()}
     * @since 0.1.0
     */
    public void setToNormalize(boolean toNormalize) {
        this.toNormalize = toNormalize;
    }

    /**
     * Flag to state if database is yet to be augmented with i.e. transitive closure
     * @since 0.1.0
     */
    public boolean isToAugment() {
        return toAugment;
    }

    /**
     * See {@link #isToAugment()}
     * @since 0.1.0
     */
    public void setToAugment(boolean toAugment) {
        this.toAugment = toAugment;
    }

    /**
     * The version of the database schema
     * @since 0.1.0
     */
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * @since 0.1.0
     */
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * A description of database.
     * 
     * Why the db was created? For which project was intended? Which problems
     * did you find when creating it? Did you have to modify any of the input
     * resources to make the import
     * process succeed?
     * @since 0.1.0
     */
    public String getDescription() {
        return description;
    }

    /**
     * See {@link #getDescription()}
     * @since 0.1.0
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * The version of the data within the database.
     * 
     * @see #getSchemaVersion()
     * @since 0.1.0
     */
    public String getVersion() {
        return version;
    }

    /**
     * See {@link #getVersion()}
     * 
     * @see #getSchemaVersion()
     * @since 0.1.0
     */
    public void setVersion(String version) {
        checkNotNull(version);
        this.version = version;
    }


}
