package org.fakekoji.jobmanager;

import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.DirectoryJsonStorage;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    public final static String BUILD_PROVIDERS = "buildProviders";
    public final static String JDK_VERSIONS = "jdkVersions";
    public final static String TASK_VARIANTS = "taskVariants";
    public final static String PLATFORMS = "platforms";
    public final static String JDK_PROJECTS = "jdkProjects";
    public final static String JDK_TEST_PROJECTS = "jdkTestProjects";
    public final static String TASKS = "tasks";

    public final BuildProviderManager buildProviderManager;
    public final JDKVersionManager jdkVersionManager;
    public final TaskVariantManager taskVariantManager;
    public final PlatformManager platformManager;
    public final JDKProjectManager jdkProjectManager;
    public final JDKTestProjectManager jdkTestProjectManager;
    public final TaskManager taskManager;

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
    
    public Result<List<Project>, OToolError> getProjects(final List<String> projectIds) {
        final List<Project> projects = new ArrayList<>();
        try {
            for (final String projectId : projectIds) {
                if (jdkProjectManager.contains(projectId)) {
                    projects.add(jdkProjectManager.read(projectId));
                    continue;
                }
                if (jdkTestProjectManager.contains(projectId)) {
                    projects.add(jdkTestProjectManager.read(projectId));
                    continue;
                }
                return Result.err(new OToolError("Unknown project: " + projectId, 400));
            }
        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }
        return Result.ok(projects);
    }

}
