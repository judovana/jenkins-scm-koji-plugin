package org.fakekoji.xmlrpc.server.expensiveobjectscache;

/**
 * properties keys.
 * any other key, s treated as method
 * unknowKey=10
 * will set timout of method unknownKey to 10 minutes
 **/
public class RemoteRequestCacheConfigKeys {
    /**
     * Time in minutes after which config is reloaded
     */
    public static final String CONFIG_REFRESH_RATE_MINUTES = "configRefreshRateMinutes";
    /**
     * time in minutes, after which the cache records are invlaidate, unless thirs method says differently
     */
    public static final String CACHE_REFRESH_RATE_MINUTES = "cacheRefreshRateMinutes";
    /**
     * space separated list of regexex of  urls
     * matching urls are NOT cached
     */
    public static final String BLACK_LISTED_URLS_LIST = "blackListedUrlsList";
    /**
     * Cache is cleared any momnet, clean=true occure in config file
     */
    public static final String CACHE_CLEAN_COMMAND = "clean";

}
