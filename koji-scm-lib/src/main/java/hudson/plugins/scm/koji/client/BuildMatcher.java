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
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildMatcher {

    public static enum OredBy {

        DATE, VERSION
    }

    private final String currentURL;
    private final Predicate<String> notProcessedNvrPredicate;
    private final GlobPredicate tagPredicate;
    private final int maxBuilds;
    private final String pkgName;
    private final String arch;
    private static final OredBy orderBy = OredBy.DATE;

    public BuildMatcher(String currentURL, Predicate<String> notProcessedNvrPredicate, GlobPredicate tagPredicate, int maxBuilds, String pkgName, String arch) {
        this.currentURL = currentURL;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxBuilds = maxBuilds;
        this.pkgName = pkgName;
        this.arch = arch;
        this.tagPredicate = tagPredicate;
    }

    public Optional<Build> getResult() {
        Stream<Build> results = listMatchingBuilds();

        Optional<Build> buildOpt = results
                .filter(b -> notProcessedNvrPredicate.test(b.getNvr()))
                .findFirst();
        return buildOpt;
    }

    public Object[] getAll() {
        Stream<Build> results = listMatchingBuilds();

        Object[] buildOpt = results
                .filter(b -> notProcessedNvrPredicate.test(b.getNvr()))
                .toArray();
        return buildOpt;
    }

    private Stream<Build> listMatchingBuilds() {
        List<Object> builds = Arrays.stream(pkgName.split(" "))
                .map(this::listPackageBuilds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (builds == null || builds.isEmpty()) {
            return Stream.empty();
        }
        // ok, obvious over-engineering here:
        return builds
                .stream()
                .sequential()
                // sorting first, to go with relevant results first:
                .sorted(this::compare)
                // getting tags per build and filtering by tags right away:
                .map(this::retrieveTags)
                .filter(this::filterByTags)
                // getting rpms and filtering by arch right away:
                .map(this::retrieveRPMs)
                .map(this::retrieveArchives)
                .filter(this::filterByArch)
                // do not go too far away into the past:
                .limit(maxBuilds)
                // composing final stream of builds:
                .map(this::toBuild)
                // sorting in reverse order:
                .sorted(Comparator.reverseOrder());
    }

    private List<Object> listPackageBuilds(String packageName) {
        Integer packageId = (Integer) execute(Constants.getPackageID, packageName);
        if (packageId == null) {
            return Collections.emptyList();
        }

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.packageID, packageId);
        paramsMap.put("state", 1);
        paramsMap.put("__starstar", Boolean.TRUE);

        Object[] results = (Object[]) execute(Constants.listBuilds, paramsMap);
        if (results == null || results.length < 1) {
            return Collections.emptyList();
        }
        return Arrays.asList(results);
    }

    private Object retrieveTags(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }

        Map m = (Map) o;
        //Object buildName = m.get(nvr);

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.build, m.get(Constants.build_id));
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute(Constants.listTags, paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            m.put("tags", res);
        }
        return o;
    }

    private boolean filterByTags(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;
        Object[] tags = (Object[]) m.get("tags");
        if (tags == null) {
            return false;
        }
        boolean tagMatch = Arrays
                .stream(tags)
                .map(t -> ((Map<String, String>) t).get(Constants.name))
                .anyMatch(tagPredicate);
        return tagMatch;
    }

    private Object retrieveRPMs(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.buildID, m.get(Constants.build_id));
        String[] arches = composeArchesArray();
        if (arches != null) {
            paramsMap.put(Constants.arches, arches);
        }
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute(Constants.listRPMs, paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            //the map already have tag rpms, but for ALL architectures. Replacing.
            //also this is called BEFORE retrieveArchives so we can put instead of add
            m.put(Constants.rpms, res);
        }

        return o;
    }

    /**
     * Archives are stored under {@link Constants#rpms} key, together with RPMs.
     * <p>
     * Name, Version and Release are not received with info about archive. We
     * need to get it from the build. Arch is taken from configuration and is
     * later used to compose filepath. Unlike with RPMs, filename is received
     * here so we can store it.
     */
    private Object retrieveArchives(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(Constants.buildID, m.get(Constants.build_id));
        String[] desiredArches = composeArchesArray();
        List<String> supportedArches = new ArrayList<>(1);
        supportedArches.add("win");
        paramsMap.put("__starstar", Boolean.TRUE);

        Object listedArchives = execute(Constants.listArchives, paramsMap);
        List<Object> filteredArchives = new ArrayList<>();
        if (listedArchives != null && (listedArchives instanceof Object[])) {
            for (Object archive : (Object[]) listedArchives) {
                if (archive != null && archive instanceof Map) {
                    for (String arch : desiredArches) {
                        Map<String, Object> rpms = new HashMap<>();
                        rpms.putAll((Map<String, Object>) archive);
                        rpms.put(Constants.name, m.get(Constants.name));
                        rpms.put(Constants.version, m.get(Constants.version));
                        rpms.put(Constants.release, m.get(Constants.release));
                        rpms.put(Constants.arch, arch);
                        rpms.put(Constants.nvr, ((Map) archive).get(Constants.filename));
                        rpms.put(Constants.filename, ((Map) archive).get(Constants.filename));
                        if (supportedArches.contains(arch)) {
                            filteredArchives.add(rpms);
                        }
                    }
                }
            }

            addRpmsToMap(m, filteredArchives.toArray());
        }

        return o;
    }

    private void addRpmsToMap(Map m, Object[] listedArchivesArray) {
        addToMap(Constants.rpms, m, listedArchivesArray);
    }

    private void addToMap(String key, Map m, Object[] listedArchivesArray) {
        Object orig = m.put(key, listedArchivesArray);
        if (orig != null && orig instanceof Object[]) {
            Object[] listedOrigianlArray = (Object[]) orig;
            Object[] connectedArrays = new Object[listedOrigianlArray.length + listedArchivesArray.length];
            System.arraycopy(listedOrigianlArray, 0, connectedArrays, 0, listedOrigianlArray.length);
            System.arraycopy(listedArchivesArray, 0, connectedArrays, listedOrigianlArray.length, listedArchivesArray.length);
            m.put(key, connectedArrays);
        }
    }

    private boolean filterByArch(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        /*        ??        */
//        if (arch == null || arch.isEmpty()) {
//            throw new RuntimeException("Arch cannot be empty");
//        }
        Map m = (Map) o;
        boolean hasRpms = m.get(Constants.rpms) != null;
        return hasRpms;
    }

    private Build toBuild(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }

        Map<String, String> m = (Map) o;
        return new Build((Integer) ((Map) o).get(Constants.build_id),
                m.get(Constants.name),
                m.get(Constants.version),
                m.get(Constants.release),
                m.get(Constants.nvr),
                dateKojiToIso(m.get(Constants.completion_time)),
                Arrays
                .stream((Object[]) ((Map) o).get(Constants.rpms))
                .map(this::toRPM)
                .collect(Collectors.toList()),
                Arrays
                .stream((Object[]) ((Map) o).get("tags"))
                .map(t -> ((Map<String, String>) t).get(Constants.name))
                .collect(Collectors.toSet()), null
        );
    }

    private RPM toRPM(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map<String, String> m = (Map) o;
        return new RPM(
                m.get(Constants.name),
                m.get(Constants.version),
                m.get(Constants.release),
                m.get(Constants.nvr),
                m.get(Constants.arch),
                m.get(Constants.filename));
    }

    private String dateKojiToIso(String kojiDate) {
        LocalDateTime date = LocalDateTime.parse(kojiDate, Constants.DTF);
        return DateTimeFormatter.ISO_DATE_TIME.format(date);
    }

    private int compare(Object o1, Object o2) {
        switch (orderBy) {
            case DATE:
                return compareBuildsByCompletionTime(o1, o2);
            case VERSION:
                return compareBuildVersions(o1, o2);
        }
        throw new RuntimeException("Unknown order");
    }

    private String[] composePkgsArray() {
        return composeArray(pkgName);
    }

    private String[] composeArchesArray() {
        return composeArray(arch);
    }

    protected Object execute(String methodName, Object... args) {
        return new XmlRpcHelper.XmlRpcExecutioner(currentURL).execute(methodName, args);
    }

    private static String[] composeArray(String values) {
        if (values == null || values.trim().isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(values, ",;\n\r\t ");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public static int compareBuildsByCompletionTime(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            // o2 is not null:
            return 1;
        }
        // o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // both o1 and o2 are not null:
        if (!(o1 instanceof Map) || !(o2 instanceof Map)) {
            throw new RuntimeException("Expected Map instances, got: " + o1 + " and " + o2);
        }
        Map<String, Object> m1 = (Map) o1;
        Map<String, Object> m2 = (Map) o2;
        return comapreKojiTime((String) m1.get("completion_time"), (String) m2.get("completion_time"), Constants.DTF);

    }

    public static int compareBuildsByCompletionTime(Build o1, Build o2) {
        return comapreKojiTime(o1.getCompletionTime(), o2.getCompletionTime(), Constants.DTF2);
    }
    public static int comapreKojiTime(String s1, String s2, DateTimeFormatter d) {
        LocalDateTime thisCompletionTime = LocalDateTime.parse(s1, d);
        LocalDateTime thatCompletionTime = LocalDateTime.parse(s2, d);
        return thatCompletionTime.compareTo(thisCompletionTime);
    }

    public static int compareBuildVersions(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            // o2 is not null:
            return 1;
        }
        // o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // both o1 and o2 are not null:
        if (!(o1 instanceof Map) || !(o2 instanceof Map)) {
            throw new RuntimeException("Expected Map instances, got: " + o1 + " and " + o2);
        }
        Map<String, String> m1 = (Map) o1;
        Map<String, String> m2 = (Map) o2;

        // comparing versions:
        int res = compareStrings(m1.get(Constants.version), m2.get(Constants.version));
        if (res != 0) {
            return res;
        }
        // version are identical, comparing releases:
        return compareStrings(m1.get(Constants.release), m2.get(Constants.release));
    }

    public static int compareStrings(String o1, String o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return 1;
        }
        // if we are here - o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // if we are here - none of the arguments is null:
        StringTokenizer tokenizer1 = new StringTokenizer(o1, "-.");
        StringTokenizer tokenizer2 = new StringTokenizer(o2, "-.");
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

    public static boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
