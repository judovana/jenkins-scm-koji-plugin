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

    public MatrixGenerator(AccessibleSettings settings, ConfigManager configManager, String testRegex, String buildRegex, TestEqualityFilter testEqualityFilter, BuildEqualityFilter buildEqualityFilter) {

        final JenkinsJobUpdater jenkinsJobUpdater = new JenkinsJobUpdater(settings);

        this.testFilter = testEqualityFilter;
        this.buildFilter = buildEqualityFilter;
        this.testRegex = Pattern.compile(testRegex);
        this.buildRgex = Pattern.compile(buildRegex);
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
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < l) {
            sb.append(" ");
        }
        return sb.toString();
    }


    public String printMatrix() throws StorageException, ManagementException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        List<BuildSpec> bs = getBuilds();
        List<TestSpec> ts = getTests();
        try {
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                int t1 = printMatrix(ps, bs, ts);
                System.out.println(t1 + "/" + (bs.size() * ts.size()));
                int t2 = printMatrix(ps, ts, bs);
                System.out.println(t2 + "/" + (bs.size() * ts.size()));
            }
            return baos.toString(utf8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    int printMatrix(PrintStream p, Iterable<? extends Spec> rows, Iterable<? extends Spec> columns) throws ManagementException, StorageException {
        int lrow = getLongest(rows) + 1;
        int lcol = getLongest(columns) + 1;
        int total = 0;

        System.out.print(fill("", lrow));
        for (Spec t : columns) {
            p.print(fill(t.toString(), lcol));
        }
        p.println();
        for (Spec b : rows) {
            p.print(fill(b.toString(), lrow));
            for (Spec t : columns) {
                List<Leaf> matched = countMatchingProjects(b, t);
                p.print(fill("" + matched.size(), lcol));
                total += matched.size();
            }
            p.println();

        }
        p.print(fill("", lrow));
        for (Spec t : columns) {
            p.print(fill(t.toString(), lcol));
        }
        p.println();
        return total;
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
                Set<Map.Entry<String, BuildPlatformConfig>> buildPlatformConfig = jc.getPlatforms().entrySet();
                for (Map.Entry<String, BuildPlatformConfig> bpce : buildPlatformConfig) {
                    TaskConfig tc = new TaskConfig(bpce.getValue().getVariants());
                    Map<String, TaskConfig> taskConfigs = new HashMap<>();
                    taskConfigs.put(null, tc);
                    for (Map.Entry<String, TaskConfig> btce : taskConfigs.entrySet()) {
                        for (VariantsConfig bvc : btce.getValue().getVariants()) {
                            iterateBuildVariantsConfig(bs, ts, counter, project, bpce.getKey(), null, btce, bvc);
                        }
                    }
                }
            } else if (project instanceof JDKProject) {
                JobConfiguration jc = ((JDKProject) project).getJobConfiguration();
                for (Map.Entry<String, PlatformConfig> bpce : jc.getPlatforms().entrySet()) {
                    for (Map.Entry<String, TaskConfig> btce : bpce.getValue().getTasks().entrySet()) {
                        for (VariantsConfig bvc : btce.getValue().getVariants()) {
                            iterateBuildVariantsConfig(bs, ts, counter, project, bpce.getKey(), bpce.getValue().getProvider(), btce, bvc);
                        }
                    }
                }
            } else {
                throw new ManagementException("Unknow project type " + project.getClass());
            }
        }
        return counter;
    }

    private void iterateBuildVariantsConfig(BuildSpec bs, TestSpec ts, List<Leaf> counter, Project project, String buildArchOs, String buildProvider, Map.Entry<String, TaskConfig> btce, VariantsConfig bvc) {
        for (Map.Entry<String, PlatformConfig> tpce : bvc.getPlatforms().entrySet()) {
            for (Map.Entry<String, TaskConfig> ttce : tpce.getValue().getTasks().entrySet()) {
                for (VariantsConfig tvc : ttce.getValue().getVariants()) {
                    String[] buildOsAarch = buildArchOs.split("\\.");
                    String btask = btce.getKey(); //always build
                    Collection<String> buildVars = bvc.getMap().values();
                    String[] testOsAarch = tpce.getKey().split("\\.");
                    String testProvider = tpce.getValue().getProvider();
                    String ttask = ttce.getKey();
                    Collection<String> testVars = tvc.getMap().values();
                    String full = buildArchOs + "." + buildProvider + "-" +
                            btask + "-" +
                            project.getProduct().getJdk() + "-" +
                            project.getId() + "-" +
                            String.join(".", buildVars) + "-" +
                            tpce.getKey() + "." + testProvider + "-" +
                            ttask + "-" +
                            String.join(".", testVars);
                    //System.out.println(full);
                    if (ts.getTask().getId().equals("build")) { //where it get from?
                        if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)) {
                            counter.add(new Leaf(full));
                        }
                    } else {
                        if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, project.getProduct().getJdk(), buildVars)
                                && taskMatcher(ts, ttask, testOsAarch[0], testOsAarch[1], testProvider, testVars)) {
                            counter.add(new Leaf(full));
                        }

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
    }
}
