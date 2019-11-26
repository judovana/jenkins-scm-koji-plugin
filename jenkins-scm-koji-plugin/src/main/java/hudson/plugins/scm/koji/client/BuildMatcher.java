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
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

abstract class BuildMatcher {

    public enum OrderBy {
        DATE, VERSION
    }

    private static final OrderBy orderBy = OrderBy.DATE;

    private final Iterable<KojiBuildProvider> buildProviders;
    private final Predicate<String> notProcessedNvrPredicate;
    private final int maxBuilds;

    BuildMatcher(
            Iterable<KojiBuildProvider> buildProviders,
            Predicate<String> notProcessedNvrPredicate,
            int maxBuilds
    ) {
        this.buildProviders = buildProviders;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxBuilds = maxBuilds;
    }

    public Optional<Build> getBuild() {
        final Optional<Build> buildOptional = StreamSupport.stream(buildProviders.spliterator(), false)
                .map(KojiBuildProvider::getBuildProvider)
                .map(this::getBuilds)
                .flatMap(Collection::stream)
                .sorted(this::compare)
                .limit(maxBuilds)
                .filter(build -> notProcessedNvrPredicate.test(build.getNvr()))
                .max(this::compare);
        return buildOptional.map(this::getBuild);
    }

    abstract List<Build> getBuilds(BuildProvider buildProvider);

    abstract Build getBuild(Build build);

    int compare(Build b1, Build b2) {
        switch (orderBy) {
            case DATE:
                return compareBuildsByCompletionTime(b1, b2);
            case VERSION:
                return compareBuildVersions(b1, b2);
        }
        throw new RuntimeException("Unknown order");
    }

    protected Object execute(String url, XmlRpcRequestParams params) {
        return new XmlRpcHelper.XmlRpcExecutioner(url).execute(params);
    }

    public static int compareBuildsByCompletionTime(Build b1, Build b2) {
        return compareKojiTime(b1.getCompletionTime(), b2.getCompletionTime(), Constants.DTF);

    }

    private static int compareKojiTime(String s1, String s2, DateTimeFormatter d) {
        LocalDateTime thisCompletionTime = LocalDateTime.parse(s1, d);
        LocalDateTime thatCompletionTime = LocalDateTime.parse(s2, d);
        return thatCompletionTime.compareTo(thisCompletionTime);
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
