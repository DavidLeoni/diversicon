package eu.kidf.diversicon.core;

import javax.annotation.Nullable;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import eu.kidf.diversicon.core.internal.Internals;

/**
 * Configuration of Diversicon. Includes UBY's {@link DBConfig}
 * and config for accessing external resources like the database
 * files over HTTP.
 * 
 * <p>
 * Class is immutable and thread safe (even if it contains DBConfig
 * which is mutable). You can create instances via the
 * {@link DivConfig#builder() builder()} method if you need to set more
 * connection parameters (i.e. proxy, timeout, ..).
 * </p>
 * 
 * @since 0.1.0
 */
public class DivConfig {

    /**
     * Default timeout in millisecs
     * 
     * @since 0.1.0
     */
    public static final int DEFAULT_TIMEOUT = 15000;

    /**
     * @since 0.1.0
     */
    private static final DivConfig INSTANCE = new DivConfig();

    @Nullable
    private String httpProxy;

    /** connection timeout in millisecs */
    private int timeout;

    private DBConfig dbConfig;

    /**
     * @since 0.1.0
     */
    private DivConfig() {
        this.httpProxy = null;
        this.timeout = DEFAULT_TIMEOUT;
        this.dbConfig = null;
    }

    /**
     * Returns the proxy
     *
     * @since 0.1.0
     */
    @Nullable
    public String getHttpProxy() {
        return httpProxy;
    }

    /**
     * Returns the timeout in millisecs
     *
     * @since 0.1.0
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Returns a new locator builder
     * 
     * The builder is not threadsafe and you can use one builder instance to
     * build only one locator instance.
     * 
     * @since 0.1.0
     */
    public static DivConfig.Builder builder() {
        return new Builder();
    }
    
    /**
     * Returns a new Diversicon config builder, created of out provided divConfig 
     * (internally it will be deep copied).
     * 
     * The builder is not threadsafe and you can use one builder instance to
     * build only one locator instance.
     * 
     * @since 0.1.0
     */
    public static DivConfig.Builder builder(DivConfig divConfig) {
        return new Builder(divConfig);
    }

    /**
     * Builder for the config. The builder is not threadsafe and you can use one
     * builder instance to build only one config instance.
     *
     * @since 0.1.0
     */
    public static class Builder {
        private DivConfig config;
        private boolean created;

        /**
         * @since 0.1.0
         */
        protected Builder() {            
            this.config = new DivConfig();
            this.created = false;
        }
        
        /**
         * @since 0.1.0
         */
        protected Builder(DivConfig divConfig) {
            this();
            this.config = Internals.deepCopy(divConfig);
        }
        
        
        /**
         * @since 0.1.0
         */
        protected DivConfig getConfig() {
            return config;
        }

        /**
         * @since 0.1.0
         */
        protected boolean getCreated() {
            return created;
        }

        /**
         * @since 0.1.0
         */
        protected void checkNotCreated() {
            if (created) {
                throw new IllegalStateException("Builder was already used to create a config!");
            }
        }


        /**
         * Sets the proxy used to perform GET and POST calls
         * 
         * @param proxyUri the proxy used by the client, usually in a format with 
         * address and port like {@code my.own-proxy.org:1234}
         * 
         * @since 0.1.0
         */
        public Builder setHttpProxy(@Nullable String proxyUri) {
            checkNotCreated();

            if (proxyUri == null) {
                this.config.httpProxy = null;
            } else {
                this.config.httpProxy= proxyUri;
            }
            
            return this;
        }

        /**
         * Sets the connection timeout expressed as number of milliseconds. Must
         * be greater than zero, otherwise IllegalArgumentException is thrown.
         * Defaults to {@link #DEFAULT_TIMEOUT}.
         *
         * @throws IllegalArgumentException
         *             is value is less than 1.
         * 
         * @since 0.1.0
         */
        public Builder setTimeout(int timeout) {
            checkNotCreated();
            Internals.checkArgument(timeout > 0, "Timeout must be > 0 ! Found instead %s", timeout);
            this.config.timeout = timeout;
            return this;
        }

        /**
         * Since DBConfig is mutable, for safety a copy of the provided object
         * is stored.
         * 
         * @since 0.1.0
         */
        public Builder setDbConfig(@Nullable DBConfig dbConfig) {
            checkNotCreated();            
            this.config.dbConfig = Internals.deepCopy(dbConfig);
            return this;
        }

        /**
         * Returns a new instance of Diversicon Config. You can't call this
         * method twice.
         * 
         * @since 0.1.0
         * 
         */
        public DivConfig build() {
            checkNotCreated();
            this.created = true;
            return this.config;
        }
    }

    /**
     * Default config. 
     * 
     * <strong>NOTE: internal DBConfig is null by default !</strong> 
     * 
     * @since 0.1.0
     */
    public static DivConfig of() {
        return INSTANCE;
    }
    
    /**
     * Creates a config out of provided UBY's {@link DBConfig} object.
     * 
     * @since 0.1.0
     */
    public static DivConfig of(DBConfig dbConfig) {
        return DivConfig.builder().setDbConfig(dbConfig).build();
    }

    /**
     * Returns the UBY's DB configuration. To preserve immutability,
     * a *copy* of the db config is returned.
     * 
     * @since 0.1.0
     */
    @Nullable
    public DBConfig getDbConfig() {        
        return Internals.deepCopy(this.dbConfig);
    }

    public DivConfig withDbConfig(DBConfig dbConfig) {
        Internals.checkNotNull(dbConfig);
        DivConfig ret = Internals.deepCopy(this);
        ret.dbConfig = dbConfig;
        return ret; 
    }
    
    
}
