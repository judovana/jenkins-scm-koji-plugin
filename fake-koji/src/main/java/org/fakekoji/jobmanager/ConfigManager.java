package org.fakekoji.jobmanager;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.DirectoryJsonStorage;
import org.fakekoji.storage.Storage;

import java.nio.file.Paths;

public class ConfigManager {
    public final static String BUILD_PROVIDERS = "buildProviders";
    public final static String JDK_VERSIONS = "jdkVersions";
    public final static String TASK_VARIANTS = "taskVariants";
    public final static String PLATFORMS = "platforms";
    public final static String JDK_PROJECTS = "jdkProjects";
    public final static String JDK_TEST_PROJECTS = "jdkTestProjects";
    public final static String TASKS = "tasks";

    private final BuildProviderManager buildProviderManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;
    private final PlatformManager platformManager;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final TaskManager taskManager;

    public ConfigManager(final AccessibleSettings settings) {
        final String storagePath = settings.getConfigRoot().getAbsolutePath();
        Storage<BuildProvider> buildProviderStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, BUILD_PROVIDERS).toFile());
        Storage<JDKVersion> jdkVersionStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, JDK_VERSIONS).toFile());
        Storage<TaskVariant> taskVariantStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, TASK_VARIANTS).toFile());
        Storage<Platform> platformStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, PLATFORMS).toFile());
        Storage<JDKProject> jdkProjectStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, JDK_PROJECTS).toFile());
        Storage<JDKTestProject> jdkTestProjectStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, JDK_TEST_PROJECTS).toFile());
        Storage<Task> taskStorage =
                new DirectoryJsonStorage<>(Paths.get(storagePath, TASKS).toFile());

        buildProviderManager = new BuildProviderManager(buildProviderStorage);
        jdkVersionManager = new JDKVersionManager(jdkVersionStorage);
        taskVariantManager = new TaskVariantManager(taskVariantStorage);
        platformManager = new PlatformManager(platformStorage);
        jdkProjectManager = new JDKProjectManager(
                jdkProjectStorage,
                jdkVersionStorage,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        jdkTestProjectManager = new JDKTestProjectManager(jdkTestProjectStorage);
        taskManager = new TaskManager(taskStorage);
    }

    public BuildProviderManager getBuildProviderManager() {
        return buildProviderManager;
    }

    public JDKVersionManager getJdkVersionManager() {
        return jdkVersionManager;
    }

    public TaskVariantManager getTaskVariantManager() {
        return taskVariantManager;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }

    public JDKProjectManager getJdkProjectManager() {
        return jdkProjectManager;
    }

    public JDKTestProjectManager getJdkTestProjectManager() {
        return jdkTestProjectManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }
}
