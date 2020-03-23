package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.*;
import org.fakekoji.jobmanager.model.*;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
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
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MatrixGenerator {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final BuildProviderManager buildProviderManager;
    private final PlatformManager platformManager;
    private final TaskVariantManager taskVariantManager;
    private final JDKVersionManager jDKVersionManager;
    private final TaskManager taskManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final JDKProjectManager jdkProjectManager;

    private static final TestEqualityFilter defaultTestFilter = new TestEqualityFilter(true, true, true, true, true);
    private static final BuildEqualityFilter defaultBuildFilter = new BuildEqualityFilter(true, true, true, true, true, true);
    private static final String defaultRegex = ".*";

    private final TestEqualityFilter testFilter;
    private final BuildEqualityFilter buildFilter;
    private final Pattern testRegex;
    private final Pattern buildRgex;


    public MatrixGenerator(AccessibleSettings settings, ConfigManager configManager) {
        this(settings, configManager, defaultRegex, defaultRegex, defaultTestFilter, defaultBuildFilter);

    }

    public MatrixGenerator(AccessibleSettings settings, ConfigManager configManager, String testRegex, String buildRegex) {
        this(settings, configManager, testRegex, buildRegex, defaultTestFilter, defaultBuildFilter);

    }

    public MatrixGenerator(AccessibleSettings settings, ConfigManager configManager, String testRegex, String buildRegex, TestEqualityFilter testEqualityFilter,
            BuildEqualityFilter buildEqualityFilter) {

        final JenkinsJobUpdater jenkinsJobUpdater = new JenkinsJobUpdater(settings);

        this.testFilter = testEqualityFilter;
        this.buildFilter = buildEqualityFilter;
        this.testRegex = Pattern.compile(testRegex == null ? defaultRegex : testRegex);
        this.buildRgex = Pattern.compile(buildRegex == null ? defaultRegex : buildRegex);
        buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
        platformManager = new PlatformManager(configManager.getPlatformStorage(), jenkinsJobUpdater);
        //contains both BUILD and TEST variants
        taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
        jDKVersionManager = new JDKVersionManager(configManager.getJdkVersionStorage());
        taskManager = new TaskManager(configManager.getTaskStorage(), jenkinsJobUpdater);


        jdkTestProjectManager = new JDKTestProjectManager(
                configManager.getJdkTestProjectStorage(),
                jenkinsJobUpdater
        );
        jdkProjectManager = new JDKProjectManager(
                configManager,
                jenkinsJobUpdater,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
    }


    public List<TestSpec> getTests() throws StorageException {
        List<TestSpec> r = new ArrayList<>();
        for (Platform platform : platformManager.readAll()) {
            for (Platform.Provider provider : platform.getProviders()) {
                for (Task task : taskManager.readAll()) {
                    if (!task.getType().equals(Task.Type.TEST)) {
                        TestSpec t = new TestSpec(platform, provider, task, testFilter);
                        if (testRegex.matcher(t.toString()).matches()) {
                            r.add(t);
                        }
                    } else {
                        Collection<Collection<TaskVariantValue>> variants = cartesianProduct(getTasksSets(taskVariantManager.readAll(), task.getType()));
                        for (Collection<TaskVariantValue> tvvs : variants) {
                            TestSpec t = new TestSpec(platform, provider, task, testFilter);
                            for (TaskVariantValue tv : tvvs) {
                                t.addVariant(tv);
                            }
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


    public List<BuildSpec> getBuilds() throws StorageException {
        List<BuildSpec> r = new ArrayList<>();
        for (Platform platform : platformManager.readAll()) {
            for (Platform.Provider provider : platform.getProviders()) {
                for (Project project : concateProjects(jdkProjectManager.readAll(), jdkTestProjectManager.readAll())) {
                    Collection<Collection<TaskVariantValue>> variants = cartesianProduct(getTasksSets(taskVariantManager.readAll(), Task.Type.BUILD));
                    for (Collection<TaskVariantValue> tvvs : variants) {
                        BuildSpec b = new BuildSpec(platform, provider, project, buildFilter);
                        for (TaskVariantValue tv : tvvs) {
                            b.addVariant(tv);
                        }
                        if (buildRgex.matcher(b.toString()).matches()) {
                            r.add(b);
                        }
                    }
                }
            }
        }
        return (List<BuildSpec>) filterByToString(r);
    }

    private Collection<TaskVariantValue>[] getTasksSets(List<TaskVariant> taskVars, Task.Type build) {
        ArrayList<Collection<TaskVariantValue>> r = new ArrayList<>(taskVars.size());
        for (int i = 0; i < taskVars.size(); i++) {
            if (taskVars.get(i).getType().equals(build)) {
                r.add(taskVars.get(i).getVariants().values());
            }
        }
        return r.toArray(new Collection[0]);
    }


    static Collection<Collection<TaskVariantValue>> cartesianProduct(Collection<TaskVariantValue>... sets) {
        if (sets.length < 2) {
            List<Collection<TaskVariantValue>> r = new ArrayList<>(1);
            if (sets.length == 1) {
                r.add(sets[0]);
            }
            return r;
        }
        return cartesianProductImpl(0, sets);
    }

    private static Collection<Collection<TaskVariantValue>> cartesianProductImpl(int index, Collection<TaskVariantValue>... sets) {
        List<Collection<TaskVariantValue>> ret = new ArrayList<>();
        if (index == sets.length) {
            ret.add(new ArrayList<>());
        } else {
            for (TaskVariantValue obj : sets[index]) {
                for (Collection<TaskVariantValue> set : cartesianProductImpl(index + 1, sets)) {
                    set.add(obj);
                    ret.add(set);
                }
            }
        }
        return ret;
    }

    private List<Project> concateProjects(List<? extends Project>... list) {
        List<Project> r = new ArrayList<>();
        for (List<? extends Project> l : list) {
            r.addAll(l);
        }
        return r;
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
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < l) {
            sb.append(by);
        }
        return sb.toString();
    }


    public String printMatrix(int orientation, boolean dropRows, boolean dropColumns) throws StorageException, ManagementException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        List<BuildSpec> bs = getBuilds();
        List<TestSpec> ts = getTests();
        try {
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                if (orientation <= 0) {
                    int t1 = printMatrix(ps, bs, ts, dropRows, dropColumns);
                    ps.println(t1 + "/" + (bs.size() * ts.size()));
                }
                if (orientation >= 0) {
                    int t2 = printMatrix(ps, ts, bs, dropRows, dropColumns);
                    ps.println(t2 + "/" + (bs.size() * ts.size()));
                }
            }
            return baos.toString(utf8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    int printMatrix(PrintStream p, Collection<? extends Spec> rows, Collection<? extends Spec> columns, boolean dropRows, boolean dropColumns) throws ManagementException, StorageException {
        int lrow = getLongest(rows) + 1;
        int lcol = getLongest(columns) + 1;
        int total = 0;

        List<List<List<Leaf>>> matrix = generateMatrix(rows, columns, dropRows, dropColumns);
        for (int i = 0; i < matrix.size(); i++) {
            List<List<Leaf>> row = matrix.get(i);
            for (int j = 0; j < row.size(); j++) {
                List<Leaf> cel = row.get(j);
                String s;
                if (cel.size() == 1 && cel.get(0) instanceof LeafTitle) {
                    s = cel.get(0).toString();
                } else {
                    s = "" + cel.size();
                    total += cel.size();
                }
                int align = lcol;
                if (j == 0 || j == row.size() - 1) {
                    align = lrow;
                }
                if (s.isEmpty()) {
                    //filing by soething, as leading spaces can be trimmed on the fly
                    p.print(fill(s, align - 1, "-"));
                    p.print(" ");
                } else {
                    p.print(fill(s, align, " "));
                }
            }
            p.println();
        }
        return total;
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
        for (Project project : concateProjects(jdkProjectManager.readAll(), jdkTestProjectManager.readAll())) {
            if (project instanceof JDKTestProject) {
                TestJobConfiguration jc = ((JDKTestProject) project).getJobConfiguration();
                List<BuildPlatformConfig> buildPlatformConfig = jc.getPlatforms();
                for (BuildPlatformConfig bpce : buildPlatformConfig) {
                    TaskConfig tc = new TaskConfig(null, bpce.getVariants());
                    List<TaskConfig> taskConfigs = new ArrayList<>();
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
        Collection<String> buildVars = bvc.getMap().values();
        String fullBuild = "" +
                buildOsAarch[0] + "." + buildOsAarch[1] + "." + buildProvider + "-" +
                btask + "-" +
                platformVersion + "-" +
                projectName + "-" +
                String.join(".", buildVars);
        //System.out.println(fullBuild);
        if (ts.getTask().getId().equals("build")) { //where it get from?
            //this is trap. We are checking, whether the build is actually building in gvem combination, first the testsuite must moreover match, then it must "itself"
            if (genericMatcher(ts, buildOsAarch[0], buildOsAarch[1], buildProvider, new ArrayList<>(0))) {
                if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)) {
                    if (btask == null) {//test only jobs
                        //we add test only job only in case, it actualy have any test, testonly jobs without tests do not have sense?
                        //but we can see it later in matrix that it do nothave run any tests
                        counter.add(new Leaf(fullBuild));
                    } else {
                        counter.add(new Leaf(fullBuild));
                    }
                }
            }
        } else {
            if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)) {
                iterateBuildVariantsConfig(bs, ts, counter, project, buildArchOs, buildProvider, btce, bvc, fullBuild);
            }
        }
    }

    private void iterateBuildVariantsConfig(BuildSpec bs, TestSpec ts, List<Leaf> counter, Project project, String buildArchOs, String buildProvider, TaskConfig btce,
            VariantsConfig bvc, String tmpBuildIdForSimpleTextIdentification) {
        for (PlatformConfig tpce : bvc.getPlatforms()) {
            for (TaskConfig ttce : tpce.getTasks()) {
                for (VariantsConfig tvc : ttce.getVariants()) {
                    String[] testOsAarch = tpce.getId().split("\\.");
                    String testProvider = tpce.getProvider();
                    String ttask = ttce.getId();
                    Collection<String> testVars = tvc.getMap().values();
                    String fullTest = "" +
                            testOsAarch[0] + "." + testOsAarch[1] + "." + testProvider + "-" +
                            ttask + "-" +
                            String.join(".", testVars);
                    //System.out.println(fullTest);
                    String full = "" +
                            tmpBuildIdForSimpleTextIdentification + "-" +
                            fullTest;
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
