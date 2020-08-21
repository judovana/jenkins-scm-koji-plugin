package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RemoteRequestsCache {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRequestsCache.class);
    private static final long minutesToMilis = 60l * 1000l;
    private final Map<String, SingleUrlResponseCache> cache = Collections.synchronizedMap(new HashMap<>());
    private final File config;
    private long configRefreshRateMinutes = 10; //if config contains clear=true then cache is cleared every this interval
    private long cacheRefreshRateMinutes = 60 * 6;
    private final Properties propRaw = new Properties();


    private class ConfigRefresh implements Runnable {

        private boolean alive = true;

        @Override
        public void run() {
            while (alive) {
                try {
                    Thread.sleep(configRefreshRateMinutes * minutesToMilis);
                } catch (Exception e) {
                    LOG.warn("Faield to read existing  cache config", e);
                }
                read();
            }
        }

        private void read() {
            if (config != null && config.exists()) {
                try (InputStream inputStream = new FileInputStream(config)) {
                    propRaw.load(inputStream);
                } catch (IOException e) {
                    LOG.warn("Faield to read existing  cache config", e);
                }
            }
            apply();
        }
    }

    private void apply() {
        String configRefreshRateMinutesS = propRaw.getProperty("configRefreshRateMinutes");
        String cacheRefreshRateMinutesS = propRaw.getProperty("cacheRefreshRateMinutes");
        if (configRefreshRateMinutesS != null) {
            try {
                configRefreshRateMinutes = Long.parseLong(configRefreshRateMinutesS);
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + configRefreshRateMinutesS + ") for configRefreshRateMinutes", ex);
            }
        }
        if (cacheRefreshRateMinutesS != null) {
            try {
                cacheRefreshRateMinutes = Long.parseLong(cacheRefreshRateMinutesS);
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom value  of (" + cacheRefreshRateMinutesS + ") for cacheRefreshRateMinutes", ex);
            }
        }
        if ("true".equals(propRaw.getProperty("clean"))) {
            cache.clear();
        }
    }

    public RemoteRequestsCache(final File config) {
        this.config = config;
        ConfigRefresh r = new ConfigRefresh();
        r.read();
        LOG.info("Cache started - cacheRefreshRateMinutes is " + cacheRefreshRateMinutes + " (0==disabled). Set config is: " + getConfigString());
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    private String getConfigString() {
        if (config == null) {
            return "null";
        } else {
            return config.getAbsolutePath() + "/" + config.exists() + " configRefreshRateMinutes=" + configRefreshRateMinutes;
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

    public Object get(final URL u, XmlRpcRequestParams params) {
        SingleUrlResponseCache cached = cache.get(u.toExternalForm());
        if (cached == null) {
            return null;
        }
        SingleUrlResponseCache.ResultWithTimeStamp cachedResult = cached.get(params);
        if (cacheRefreshRateMinutes <= 0) {
            return null; //disabled
        }
        if (cachedResult == null) {
            return null;
        } else {
            if (isValid(cachedResult.getDateCreated(), cacheRefreshRateMinutes * minutesToMilis, params.getMethodName())) {
                return cachedResult.getResult();
            } else {
                return null;
            }
        }
    }

    private boolean isValid(final Date dateCreated, final long cachemaxLive, String methodName) {
        String rawCustomTimePerMethod = propRaw.getProperty(methodName);
        long customTimeoutPerMethod;
        if (rawCustomTimePerMethod != null) {
            try {
                customTimeoutPerMethod = Long.parseLong(rawCustomTimePerMethod) * minutesToMilis;
                return new Date().getTime() - dateCreated.getTime() < customTimeoutPerMethod;
            } catch (Exception ex) {
                LOG.warn("Failed to read or apply custom method (" + methodName + ") timeout (" + rawCustomTimePerMethod + ")", ex);
            }
        }
        return new Date().getTime() - dateCreated.getTime() < cachemaxLive;
    }

}
