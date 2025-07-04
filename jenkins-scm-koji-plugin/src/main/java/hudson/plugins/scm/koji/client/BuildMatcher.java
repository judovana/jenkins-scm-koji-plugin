/*
 * The MIT License
 *
 * Copyright 2017 jvanek.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.plugins.scm.koji.LoggerHelp;
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;

import org.fakekoji.xmlrpc.server.expensiveobjectscache.RemoteRequestCacheConfigKeys;
import org.fakekoji.xmlrpc.server.expensiveobjectscache.RemoteRequestsCache;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Stream;

abstract class BuildMatcher {

    public enum OrderBy {
        DATE, VERSION
    }

    private static final OrderBy orderBy = OrderBy.DATE;

    private final List<KojiBuildProvider> buildProviders;
    private final Predicate<String> notProcessedNvrPredicate;
    private final int maxBuilds;
    private final LoggerHelp logger;
    private static final Logger LOG = LoggerFactory.getLogger(KojiSCM.class);

    BuildMatcher(List<KojiBuildProvider> buildProviders, Predicate<String> notProcessedNvrPredicate, int maxBuilds, LoggerHelp logger) {
        this.buildProviders = buildProviders;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxBuilds = maxBuilds;
        this.logger = logger;
    }

    /**
     * This sorts and filter in following way:
     *
     * exlude list si one item  `2`, max bnuilds is 3
     * 2 1 4 3
     * get sorted
     * 1 2 3 4
     * get cut
     * 1 2 3
     * get filtered
     * 1 3
     *
     * you must filter after limit, otherwise strange builds will go in. The tests are covering this
     */
    public static Stream<Build> listBuilds(BuildMatcher bm, LoggerHelp logger) {
        for (KojiBuildProvider provider : bm.buildProviders) {
            List<Build> builds;
            try {
                builds = bm.getBuilds(provider.getBuildProvider());
            } catch (Exception ex) {
                if (logger != null) {
                    logger.log("", ex);
                    logger.log("Failed to read builds from " + provider.getTopUrl() + ", trying next one");
                }
                LOG.warn("", ex);
                LOG.error("Failed to read builds from " + provider.getTopUrl() + ", trying next one");
                continue;
            }
            return builds.stream()
                    .sorted(BuildMatcher::compare)
                    .limit(bm.maxBuilds)
                    .filter(build -> bm.notProcessedNvrPredicate.test(build.getNvr()));
        }
        throw new RuntimeException("All providers tried, all failed");

    }

    /**
     * From previous javadoc, returns 3
     */
    public static Optional<Build> getLatestOfNewestBuilds(BuildMatcher bm, LoggerHelp logger) {
        final Optional<Build> buildOptional = listBuilds(bm, logger).max(BuildMatcher::compare);
        return buildOptional.map(bm::getBuild);
    }

    public Optional<Build> getBuild() {
        return getLatestOfNewestBuilds(this, logger);
    }

    abstract List<Build> getBuilds(BuildProvider buildProvider);

    abstract Build getBuild(Build build);

    public static int compare(Build b1, Build b2) {
        switch (orderBy) {
            case DATE:
                return compareBuildsByCompletionTime(b1, b2);
            case VERSION:
                return compareBuildVersions(b1, b2);
        }
        throw new RuntimeException("Unknown order");
    }

    private static final RemoteRequestsCache cache = new RemoteRequestsCache(
            RemoteRequestCacheConfigKeys.DEFAULT_CONFIG_LOCATION,
            (url, params) -> new XmlRpcHelper.XmlRpcExecutioner(url).execute(params));

    public static Object execute(String url, XmlRpcRequestParams params) {
        return cache.obtain(url, params);
    }

    public static int compareBuildsByCompletionTime(Build b1, Build b2) {
        return compareKojiTime(b1.getCompletionTime(), b2.getCompletionTime(), Constants.DTF);

    }

    private static int compareKojiTime(String s1, String s2, DateTimeFormatter d) {
        LocalDateTime thisCompletionTime = LocalDateTime.parse(sanitizeBadKojiDate(s1), d);
        LocalDateTime thatCompletionTime = LocalDateTime.parse(sanitizeBadKojiDate(s2), d);
        return thatCompletionTime.compareTo(thisCompletionTime);
    }


    static String sanitizeBadKojiDate(String corruptedDate) {
        if (corruptedDate.contains("+")) {
            return corruptedDate.replaceAll("\\+[0-9]{1,2}:[0-9]{1,2}$", "");
        } else {
            return corruptedDate;
        }
    }

    private static int compareBuildVersions(Build b1, Build b2) {
        // comparing versions:
        int res = compareStrings(b1.getVersion(), b2.getVersion());
        if (res != 0) {
            return res;
        }
        // version are identical, comparing releases:
        return compareStrings(b1.getRelease(), b2.getRelease());
    }

    private static int compareStrings(String s1, String s2) {
        StringTokenizer tokenizer1 = new StringTokenizer(s1, "-.");
        StringTokenizer tokenizer2 = new StringTokenizer(s2, "-.");
        while (tokenizer1.hasMoreTokens() && tokenizer2.hasMoreTokens()) {
            String t1 = tokenizer1.nextToken();
            String t2 = tokenizer2.nextToken();
            if (allDigits(t1) && allDigits(t2)) {
                int i1 = Integer.parseInt(t1);
                int i2 = Integer.parseInt(t2);
                int intCompared = i1 - i2;
                if (intCompared != 0) {
                    return intCompared > 0 ? -1 : 1;
                }
                continue;
            }
            int stringCompared = t1.compareTo(t2);
            if (stringCompared != 0) {
                return stringCompared > 0 ? -1 : 1;
            }
        }
        // if we are here then one of strings has ended,
        // longer will be considered bigger version:
        if (tokenizer1.hasMoreTokens()) {
            return -1;
        }
        if (tokenizer2.hasMoreTokens()) {
            return 1;
        }
        return 0;
    }

    private static boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
