package org.fakekoji.xmlrpc.server.expensiveobjectscache;

/**
 * properties keys.
 * any other key, s treated as method
 * unknowKey=10
 * will set timout of method unknownKey to 10 minutes
 * It support also extended  method@urlRegex
 * so method1=10 will set generic timeout for method1 to 10 minutes
 * However method1@host=1 will set method1, if reqest on host named host. to 1. Note, port nor path is not considered here
 */
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
     * N. After acheRefreshRateMinutes 9or per method) x  this number, the item will be removed fromo cache for ever.
     * The check runs when configRefreshRateMinutes refresh thr config
     * If set to 0, items remains in cache forever
     */
    public static final String CACHE_RELEASE_TIMOUT_MULTIPLIER = "cacheReleaseTimeout";
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
