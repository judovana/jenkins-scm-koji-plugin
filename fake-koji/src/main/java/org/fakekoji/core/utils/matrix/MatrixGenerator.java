package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.model.BuildPlatformConfig;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobConfiguration;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TestJobConfiguration;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MatrixGenerator {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final TestEqualityFilter defaultTestFilter = new TestEqualityFilter(true, true, true, true, true);
    private static final BuildEqualityFilter defaultBuildFilter = new BuildEqualityFilter(true, true, true, true, true, true);
    private static final String defaultRegex = ".*";

    private final TestEqualityFilter testFilter;
    private final BuildEqualityFilter buildFilter;
    private final Pattern testRegex;
    private final Pattern buildRgex;
    private final String[] project;
    private final ConfigCache cache;


    public MatrixGenerator(final ConfigManager configManager, String[] project) {
        this(configManager, defaultRegex, defaultRegex, defaultTestFilter, defaultBuildFilter, project);

    }

    public MatrixGenerator(final ConfigManager configManager, String testRegex, String buildRegex, String[] project) {
        this(configManager, testRegex, buildRegex, defaultTestFilter, defaultBuildFilter, project);

    }

    public MatrixGenerator(
            final ConfigManager configManager,
            String testRegex,
            String buildRegex,
            TestEqualityFilter testEqualityFilter,
            BuildEqualityFilter buildEqualityFilter,
            String[] project
    ) {

        this.testFilter = testEqualityFilter;
        this.buildFilter = buildEqualityFilter;
        this.testRegex = Pattern.compile(testRegex == null ? defaultRegex : testRegex);
        this.buildRgex = Pattern.compile(buildRegex == null ? defaultRegex : buildRegex);

        this.project = project;
        if (project != null) {
            Arrays.sort(project);
        }

        try {
            cache = new ConfigCache(configManager);
        } catch (StorageException se) {
            throw new RuntimeException(se);
        }

    }

    private List<List<String>> getTaskVariantValuesProduct(Collection<TaskVariant> taskVariants) {
        return cartesianProduct(
                taskVariants
                        .stream()
                        .map(variant -> variant.getVariants()
                                .values()
                                .stream()
                                .map(TaskVariantValue::getId)
                                .collect(Collectors.toList())
                        )
                        .collect(Collectors.toList())
        );
    }

    public List<TestSpec> getTests() {
        final List<List<String>> variantsProducts = getTaskVariantValuesProduct(cache.getTestTaskVariants());
        List<TestSpec> r = new ArrayList<>();
        for (Platform origPlatform : cache.getPlatforms()) {
            Platform platform = clonePlatformForProviders(origPlatform);
            for (Platform.Provider provider : platform.getProviders()) {
                for (Task task : cache.getTasks()) {
                    if (!task.getType().equals(Task.Type.TEST)) {
                        TestSpec t = new TestSpec(platform, provider, task, testFilter);
                        if (testRegex.matcher(t.toString()).matches()) {
                            r.add(t);
                        }
                    } else {
                        for (List<String> variantsProduct : variantsProducts) {
                            TestSpec t = new TestSpec(platform, provider, task, variantsProduct, testFilter);
                            if (testRegex.matcher(t.toString()).matches()) {
                                r.add(t);
                            }
                        }
                    }
                }
            }
        }
        return (List<TestSpec>) filterByToString(r);
    }

    private Platform clonePlatformForProviders(Platform origPlatform) {
        if (origPlatform.getProviders().isEmpty()) {
            return new Platform(
                    origPlatform.getId(),
                    origPlatform.getOs(),
                    origPlatform.getVersion(),
                    origPlatform.getVersionNumber(),
                    origPlatform.getArchitecture(),
                    origPlatform.getKojiArch().orElse(null),
                    createNoneProviderList(),
                    origPlatform.getVmName(),
                    origPlatform.getTestingYstream(),
                    origPlatform.getStableZstream(),
                    origPlatform.getTags(),
                    origPlatform.getVariables());
        } else {
            return origPlatform;
        }
    }

    private List<Platform.Provider> createNoneProviderList() {
        return Arrays.asList(createNoneProvider());
    }

    private Platform.Provider createNoneProvider() {
        return new Platform.Provider(
                "noProvider",
                Collections.emptyList(),
                Collections.emptyList());
    }

    private List<? extends Spec> filterByToString(List<? extends Spec> r) {
        for (int i = 0; i < r.size(); i++) {
            for (int j = i + 1; j < r.size(); j++) {
                if (r.get(i).toString().equals(r.get(j).toString())) {
                    r.remove(j);
                    j--;
                }
            }
        }
        return Collections.unmodifiableList(r);
    }


    public List<BuildSpec> getBuilds() {
        final List<List<String>> variantsProducts = getTaskVariantValuesProduct(cache.getBuildTaskVariants());
        final List<BuildSpec> r = new ArrayList<>();
        for (Platform origPlatform : cache.getPlatforms()) {
            Platform platform = clonePlatformForProviders(origPlatform);
            for (Platform.Provider provider : platform.getProviders()) {
                for (Project project : cache.getProjects())
                    if (matchProject(project.getId())) {
                        for (List<String> variantProduct : variantsProducts) {
                            BuildSpec b = new BuildSpec(platform, provider, project, variantProduct, buildFilter);
                            if (buildRgex.matcher(b.toString()).matches()) {
                                r.add(b);
                            }
                        }
                    }
            }
        }
        return (List<BuildSpec>) filterByToString(r);
    }

    private boolean matchProject(String project) {
        if (this.project == null || this.project.length == 0) {
            return true;
        }
        for (String p : this.project) {
            if (p.equals(project)) {
                return true;
            }
        }
        return false;
    }

    static List<List<String>> cartesianProduct(List<List<String>> values) {
        if (values.size() < 2) {
            return new ArrayList<>(values);
        }
        return cartesianProductImpl(0, values);
    }

    private static List<List<String>> cartesianProductImpl(int index, List<List<String>> values) {
        List<List<String>> ret = new ArrayList<>();
        if (index == values.size()) {
            ret.add(new ArrayList<>());
        } else {
            for (String obj : values.get(index)) {
                for (List<String> set : cartesianProductImpl(index + 1, values)) {
                    set.add(0, obj); // needs to be prepended so the order of variants is preserved
                    ret.add(set);
                }
            }
        }
        return ret;
    }

    static int getLongest(Iterable<? extends Spec> l) {
        int max = Integer.MIN_VALUE;
        for (Object o : l) {
            if (o.toString().length() > max) {
                max = o.toString().length();
            }
        }
        return max;
    }

    static String fill(String s, int l) {
        return fill(s, l, " ");
    }

    static String fill(String s, int l, String by) {
        StringBuilder filledString = new StringBuilder(s);
        while (filledString.length() < l) {
            filledString.append(by);
        }
        return filledString.toString();
    }


    public String printMatrix(int orientation, boolean dropRows, boolean dropColumns, TableFormatter tf) throws StorageException, ManagementException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        List<BuildSpec> bs = getBuilds();
        List<TestSpec> ts = getTests();
        try {
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                if (orientation <= 0) {
                    printMatrix(ps, bs, ts, dropRows, dropColumns, tf);
                }
                if (orientation >= 0) {
                    printMatrix(ps, ts, bs, dropRows, dropColumns, tf);
                }
            }
            return baos.toString(utf8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void printMatrix(PrintStream p, Collection<? extends Spec> rows, Collection<? extends Spec> columns, boolean dropRows, boolean dropColumns, TableFormatter tf) throws ManagementException, StorageException {
        int lrow = getLongest(rows) + 1;
        int lcol = getLongest(columns) + 1;
        int total = 0;
        p.print(tf.tableStart());
        List<List<List<Leaf>>> matrix = generateMatrix(rows, columns, dropRows, dropColumns);
        for (int i = 0; i < matrix.size(); i++) {
            List<List<Leaf>> row = matrix.get(i);
            p.print(tf.rowStart());
            for (int j = 0; j < row.size(); j++) {
                int maxForSpan = 1;
                for (int ii = 0; ii < matrix.size(); ii++) {
                    List<List<Leaf>> rowForSpan = matrix.get(ii);
                    maxForSpan = Math.max(rowForSpan.get(j).size(), maxForSpan);
                }
                List<Leaf> cel = row.get(j);
                String cellContent;
                if (cel.size() == 1 && cel.get(0) instanceof LeafTitle) {
                    cellContent = cel.get(0).toString();
                } else {
                    cellContent = tf.getContext(cel, maxForSpan);
                    total += cel.size();
                }
                int align = lcol;
                if (j == 0 || j == row.size() - 1) {
                    align = lrow;
                }
                if (cellContent.isEmpty()) {
                    if (i == 0 && (j == 0 || j == row.size() - 1)) {
                        cellContent = tf.initialCell(project);
                    } else if (i == matrix.size() - 1 && (j == 0 || j == row.size() - 1)) {
                        cellContent = tf.lastCell(total, rows.size() * columns.size());
                    } else {
                        //filing by soething, as leading spaces can be trimmed on the fly
                        cellContent = fill(cellContent, align - 1, "-") + " ";
                    }
                }
                if (cel.size() == 1 && cel.get(0) instanceof LeafTitle) {
                    p.print(tf.cellStart(maxForSpan) + fill(cellContent, align, " ") + tf.cellEnd());
                } else {
                    p.print(tf.cellStart() + fill(cellContent, align, " ") + tf.cellEnd());
                }
            }
            p.println(tf.rowEnd());
        }
        p.print(tf.tableEnd());
    }


    private List<List<List<Leaf>>> generateMatrix(Collection<? extends Spec> rows, Collection<? extends Spec> columns, boolean dropRows, boolean dropColumns)
            throws ManagementException, StorageException {
        //last list is content of single cel
        List<List<List<Leaf>>> listOfRows = new ArrayList<>(rows.size() + 2/*rows headers are additional first and last column*/);

        List<List<Leaf>> initialRow = new ArrayList<>(columns.size() + 2);
        initialRow.add(Arrays.asList(new LeafTitle("")));//initial empty intersection, upper left corner
        for (Spec t : columns) {
            initialRow.add(Arrays.asList(new LeafTitle(t.toString())));
        }
        initialRow.add(initialRow.get(0));//last empty intersection, upper right corner, reusing
        listOfRows.add(initialRow);
        for (Spec b : rows) {
            List<List<Leaf>> row = new ArrayList<>(columns.size() + 2);
            row.add(Arrays.asList(new LeafTitle(b.toString())));
            for (Spec t : columns) {
                List<Leaf> matched = countMatchingProjects(b, t);
                row.add(matched);
            }
            row.add(row.get(0)); //again ending by rowtitle, why not reuse
            listOfRows.add(row);
        }
        listOfRows.add(new ArrayList<>(initialRow)); //and last row are again only titles, not reusing, because of column delete

        if (dropRows) {
            //not dropping first and last with headers
            for (int i = 1; i < listOfRows.size() - 1; i++) {
                List<List<Leaf>> row = listOfRows.get(i);
                int total = 0;
                //same skip here, thus skipping the instance of checks
                for (int j = 1; j < row.size() - 1; j++) {
                    total = total + row.get(j).size();
                }
                if (total == 0) {
                    listOfRows.remove(i);
                    i--;
                }
            }
        }

        if (dropColumns) {
            //not dropping first and last with headers
            //crawling columns
            for (int j = 1; j < listOfRows.get(0).size() - 1; j++) {
                int total = 0;
                //same skip here, thus skipping the instance of checks
                //crawling rows
                for (int i = 1; i < listOfRows.size() - 1; i++) {
                    total = total + listOfRows.get(i).get(j).size();
                }
                if (total == 0) {
                    deleteClumn(listOfRows, j);
                    j--;
                }
            }
        }
        return listOfRows;
    }

    private static void deleteClumn(List<List<List<Leaf>>> listOfRows, int j) {
        for (List<List<Leaf>> row : listOfRows) {
            row.remove(j);
        }
    }

    private List<Leaf> countMatchingProjects(Spec b, Spec t) throws StorageException, ManagementException {
        BuildSpec bs = null;
        TestSpec ts = null;
        if (b instanceof BuildSpec) {
            bs = (BuildSpec) b;
        }
        if (t instanceof BuildSpec) {
            bs = (BuildSpec) t;
        }
        if (b instanceof TestSpec) {
            ts = (TestSpec) b;
        }
        if (t instanceof TestSpec) {
            ts = (TestSpec) t;
        }
        if (bs == null | ts == null) {
            throw new StorageException("Only build x test ot test x build can be searched for, nothing else");
        }
        List<Leaf> counter = new ArrayList<>();
        //do not optimise, will break the compression of attributes
        for (Project project : cache.getProjects())
            if (matchProject(project.getId())) {
                if (project instanceof JDKTestProject) {
                    TestJobConfiguration jc = ((JDKTestProject) project).getJobConfiguration();
                    Set<BuildPlatformConfig> buildPlatformConfig = jc.getPlatforms();
                    for (BuildPlatformConfig bpce : buildPlatformConfig) {
                        TaskConfig tc = new TaskConfig(null, bpce.getVariants());
                        Set<TaskConfig> taskConfigs = new HashSet<>();
                        taskConfigs.add(tc);
                        for (TaskConfig btce : taskConfigs) {
                            for (VariantsConfig bvc : btce.getVariants()) {
                                checkAndIterateBuild(bs, ts, counter, project, bpce.getId(), null, btce, bvc);
                            }
                        }
                    }
                } else if (project instanceof JDKProject) {
                    JobConfiguration jc = ((JDKProject) project).getJobConfiguration();
                    for (PlatformConfig bpce : jc.getPlatforms()) {
                        for (TaskConfig btce : bpce.getTasks()) {
                            for (VariantsConfig bvc : btce.getVariants()) {
                                checkAndIterateBuild(bs, ts, counter, project, bpce.getId(), bpce.getProvider(), btce, bvc);
                            }
                        }
                    }
                } else {
                    throw new ManagementException("Unknow project type " + project.getClass());
                }
            }
        return counter;
    }

    private void checkAndIterateBuild(BuildSpec bs, TestSpec ts, List<Leaf> counter, Project project, String buildArchOs, String buildProvider, TaskConfig btce,
                                      VariantsConfig bvc) {
        //builds must be counted here, otherwise they a) multiply b) do not exists for project less leaves(so building wthout testing)
        String[] buildOsAarch = buildArchOs.split("\\.");
        String btask = btce.getId(); //always build
        String platformVersion = project.getProduct().getJdk();
        String projectName = project.getId();
        final String variants = bvc.concatVariants(cache);
        Collection<String> buildVars = bvc.getMap().values();
        String fullBuild = "" + //warning, is used for job url generation
                btask + "-" +
                platformVersion + "-" +
                projectName + "-" +
                buildOsAarch[0] + "." + buildOsAarch[1] + "." + buildProvider + "-" +
                variants;
        String providerLessFutureTaskFullBuild = "" + //warning, is used for job url generation
                platformVersion + "-" +
                projectName + "-" +
                buildOsAarch[0] + "." + buildOsAarch[1] + "-" +
                variants;
        //System.out.println(fullBuild);
        if (ts.getTask().getId().equals("build")) { //where it get from?
            //this is trap. We are checking, whether the build is actually building in gvem combination, first the testsuite must moreover match, then it must "itself"
            if (genericMatcher(ts, buildOsAarch[0], buildOsAarch[1], buildProvider, new ArrayList<>(0))) {
                if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)) {
                    if (btask == null) {//test only jobs
                        //we add test only job only in case, it actualy have any test, testonly jobs without tests do not have sense?
                        //but we can see it later in matrix that it do nothave run any tests
                        if (project instanceof JDKTestProject) {
                            counter.addAll(createProvidersUrls((JDKTestProject) project, fullBuild));
                        } else {
                            counter.add(new Leaf(fullBuild));
                        }
                    } else {
                        if (project instanceof JDKTestProject) {
                            counter.addAll(createProvidersUrls((JDKTestProject) project, fullBuild));
                        } else {
                            counter.add(new Leaf(fullBuild));
                        }
                    }
                }
            }
        } else {
            if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)) {
                iterateBuildVariantsConfig(bs, ts, counter, project, buildArchOs, buildProvider, btce, bvc, providerLessFutureTaskFullBuild);
            }
        }
    }

    private Collection<Leaf> createProvidersUrls(JDKTestProject project, String fallBack) {
        List<Leaf> r = new ArrayList<>();
        for (String providerId : project.getBuildProviders()) {
            BuildProvider buildProvider = getProvider(providerId);
            if (buildProvider == null) {
                r.add(new Leaf(fallBack));
            } else {
                if (buildProvider.getTopUrl().endsWith("hub") || buildProvider.getTopUrl().endsWith("hub/")) {
                    String nwTopUrl = buildProvider.getTopUrl();
                    if (buildProvider.getTopUrl().endsWith("hub")) {
                        nwTopUrl = nwTopUrl.substring(0, nwTopUrl.length() - 3);
                    } else if (buildProvider.getTopUrl().endsWith("hub/")) {
                        nwTopUrl = nwTopUrl.substring(0, nwTopUrl.length() - 4);
                    }
                    nwTopUrl = nwTopUrl.replace("hub", "web");
                    r.add(new Leaf(nwTopUrl + "/search?match=glob&type=package&terms=" + project.getProduct().getPackageName()));
                } else {
                    r.add(new Leaf(buildProvider.getDownloadUrl() + "/" + project.getProduct().getPackageName()));
                }
            }
        }
        return r;
    }

    private BuildProvider getProvider(String providerId) {
        for (BuildProvider buildProvider : cache.getBuildProviders()) {
            if (buildProvider.getId().equals(providerId)) {
                return buildProvider;
            }
        }
        return null;
    }

    private void iterateBuildVariantsConfig(BuildSpec bs, TestSpec ts, List<Leaf> counter, Project project, String buildArchOs, String buildProvider, TaskConfig btce,
                                            VariantsConfig bvc, String tmpBuildIdForSimpleTextIdentification) {
        for (PlatformConfig tpce : bvc.getPlatforms()) {
            for (TaskConfig ttce : tpce.getTasks()) {
                for (VariantsConfig tvc : ttce.getVariants()) {
                    String[] testOsAarch = tpce.getId().split("\\.");
                    String testProvider = tpce.getProvider();
                    String ttask = ttce.getId();
                    final String variants = tvc.concatVariants(cache);
                    Collection<String> testVars = tvc.getMap().values();
                    String full = "" + //warning, is used for job url generation
                            ttask + "-" +
                            tmpBuildIdForSimpleTextIdentification + "-" +
                            testOsAarch[0] + "." + testOsAarch[1] + "." + testProvider + "-" +
                            variants;
                    if (taskMatcher(ts, ttask, testOsAarch[0], testOsAarch[1], testProvider, testVars)) {
                        counter.add(new Leaf(full));
                    }
                }
            }
        }
    }


    private static boolean genericMatcher(Spec s, String os, String arch, String provider, Collection<String> variants) {
        return (s.matchOs(os)) &&
                (s.matchArch(arch)) &&
                (s.matchProvider(provider)) &&
                (s.matchVars(variants));
    }

    private static boolean buildMatcher(BuildSpec bs, String projectId, String os, String arch, String provider, String jdk, Collection<String> variants) {
        return (bs.matchProject(projectId) &&
                bs.matchJdk(jdk) &&
                genericMatcher(bs, os, arch, provider, variants));
    }

    private static boolean taskMatcher(TestSpec ts, String taskId, String os, String arch, String provider, Collection<String> variants) {
        return (ts.matchSuite(taskId)) &&
                genericMatcher(ts, os, arch, provider, variants);
    }

    public static class Leaf {

        //TODO: replace by soem structure or other way from which
        //will be able to generate jenkins url or the job itself
        private final String fullPath;

        public Leaf(String fullPath) {
            this.fullPath = fullPath;
        }

        @Override
        public String toString() {
            return fullPath;
        }
    }

    private static class LeafTitle extends Leaf {
        final String simpleTitle;

        LeafTitle(String title) {
            super(null);
            simpleTitle = title;
        }

        @Override
        public String toString() {
            return simpleTitle;
        }
    }
}
