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

public class MatrixGenerator {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    final BuildProviderManager buildProviderManager;
    final PlatformManager platformManager;
    final TaskVariantManager taskVariantManager;
    final JDKVersionManager jDKVersionManager;
    final TaskManager taskManager;
    final JDKTestProjectManager jdkTestProjectManager;
    final JDKProjectManager jdkProjectManager;

    public MatrixGenerator(AccessibleSettings settings, ConfigManager configManager) {
        final JenkinsJobUpdater jenkinsJobUpdater = new JenkinsJobUpdater(settings);
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
        ArrayList<TestSpec> r = new ArrayList<>();
        for (Platform platform : platformManager.readAll()) {
            for (Platform.Provider provider : platform.getProviders()) {
                for (Task task : taskManager.readAll()) {
                    if (!task.getType().equals(Task.Type.TEST)) {
                        TestSpec t = new TestSpec(platform, provider, task);
                        r.add(t);
                    } else {
                        Collection<Collection<TaskVariantValue>> variants = cartesianProduct(getTasksSets(taskVariantManager.readAll(), task.getType()));
                        for (Collection<TaskVariantValue> tvvs : variants) {
                            TestSpec t = new TestSpec(platform, provider, task);
                            for (TaskVariantValue tv : tvvs) {
                                t.addVariant(tv);
                            }
                            r.add(t);
                        }
                    }
                }
            }
        }
        return r;
    }


    public List<BuildSpec> getBuilds() throws StorageException {
        ArrayList<BuildSpec> r = new ArrayList<>();
        for (Platform platform : platformManager.readAll()) {
            for (Platform.Provider provider : platform.getProviders()) {
                for (Project project : concateProjects(jdkProjectManager.readAll(), jdkTestProjectManager.readAll())) {
                    Collection<Collection<TaskVariantValue>> variants = cartesianProduct(getTasksSets(taskVariantManager.readAll(), Task.Type.BUILD));
                    for (Collection<TaskVariantValue> tvvs : variants) {
                        BuildSpec b = new BuildSpec(platform, provider, project);
                        for (TaskVariantValue tv : tvvs) {
                            b.addVariant(tv);
                        }
                        r.add(b);
                    }
                }
            }
        }
        return r;
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
                printMatrix(ps, bs, ts);
                printMatrix(ps, ts, bs);
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
                int matches = countMatchingProjects(b, t);
                p.print(fill("" + matches, lcol));
                total += matches;
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

    private int countMatchingProjects(Spec b, Spec t) throws StorageException, ManagementException {
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
        int[] counter = {0};
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
        return counter[0];
    }

    private void iterateBuildVariantsConfig(BuildSpec bs, TestSpec ts, int[] counter, Project project, String buildArchOs, String buildProvider, Map.Entry<String, TaskConfig> btce, VariantsConfig bvc) {
        for (Map.Entry<String, PlatformConfig> tpce : bvc.getPlatforms().entrySet()) {
            for (Map.Entry<String, TaskConfig> ttce : tpce.getValue().getTasks().entrySet()) {
                for (VariantsConfig tvc : ttce.getValue().getVariants()) {
                    String full = buildArchOs + "-" +
                            btce.getKey() + "-" +
                            project.getId() + "-" +
                            String.join(".", bvc.getMap().values()) + "-" +
                            tpce.getKey() + "-" +
                            ttce.getKey() + "-" +
                            String.join(".", tvc.getMap().values());
                    String[] buildOsAarch = buildArchOs.split("\\.");
                    String btask = btce.getKey(); //always build
                    Collection<String> buildVars = bvc.getMap().values();
                    String[] testOsAarch = tpce.getKey().split("\\.");
                    String testProvider = tpce.getValue().getProvider();
                    String ttask = ttce.getKey();
                    Collection<String> testVars = tvc.getMap().values();
                    //System.out.println(full);
                    if (ts.getTask().getId().equals("build")) { //where it get from?
                        if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, buildVars)) {
                            counter[0]++;
                        }
                    } else {
                        if (buildMatcher(bs, project.getId(), buildOsAarch[0], buildOsAarch[1], buildProvider, buildVars)
                                && taskMatcher(ts, ttask, testOsAarch[0], testOsAarch[1], testProvider, testVars)) {
                            counter[0]++;
                        }

                    }
                }
            }
        }
    }


    private boolean genericMatcher(Spec s, String os, String arch, String provider, Collection<String> variants) {
        return s.matchOs(os) &&
                s.matchArch(arch) &&
                s.matchProvider(provider) &&
                s.matchVars(variants);
    }

    private boolean buildMatcher(BuildSpec bs, String projectId, String os, String arch, String provider, Collection<String> variants) {
        return bs.getProject().getId().equals(projectId) &&
                genericMatcher(bs, os, arch, provider, variants);
    }

    private boolean taskMatcher(TestSpec ts, String taskId, String os, String arch, String provider, Collection<String> variants) {
        return ts.getTask().getId().equals(taskId) &&
                genericMatcher(ts, os, arch, provider, variants);
    }

}
