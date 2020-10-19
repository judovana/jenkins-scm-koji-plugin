package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import hudson.plugins.scm.koji.Constants;

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
    public static final File DEFAULT_CONFIG_LOCATION = new File(System.getProperty("user.home"), "kojiscmplugin-xmlrpc.caching");
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
    public static final String CACHE_RELEASE_TIMEOUT_MULTIPLIER = "cacheReleaseTimeout";
    /**
     * space separated list of regexex of  urls
     * matching urls are NOT cached
     */
    public static final String BLACK_LISTED_URLS_LIST = "blackListedUrlsList";
    /**
     * Cache is cleared any momnet, clean=true occure in config file
     */
    public static final String CACHE_CLEAN_COMMAND = "clean";
    public static final String DUMP_COMMAND = "dump";
    public static final String METHOD_AT_DELIMITER = "@";
    public static final String NEW_API_MACHINE = "hydra";
    public static final String NEW_API_DOMAIN = "brq.redhat.com";
    public static final String NEW_API_SERVER = NEW_API_MACHINE + "." + NEW_API_DOMAIN;

    public static void saveDefault() throws IOException {
        saveExemplar(DEFAULT_CONFIG_LOCATION);
    }

    public static void saveExemplar(File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            printExemplar(fos);
        }

    }

    public static void printExemplar(OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "utf-8"))) {
            bw.write("# " + DEFAULT_CONFIG_LOCATION.getAbsolutePath() + "\n"
                    + "# copypasted defaults 10 minutes and 6 hours\n"
                    + CONFIG_REFRESH_RATE_MINUTES + "=10\n"
                    + CACHE_REFRESH_RATE_MINUTES + "cacheRefreshRateMinutes=0\n"
                    + "# hopefully there is a lot of ram on hydra\n"
                    + CACHE_RELEASE_TIMEOUT_MULTIPLIER + "=10\n"
                    + "\n"
                    + "# although  hydra is quick enough, it keeps reading FS, lower this  rate at least a bit\n"
                    + "#" + BLACK_LISTED_URLS_LIST + "=.*" + NEW_API_MACHINE + ".*\n"
                    + "\n"
                    + "#### individual methods ####\n"
                    + "\n"
                    + "# list builds is listing new builds on koji/brew/old api hydra; thus is refrehed  every 20 minutes\n"
                    + Constants.listBuilds + "=20\n"
                    + "\n"
                    + "# retaging is touching us very few but unluckily is maybe most used method, and shoudl be cached as much as possible\n"
                    + "# set to 24h now, but would be nice to have it longer, as we are testing gating.*, candidate.* and moreover all\n"
                    + "# if the build is disbaled, it is usually disabled much later after testing\n"
                    + "# on hydra, this method caching should be quite small,  as we use tag to swap from should be built -> built state on old api\n"
                    + Constants.listTags + "=1440\n"
                    + Constants.listTags + METHOD_AT_DELIMITER + NEW_API_SERVER + "=20\n"
                    + "\n"
                    + "# listrpms and listarchives is listing RPMS/archives of given build.\n"
                    + "# although on koji/brew this is immutable, it is not so it on hydra\n"
                    + "# on hydra, those two method caching should be disabled, as we are uploading files in runtime, and can happen, that not fully uplaoded build is picked up\n"
                    + "# note, this method should be invoked oly for final, selevted NVR, thus should notbe performance blocker\n"
                    + "# note, 99% requests to hydra is via powerfull single qestion new api: getBuildList and getBuildDetail\n"
                    + Constants.listRPMs + "=1440\n"
                    + Constants.listArchives + "=1440\n"
                    + Constants.listRPMs + METHOD_AT_DELIMITER + NEW_API_SERVER + "=0\n"
                    + Constants.listArchives + METHOD_AT_DELIMITER + NEW_API_SERVER + "=0\n"
                    + "\n"
                    + "# this is immutable onbrew/koji, refreshing once per week\n"
                    + "# I'm not sure how it is on hydra, where it is hash of File instance. \n"
                    + Constants.getPackageID + "=10080\n"
                    + Constants.getPackageID + METHOD_AT_DELIMITER + NEW_API_SERVER + "=1440\n"
                    + "\n"
                    + "# new api on hydra\n"
                    + Constants.getBuildList + "=20\n"
                    + Constants.getBuildDetail + "=1440\n"
                    + "\n"
                    + "# "+CACHE_CLEAN_COMMAND+"=false\n"
                    + "# "+DUMP_COMMAND+"=false\n"
                    + "");
        }
    }

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            printExemplar(System.out);
        } else {
            saveDefault();
            System.out.println("saved " + DEFAULT_CONFIG_LOCATION.getAbsolutePath());
        }
    }

}
