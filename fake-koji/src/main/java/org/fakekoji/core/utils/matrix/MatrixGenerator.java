package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.Project;
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
import java.util.logging.Level;
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


    public String emptyPrint() throws StorageException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        List<BuildSpec> bs = getBuilds();
        List<TestSpec> ts = getTests();
        try {
            try (PrintStream ps = new PrintStream(baos, true, utf8)) {
                emptyPrint(ps, bs, ts);
                emptyPrint(ps, ts, bs);
            }
            return baos.toString(utf8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void emptyPrint(PrintStream p, Iterable<? extends Spec> rows, Iterable<? extends Spec> columns) {
        int lrow = getLongest(rows) + 1;
        int lcol = getLongest(columns) + 1;

        System.out.print(fill("", lrow));
        for (Spec t : columns) {
            p.print(fill(t.toString(), lcol));
        }
        p.println();
        for (Spec b : rows) {
            p.println(fill(b.toString(), lrow));
        }
        p.print(fill("", lrow));
        for (Spec t : columns) {
            p.print(fill(t.toString(), lcol));
        }
        p.println();
    }

}
