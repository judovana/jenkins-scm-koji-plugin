package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Parser;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantCategory;
import org.fakekoji.storage.StorageException;
import org.fakekoji.jobmanager.ConfigManager;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JDKProjectParser implements Parser<JDKProject, Set<Job>> {

    private final JobBuilder jobBuilder;
    private final ConfigManager configManager;
    private final File repositoriesRoot;

    JDKProjectParser(final ConfigManager configManager, File repositoriesRoot) throws StorageException {
        this.configManager = configManager;
        this.repositoriesRoot = repositoriesRoot;
        jobBuilder = new JobBuilder(configManager);
    }

    @Override
    public Set<Job> parse(JDKProject project) throws ManagementException, StorageException {
        if (!configManager.getProductStorage().contains(project.getProduct())) {
            throw new ManagementException("Unknown product: " + project.getProduct());
        }
        final Product product = configManager.getProductStorage().load(project.getProduct(), Product.class);

        final Set<BuildProvider> buildProviders = new HashSet<>();
        for (final String buildProvider : project.getBuildProviders()) {
            if (!configManager.getBuildProviderStorage().contains(buildProvider)) {
                throw new ManagementException("Unknown build provider: " + buildProvider);
            }
            buildProviders.add(configManager.getBuildProviderStorage().load(buildProvider, BuildProvider.class));
        }

        jobBuilder.setProjectName(project.getId());
        jobBuilder.setProduct(product);
        jobBuilder.setBuildProviders(buildProviders);

        project.getJobConfiguration().getPlatforms().forEach(getPlatformsConsumer());

        jobBuilder.buildPullJob();
        return jobBuilder.getJobs();
    }

    private BiConsumer<String, PlatformConfig> getPlatformsConsumer() {
        return ManagementUtils.managementBiConsumerWrapper(
                (String platformId, PlatformConfig platformConfig) -> {
                    jobBuilder.setPlatform(platformId);
                    platformConfig.getTasks().forEach(getTasksConsumer());
                    jobBuilder.resetPlatform();
                }
        );
    }

    private BiConsumer<String, TaskConfig> getTasksConsumer() {
        return ManagementUtils.managementBiConsumerWrapper(
                (String taskId, TaskConfig taskConfig) -> {
                    jobBuilder.setTask(taskId);
                    taskConfig.getVariants().forEach(getVariantsConsumer());
                    jobBuilder.resetTask();
                }
        );
    }

    private Consumer<VariantsConfig> getVariantsConsumer() {
        return ManagementUtils.managementConsumerWrapper(
                (VariantsConfig variantsConfig) -> {
                    jobBuilder.setVariants(variantsConfig.getMap());
                    variantsConfig.getPlatforms().forEach(getPlatformsConsumer());
                    jobBuilder.resetVariants();
                }
        );
    }

    private class JobBuilder {

        private final Map<String, Platform> platformsMap;
        private final Map<String, Task> tasksMap;
        private final Map<String, TaskVariantCategory> testVariantCategoriesMap;
        private final Map<String, TaskVariantCategory> buildVariantCategoriesMap;

        private final Set<Job> jobs;
        private final Set<String> buildVariantsStrings;

        private String projectName;
        private Product product;
        private Set<BuildProvider> buildProviders;
        private Platform buildPlatform;
        private Task buildTask;
        private Map<TaskVariantCategory, TaskVariant> buildVariants;
        private Platform testPlatform;
        private Task testTask;
        private BuildJob buildJob;
        private Map<TaskVariantCategory, TaskVariant> testVariants;

        JobBuilder(final ConfigManager configManager) throws StorageException {
            jobs = new HashSet<>();
            buildVariantsStrings = new HashSet<>();
            buildPlatform = null;
            buildTask = null;
            buildVariants = null;
            testPlatform = null;
            testTask = null;
            testVariants = null;
            tasksMap = configManager.getTaskStorage()
                    .loadAll(Task.class)
                    .stream()
                    .collect(Collectors.toMap(Task::getId, task -> task));
            platformsMap = configManager.getPlatformStorage()
                    .loadAll(Platform.class)
                    .stream()
                    .collect(Collectors.toMap(Platform::getId, platform -> platform));
            testVariantCategoriesMap = configManager.getTaskVariantCategoryStorage()
                    .loadAll(TaskVariantCategory.class)
                    .stream()
                    .filter(category -> category.getType().equals(TaskVariantCategory.Usage.TEST))
                    .collect(Collectors.toMap(TaskVariantCategory::getId, taskVariantCategory -> taskVariantCategory));
            buildVariantCategoriesMap = configManager.getTaskVariantCategoryStorage()
                    .loadAll(TaskVariantCategory.class)
                    .stream()
                    .filter(category -> category.getType().equals(TaskVariantCategory.Usage.BUILD))
                    .collect(Collectors.toMap(TaskVariantCategory::getId, taskVariantCategory -> taskVariantCategory));
        }

        Set<Job> getJobs() {
            return jobs;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public void setBuildProviders(Set<BuildProvider> buildProviders) {
            this.buildProviders = buildProviders;
        }

        private void buildPullJob() {
            jobs.add(new PullJob(
                    projectName,
                    product,
                    buildVariantsStrings,
                    repositoriesRoot
            ));
        }

        private void buildBuildJob() {
            buildJob = new BuildJob(
                    projectName,
                    product,
                    buildProviders,
                    buildTask,
                    buildPlatform,
                    buildVariants
            );
            buildVariantsStrings.add(buildJob.getVariants().entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> entry.getValue().getId())
                    .collect(Collectors.joining(Job.DELIMITER)));
            jobs.add(buildJob);
        }

        private void buildTestJob() {
            final TestJob tj = new TestJob(
                    buildJob,
                    testTask,
                    testPlatform,
                    testVariants
            );
            jobs.add(tj);
        }

        void resetPlatform() {
            if (testPlatform != null) {
                testPlatform = null;
                return;
            }
            if (buildPlatform != null) {
                buildPlatform = null;
                return;
            }
            throw new RuntimeException("Resetting platform error");
        }

        public void setPlatform(String id) {
            if (buildPlatform == null) {
                buildPlatform = platformsMap.get(id);
                return;
            }
            if (testPlatform == null) {
                testPlatform = platformsMap.get(id);
                return;
            }
            throw new RuntimeException("Setting platform error");
        }

        void resetTask() {
            if (testTask != null) {
                testTask = null;
                return;
            }
            if (buildTask != null) {
                buildTask = null;
                return;
            }
            throw new RuntimeException("Resetting task error");
        }

        void setTask(String id) {
            if (buildTask == null) {
                buildTask = tasksMap.get(id);
                return;
            }
            if (testTask == null) {
                testTask = tasksMap.get(id);
                return;
            }
            throw new RuntimeException("Setting task error");
        }

        void resetVariants() {
            if (testVariants != null) {
                testVariants = null;
                return;
            }
            if (buildVariants != null) {
                buildVariants = null;
                return;
            }
            throw new RuntimeException("Resetting platform error");
        }

        void setVariants(Map<String, String> variants) {
            if (buildVariants == null) {
                buildVariants = new HashMap<>(variants.size());
                variants.forEach((String key, String value) -> {
                    final TaskVariantCategory category = buildVariantCategoriesMap.get(key);
                    if (category == null) {
                        throw new RuntimeException("Unknown variant category");
                    }
                    final TaskVariant variant = category.getVariants().get(value);
                    if (variant == null) {
                        throw new RuntimeException("Unknown variant");
                    }
                    buildVariants.put(category, variant);
                });
                buildBuildJob();
                return;
            }
            if (testVariants == null) {
                testVariants = new HashMap<>(variants.size());
                variants.forEach((String key, String value) -> {
                    final TaskVariantCategory category = testVariantCategoriesMap.get(key);
                    if (category == null) {
                        throw new RuntimeException("Unknown variant category");
                    }
                    final TaskVariant variant = category.getVariants().get(value);
                    if (variant == null) {
                        throw new RuntimeException("Unknown variant");
                    }
                    testVariants.put(category, variant);
                });
                buildTestJob();
                return;
            }
            throw new RuntimeException("Setting platform error");
        }
    }

}
