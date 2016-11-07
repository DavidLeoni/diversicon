package eu.kidf.diversicon.core;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.internal.Internals;

/**
 * Configuration for Xml validator. To create objects, use {@link #builder()}
 * 
 * @since 0.1.0
 *
 */
public class XmlValidationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(XmlValidationConfig.class);                
    
    private Logger log;
    private long logLimit;
    private boolean failFast;
    @Nullable
    private String xsdUrl;
    
    private DivConfig diversiconConfig;

    /**
     * @since 0.1.0
     */
    private XmlValidationConfig() {
        this.log = LoggerFactory.getLogger(XmlValidationConfig.class);
        this.logLimit = Diversicons.DEFAULT_LOG_LIMIT;
        this.failFast = false;
        this.xsdUrl = null;
        this.diversiconConfig = DivConfig.of();
    }

    /**
     * Once done building, call {@link Builder#build()}
     * @since 0.1.0
     */
    public static Builder builder(){
        return new Builder();
    }  

    /**
     * Returns the amount of logs which will be outputted. If -1 all
     * log messages will be emitted.
     * 
     * @since 0.1.0
     */
    public long getLogLimit() {
        return logLimit;
    }

  

    /**
     * If true the handler will throw an error as soon {{@link #getLogLimit()}
     * log limit} errors
     * are reached. If logLimit is -1 the handler will throw on first error.
     * 
     * @since 0.1.0
     */
    public boolean isFailFast() {
        return failFast;
    }

    
   

    /**
     * The logger where to which messages are redirected.
     * 
     * @since 0.1.0
     */
    public Logger getLog() {
        return log;
    }

    /**
     * @since 0.1.0
     */
    public DivConfig getdiversiconConfig(){
        return diversiconConfig;
    }
    
    /**
     * The Xml Schema to use to validate the document. Will override the
     * schema pointed to in the document. If unknown, use an empty string.
     * 
     * @since 0.1.0
     */
    public String getXsdUrl() {
        return xsdUrl;
    }

    /**
     * @since 0.1.0
     *
     */
    public static class Builder {
        private XmlValidationConfig config;
        private boolean built;
        
        /**
         * @since 0.1.0
         */        
        private Builder(){
            this.built = false;
            this.config = new XmlValidationConfig();            
        }
        
        /**
         * Creates a validation config object.
         * <p>
         * NOTE: Each builder
         * instance can build only one object, attempts to build more than
         * one will result in an exception.
         * </p>
         * 
         * @throws DivException
         *             if an object was already built with this builder.
         * 
         * @since 0.1.0
         */       
        public XmlValidationConfig build(){
            checkBuilt();
            
            this.built = true;
            return config;
        }
        
        /**
         * @since 0.1.0
         */
        private void checkBuilt(){
            if (built){
                throw new DivException("A Validation config was already built!");
            }
        }
        
        
        
        /**
         * See {@link XmlValidationConfig#getLog()}
         * 
         * @since 0.1.0
         */    
        public Builder setLog(@Nullable Logger log) {
            checkBuilt();
            
            if (log == null) {
                config.log.error("Found null log, using default one!");
            } else {
                config.log = log;
            }
            return this;
        }
        
        
        
        /**
         * See {@link XmlValidationConfig#getdiversiconConfig()}
         * 
         * @since 0.1.0
         */    
        public Builder setdiversiconConfig(DivConfig diversiconConfig) {
            checkBuilt();
            Internals.checkNotNull(diversiconConfig, "Locator config can't be null! If you want to use the default one, calle diversiconConfig.of()!");
            
            config.diversiconConfig= diversiconConfig;
            
            return this;
        }
               
        /**
         * See {@link XmlValidationConfig#getLogLimit()}.
         * 
         * @param logLimit
         *            if -1 it means all messages are sent to logger.
         * 
         * @since 0.1.0
         */
        public Builder setLogLimit(long logLimit) {
            checkBuilt();
            
            if (logLimit < -1) {
                config.log.error("Found config.getLogLimit() < -1, setting it to -1");
                config.logLimit = -1;
            } else {
                config.logLimit = logLimit;
            }
            return this;
        }
        /**
         * See {@link XmlValidationConfig#isFailFast()}
         * 
         * @since 0.1.0
         */
        public Builder setFailFast(boolean failFast) {
            checkBuilt();
            
            config.failFast = failFast;
            return this;
        }
        /**
         * See {@link XmlValidationConfig#getXsdUrl()}
         * 
         * @since 0.1.0
         */
        public Builder setXsdUrl( String xsdUrl) {
            checkBuilt();
            if (xsdUrl == null){
                LOG.error("Found null xsdUrl, setting it to the empty string!");
                this.config.xsdUrl = "";
            } else {
                config.xsdUrl = xsdUrl;
            }
            return this;
        }
        

         
    }

    /**
     * 
     * Creates an instance of the config with provided log.
     * 
     * @param log see {@link #getLog()}
     * 
     * @since 0.1.0
     */
    public static XmlValidationConfig of(Logger log) {
        return XmlValidationConfig.builder().setLog(log).build();
    }

}
