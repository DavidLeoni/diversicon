package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import javax.annotation.Nullable;

import eu.kidf.diversicon.core.exceptions.DivNotFoundException;


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
    private boolean toValidate;

    /**
     * @since 0.1.0
     */
    public DbInfo() {
        this.schemaVersion = Diversicons.SCHEMA_VERSION_1_0;
        this.version = "";       
        this.toValidate = false;
        this.toNormalize = false;
        this.toAugment = false;
        this.description = "";        
    }
       

    /**
     * Return the current import job. Note it can be {@code null}, for a null-safe version
     * use {@link #currentImportJob} instead.
     * 
     * @throws DivNotFound if there is no import job
     * 
     * @since 0.1.0
     */
    @Nullable
    public ImportJob getCurrentImportJob() {
        return currentImportJob;
    }
    
    /**
     * 
     * Return the current import job. If 
     * 
     * @throws DivNotFoundException if there is no import job
     * @see #getCurrentImportJob()
     * 
     * @since 0.1.0
     */
    public ImportJob currentImportJob() {
        if (this.currentImportJob == null){
            throw new DivNotFoundException("There is no current import job!");
        }
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
     * 
     * @since 0.1.0
     */
    public boolean isToNormalize() {
        return toNormalize;
    }

    /**
     * See {@link #isToNormalize()}
     * 
     * @since 0.1.0
     */
    public void setToNormalize(boolean toNormalize) {
        this.toNormalize = toNormalize;
    }

    /**
     * Flag to state if database is yet to be augmented with i.e. transitive closure
     * 
     * @since 0.1.0
     */
    public boolean isToAugment() {
        return toAugment;
    }

    /**
     * Flag to state if database is yet to be validated. For now we only check most
     * blatant violations (i.e. self-loops) 
     *
     * @since 0.1.0
     */
    public boolean isToValidate() {
        return toValidate;
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


    /**
     * @see #isToValidate()
     * @since 0.1.0
     */
    public void setToValidate(boolean toValidate) {
        this.toValidate = toValidate;
    }


}
