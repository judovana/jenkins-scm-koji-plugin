package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RemoteRequestsCache {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRequestsCache.class);
    private static final long minutesToMillis = 60l * 1000l;

    private final Map<String, SingleUrlResponseCache> cache = Collections.synchronizedMap(new HashMap<>());
    private final File config;
    private static final long CONFIG_DEFAULT = 10;
    private long configRefreshRateMinutes = CONFIG_DEFAULT;
    static final long CACHE_DEFAULT = 60 * 6;
    private long cacheRefreshRateMinutes = CACHE_DEFAULT;
    static final long RELEASE_DEFAULT = CACHE_DEFAULT * 5;
    private long cacheReleaseRate = RELEASE_DEFAULT;
    private Properties propRaw = new Properties();
    private final OriginalObjectProvider originalProvider;
    private List<Pattern> blackListedUrlsList = new ArrayList<>();
    protected boolean loaded = false;

    public Object obtain(String url, XmlRpcRequestParams params) {
        try {
            final URL u = new URL(url);
            final Object cached = this.get(u, params);
            if (cached != null) {
                return cached;
            } else {
                Object answer = originalProvider.obtainOriginal(url, params);
                this.put(answer, u, params);
                return answer;
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }


    private class ConfigRefresh implements Runnable {

        private boolean alive = true;

        @Override
        public void run() {
            while (alive) {
                try {
                    Thread.sleep(getConfigRefreshRateMilis());
                } catch (Exception e) {
                    LOG.warn("Failed to read existing  cache config", e);
                }
                read();
            }
        }

        private void read() {
            if (config != null && config.exists()) {
                try (InputStream inputStream = new FileInputStream(config)) {
                    Properties propNew = new Properties();
                    propNew.load(inputStream);
                    propRaw = propNew;
                } catch (IOException e) {
                    LOG.warn("Failed to read existing  cache config", e);
                }
            }
            apply();
            freeOldItems();
        }
    }

    protected long toUnits(long time) {
        return time * minutesToMillis;
    }

    protected long getConfigRefreshRateMilis() {
        return toUnits(configRefreshRateMinutes);
    }

    protected void setProperties(Properties prop) {
        this.propRaw = prop;
        apply();
    }

    private void freeOldItems() {
        if (cacheReleaseRate > 0) {
            Date now = new Date();
            Collection<SingleUrlResponseCache> caches = cache.values();
            for (SingleUrlResponseCache cache : caches) {
                Set<Map.Entry<XmlRpcRequestParams, SingleUrlResponseCache.ResultWithTimeStamp>> kvs = cache.getContent();
                for (Map.Entry<XmlRpcRequestParams, SingleUrlResponseCache.ResultWithTimeStamp> kv : kvs) {
                    long t = getPerMethodValidnesMilis(kv.getKey().getMethodName(), cache.getId().getHost());
                    if (now.getTime() - kv.getValue().getDateCreated().getTime() > t * cacheReleaseRate) {
                        cache.remove(kv.getKey());
                    }
                }
            }
        }
    }

    private void apply() {
        String configRefreshRateMinutesS = propRaw.getProperty(RemoteRequestCacheConfigKeys.CONFIG_REFRESH_RATE_MINUTES);
        String cacheRefreshRateMinutesS = propRaw.getProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES);
        String cacheReleaseS = propRaw.getProperty(RemoteRequestCacheConfigKeys.CACHE_RELEASE_TIMEOUT_MULTIPLIER);
        String blackListedUrlsListS = propRaw.getProperty(RemoteRequestCacheConfigKeys.BLACK_LISTED_URLS_LIST);
        if (configRefreshRateMinutesS != null) {
            try {
                configRefreshRateMinutes = Long.parseLong(configRefreshRateMinutesS);
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + configRefreshRateMinutesS + ") for " + RemoteRequestCacheConfigKeys.CONFIG_REFRESH_RATE_MINUTES + "", ex);
            }
        } else {
            configRefreshRateMinutes = CONFIG_DEFAULT;
        }
        if (cacheRefreshRateMinutesS != null) {
            try {
                cacheRefreshRateMinutes = Long.parseLong(cacheRefreshRateMinutesS);
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + cacheRefreshRateMinutesS + ") for " + RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, ex);
            }
        } else {
            if (config!=null && !config.exists()) {
                cacheRefreshRateMinutes = 0;
            } else {
                cacheRefreshRateMinutes = CACHE_DEFAULT;
            }
        }
        if (cacheReleaseS != null) {
            try {
                cacheReleaseRate = Long.parseLong(cacheReleaseS);
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + cacheReleaseS + ") for " + RemoteRequestCacheConfigKeys.CACHE_RELEASE_TIMEOUT_MULTIPLIER, ex);
            }
        } else {
            cacheReleaseRate  = CACHE_DEFAULT;
        }
        if (blackListedUrlsListS != null && blackListedUrlsListS.trim().length() > 0) {
            try {
                blackListedUrlsList = Arrays.stream(blackListedUrlsListS.split("\\s+")).map(Pattern::compile).collect(Collectors.toList());
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + configRefreshRateMinutesS + ") for " + RemoteRequestCacheConfigKeys.CONFIG_REFRESH_RATE_MINUTES + "", ex);
            }
        } else {
            blackListedUrlsList = new ArrayList<>();
        }
        if ("true".equals(propRaw.getProperty(RemoteRequestCacheConfigKeys.CACHE_CLEAN_COMMAND))) {
            cache.clear();
        }
        loaded = true;
    }

    public RemoteRequestsCache(final File config, OriginalObjectProvider originalObjectProvider) {
        this.config = config;
        this.originalProvider = originalObjectProvider;
        ConfigRefresh r = new ConfigRefresh();
        r.read();
        LOG.info("Cache started - " + RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES + " is " + cacheRefreshRateMinutes + " (0==disabled). Set config is: " + getConfigString());
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    private String getConfigString() {
        if (config == null) {
            return "null";
        } else {
            return config.getAbsolutePath() + "/" + config.exists() + " " + RemoteRequestCacheConfigKeys.CONFIG_REFRESH_RATE_MINUTES + "=" + configRefreshRateMinutes;
        }
    }


    private SingleUrlResponseCache ensure(final URL u) {
        //we can not use URL as key, becasue it includes resolved IP in hash. That can differ in reqests to same URL
        SingleUrlResponseCache sux = cache.get(u.toExternalForm());
        if (sux == null) {
            sux = new SingleUrlResponseCache(u);
            cache.put(u.toExternalForm(), sux);
        }
        return sux;
    }

    public void put(final Object result, final URL u, XmlRpcRequestParams params) {
        ensure(u).put(result, params);
    }

    private Object get(final URL u, XmlRpcRequestParams params) {
        if (cacheRefreshRateMinutes == 0) {
            return null;
        }
        if (isBlacklisted(u)) {
            return null;
        }
        SingleUrlResponseCache cached = cache.get(u.toExternalForm());
        if (cached == null) {
            return null;
        }
        SingleUrlResponseCache.ResultWithTimeStamp cachedResult = cached.get(params);
        if (cachedResult == null) {
            return null;
        } else {
            Boolean validity = isValid(cachedResult, params.getMethodName(), u.getHost());
            if (validity == null) {
                return null; //disbaled by global or by method
            }
            if (validity) {
                return cachedResult.getResult();
            } else {
                //if the  objkect is already being replaced, we do not check the time and return it as valid, as we know, it will already be refreshed
                if (cachedResult.isNotBeingReplaced()) {
                    cachedResult.flagBeingReplaced();
                    return null;
                } else {
                    return cachedResult.getResult();
                }
            }
        }
    }

    private boolean isBlacklisted(URL u) {
        String url = u.toExternalForm();
        for (Pattern p : blackListedUrlsList) {
            if (p.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    protected long getDefaultValidnesMilis() {
        return toUnits(cacheRefreshRateMinutes);
    }

    protected long getPerMethodValidnesMilis(String methodName, String host) {
        // for method names:
        //See: hudson.plugins.scm.koji.Constants for methods
        //See: XmlRpcRequestParams getMethodName() vaues
        String rawCustomTimePerMethodPerHost = propRaw.getProperty(methodFromUrl(methodName, host));
        long customTimeoutPerMethod;
        if (rawCustomTimePerMethodPerHost != null) {
            try {
                customTimeoutPerMethod = toUnits(Long.parseLong(rawCustomTimePerMethodPerHost));
                return customTimeoutPerMethod;
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom method (" + methodFromUrl(methodName, host) + ") timeout (" + rawCustomTimePerMethodPerHost + ")", ex);
            }
        }
        String rawCustomTimePerMethod = propRaw.getProperty(methodName);
        if (rawCustomTimePerMethod != null) {
            try {
                customTimeoutPerMethod = toUnits(Long.parseLong(rawCustomTimePerMethod));
                return customTimeoutPerMethod;
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom method (" + methodName + ") timeout (" + rawCustomTimePerMethod + ")", ex);
            }
        }
        return getDefaultValidnesMilis();
    }

    private String methodFromUrl(String methodName, String host) {
        return methodName + RemoteRequestCacheConfigKeys.METHOD_AT_DELIMITER + host;
    }

    @SuppressFBWarnings(value = "NP_BOOLEAN_RETURN_NULL", justification = "private single usage method")
    private Boolean isValid(final SingleUrlResponseCache.ResultWithTimeStamp temptedResult, String methodName, String host) {
        final Date dateCreated = temptedResult.getDateCreated();
        long timeForThisMethodOrDefault = getPerMethodValidnesMilis(methodName, host);
        if (timeForThisMethodOrDefault == 0) {
            return null;
        } else {
            return new Date().getTime() - dateCreated.getTime() < timeForThisMethodOrDefault;
        }
    }

}
