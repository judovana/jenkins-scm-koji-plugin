package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.DirectoryJsonStorage;
import org.fakekoji.storage.Storage;

import java.nio.file.Paths;

public class ConfigManager {

    private final static String BUILD_PROVIDERS = "buildProviders";
    private final static String PRODUCTS = "products";
    private final static String TASK_VARIANTS = "taskVariants";
    private final static String PLATFORMS = "platforms";
    private final static String PROJECTS = "jdkProjects";
    private final static String JDK_TEST_PROJECTS = "jdkTestProjects";
    private final static String TASKS = "tasks";

    private final Storage<BuildProvider> buildProviderStorage;
    private final Storage<Product> productStorage;
    private final Storage<TaskVariant> taskVariantStorage;
    private final Storage<Platform> platformStorage;
    private final Storage<JDKProject> jdkProjectStorage;
    private final Storage<JDKTestProject> jdkTestProjectStorage;
    private final Storage<Task> taskStorage;

    private ConfigManager(
            Storage<BuildProvider> buildProviderStorage,
            Storage<Product> productStorage,
            Storage<TaskVariant> taskVariantStorage,
            Storage<Platform> platformStorage,
            Storage<JDKProject> jdkProjectStorage,
            Storage<JDKTestProject> jdkTestProjectStorage,
            Storage<Task> taskStorage
    ) {
        this.buildProviderStorage = buildProviderStorage;
        this.productStorage = productStorage;
        this.taskVariantStorage = taskVariantStorage;
        this.platformStorage = platformStorage;
        this.jdkProjectStorage = jdkProjectStorage;
        this.jdkTestProjectStorage = jdkTestProjectStorage;
        this.taskStorage = taskStorage;
    }

    public static ConfigManager create(String storagePath) {
        return new ConfigManager(
                new DirectoryJsonStorage<>(Paths.get(storagePath, BUILD_PROVIDERS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, PRODUCTS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, TASK_VARIANTS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, PLATFORMS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, PROJECTS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, JDK_TEST_PROJECTS).toFile()),
                new DirectoryJsonStorage<>(Paths.get(storagePath, TASKS).toFile())
        );
    }

    public Storage<BuildProvider> getBuildProviderStorage() {
        return buildProviderStorage;
    }

    public Storage<Product> getProductStorage() {
        return productStorage;
    }

    public Storage<TaskVariant> getTaskVariantStorage() {
        return taskVariantStorage;
    }

    public Storage<Platform> getPlatformStorage() {
        return platformStorage;
    }

    public Storage<JDKProject> getJdkProjectStorage() {
        return jdkProjectStorage;
    }

    public Storage<JDKTestProject> getJdkTestProjectStorage() {
        return jdkTestProjectStorage;
    }

    public Storage<Task> getTaskStorage() {
        return taskStorage;
    }
}
