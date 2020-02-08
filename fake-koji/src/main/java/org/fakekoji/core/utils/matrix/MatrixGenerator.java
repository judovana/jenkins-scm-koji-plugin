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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    void printMatrix(PrintStream p, Iterable<? extends Spec> rows, Iterable<? extends Spec> columns) throws ManagementException, StorageException {
        int lrow = getLongest(rows) + 1;
        int lcol = getLongest(columns) + 1;

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
            }
            p.println();

        }
        p.print(fill("", lrow));
        for (Spec t : columns) {
            p.print(fill(t.toString(), lcol));
        }
        p.println();
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
        int counter = 0;
        //do not optimise, will break the compression of attributes
        for (JDKProject project : jdkProjectManager.readAll()) {
            for (Map.Entry<String, PlatformConfig> bpce : project.getJobConfiguration().getPlatforms().entrySet()) {
                for (Map.Entry<String, TaskConfig> btce : bpce.getValue().getTasks().entrySet()) {
                    for (VariantsConfig bvc : btce.getValue().getVariants()) {
                        for (Map.Entry<String, PlatformConfig> tpce : bvc.getPlatforms().entrySet()) {
                            for (Map.Entry<String, TaskConfig> ttce : tpce.getValue().getTasks().entrySet()) {
                                for (VariantsConfig tvc : ttce.getValue().getVariants()) {
                                    String full = bpce.getKey() + "-" +
                                            btce.getKey() + "-" +
                                            project.getId() + "-" +
                                            String.join(".", bvc.getMap().values()) + "-" +
                                            tpce.getKey() + "-" +
                                            ttce.getKey() + "-" +
                                            String.join(".", tvc.getMap().values());
                                    String[] buildOsAarch = bpce.getKey().split("\\.");
                                    String buildProvider = bpce.getValue().getProvider();
                                    String btask = btce.getKey();
                                    Collection<String> buildVars = bvc.getMap().values();
                                    String[] testOsAarch = tpce.getKey().split("\\.");
                                    String testProvider = tpce.getValue().getProvider();
                                    String ttask = ttce.getKey();
                                    Collection<String> testVars = tvc.getMap().values();
                                    //System.out.println(full);
                                    if (ts.getTask().getId().equals("build")) { //where it get from?
                                        if (bs.getProject().getId().equals(project.getId())) {
                                            if (bs.matchOs(buildOsAarch[0])) {
                                                if (bs.matchArch(buildOsAarch[1])) {
                                                    if (bs.matchProvider(buildProvider)) {
                                                        if (bs.matchVars(buildVars)) {
                                                            counter++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        if (bs.getProject().getId().equals(project.getId())) {
                                            if (bs.matchOs(buildOsAarch[0])) {
                                                if (bs.matchArch(buildOsAarch[1])) {
                                                    if (bs.matchProvider(buildProvider)) {
                                                        if (bs.matchVars(buildVars)) {
                                                            if (ts.matchOs(testOsAarch[0])) {
                                                                if (ts.matchArch(testOsAarch[1])) {
                                                                    if (ts.matchProvider(testProvider)) {
                                                                        if (ts.matchVars(testVars)) {
                                                                            counter++;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (JDKTestProject p : jdkTestProjectManager.readAll()) {
            //afaik identical to jdkProject, only build platfrom do not contain provider
            //so no separate loop folk
        }
        return counter;
    }

}
