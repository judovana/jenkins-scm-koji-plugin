package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Parser;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.BuildPlatformConfig;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.storage.StorageException;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JDKProjectParser implements Parser<Project, Set<Job>> {

    private JobBuilder jobBuilder;
    private final ConfigManager configManager;
    private final File repositoriesRoot;
    private final File scriptsRoot;

    public JDKProjectParser(
            final ConfigManager configManager,
            File repositoriesRoot,
            File scriptsRoot
    ) {
        this.configManager = configManager;
        this.repositoriesRoot = repositoriesRoot;
        this.scriptsRoot = scriptsRoot;
    }

    @Override
    public Set<Job> parse(Project project) throws ManagementException, StorageException {
        jobBuilder = new JobBuilder(configManager, project.getType());
        if (!configManager.getJdkVersionStorage().contains(project.getProduct().getJdk())) {
            throw new ManagementException("Unknown product: " + project.getProduct());
        }
        final JDKVersion jdkVersion = configManager.getJdkVersionStorage().load(project.getProduct().getJdk(), JDKVersion.class);

        final Set<BuildProvider> buildProviders = new HashSet<>();
        for (final String buildProvider : project.getBuildProviders()) {
            if (!configManager.getBuildProviderStorage().contains(buildProvider)) {
                throw new ManagementException("Unknown build provider: " + buildProvider);
            }
            buildProviders.add(configManager.getBuildProviderStorage().load(buildProvider, BuildProvider.class));
        }

        jobBuilder.setProjectName(project.getId());
        jobBuilder.setProduct(project.getProduct());
        jobBuilder.setJDKVersion(jdkVersion);
        jobBuilder.setBuildProviders(buildProviders);
        jobBuilder.setProjectVariables(project.getVariables());

        switch (project.getType()) {
            case JDK_PROJECT:
                return parse((JDKProject) project);
            case JDK_TEST_PROJECT:
                return parse((JDKTestProject) project);
        }
        return Collections.emptySet();
    }

    private Set<Job> parse(JDKProject project) {
        project.getJobConfiguration().getPlatforms().forEach(getPlatformsConsumer());

        jobBuilder.buildPullJob();
        return jobBuilder.getJobs();
    }

    private Set<Job> parse(JDKTestProject project) throws ManagementException {
        jobBuilder.subpackageBlacklist = project.getSubpackageBlacklist();
        jobBuilder.subpackageWhitelist = project.getSubpackageWhitelist();
        jobBuilder.buildTask = new Task();
        project.getJobConfiguration().getPlatforms().forEach(getBuildPlatformConsumer());
        return jobBuilder.getJobs();
    }

    private Consumer<BuildPlatformConfig> getBuildPlatformConsumer() {
        return ManagementUtils.managementConsumerWrapper(
                (BuildPlatformConfig config) -> {
                    jobBuilder.setPlatform(config);
                    config.getVariants().forEach(getVariantsConsumer());
                    jobBuilder.resetPlatform();
                }
        );
    }

    private Consumer<PlatformConfig> getPlatformsConsumer() {
        return ManagementUtils.managementConsumerWrapper(
                (PlatformConfig platformConfig) -> {
                    jobBuilder.setPlatform(platformConfig);
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

        private final Project.ProjectType projectType;
        private final Map<String, Platform> platformsMap;
        private final Map<String, Task> tasksMap;
        private final Map<String, TaskVariant> testVariantsMap;
        private final Map<String, TaskVariant> buildVariantsMap;

        private List<String> subpackageBlacklist = Collections.emptyList();
        private List<String> subpackageWhitelist = Collections.emptyList();

        private final Set<Job> jobs;

        private String projectName;
        private JDKVersion jdkVersion;
        private Product product;
        private Set<BuildProvider> buildProviders;
        private String buildPlatformProvider;
        private String testPlatformProvider;
        private Platform buildPlatform;
        private Task buildTask;
        private Map<TaskVariant, TaskVariantValue> buildVariants;
        private Platform testPlatform;
        private Task testTask;
        private BuildJob buildJob;
        private Map<TaskVariant, TaskVariantValue> testVariants;
        private List<OToolVariable> projectVariables;

        JobBuilder(final ConfigManager configManager, Project.ProjectType projectType) throws StorageException {
            this.projectType = projectType;
            jobs = new HashSet<>();
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
            testVariantsMap = configManager.getTaskVariantStorage()
                    .loadAll(TaskVariant.class)
                    .stream()
                    .filter(variant -> variant.getType().equals(Task.Type.TEST))
                    .collect(Collectors.toMap(TaskVariant::getId, taskVariant -> taskVariant));
            buildVariantsMap = configManager.getTaskVariantStorage()
                    .loadAll(TaskVariant.class)
                    .stream()
                    .filter(variant -> variant.getType().equals(Task.Type.BUILD))
                    .collect(Collectors.toMap(TaskVariant::getId, taskVariant -> taskVariant));
        }

        Set<Job> getJobs() {
            return jobs;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public void setJDKVersion(JDKVersion jdkVersion) {
            this.jdkVersion = jdkVersion;
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
                    jdkVersion,
                    repositoriesRoot,
                    scriptsRoot,
                    projectVariables
            ));
        }

        private void buildBuildJob() {
            buildJob = new BuildJob(
                    buildPlatformProvider,
                    projectName,
                    product,
                    jdkVersion,
                    buildProviders,
                    buildTask,
                    buildPlatform,
                    buildVariants,
                    scriptsRoot,
                    projectVariables
            );
            jobs.add(buildJob);
        }

        private void buildTestJob() {
            final TestJob tj = (buildJob == null) ?
                    new TestJob(
                            testPlatformProvider,
                            projectName,
                            Project.ProjectType.JDK_TEST_PROJECT,
                            product,
                            jdkVersion,
                            buildProviders,
                            testTask,
                            testPlatform,
                            testVariants,
                            buildPlatform,
                            buildVariants,
                            subpackageBlacklist,
                            subpackageWhitelist,
                            scriptsRoot,
                            projectVariables
                    ) :
                    new TestJob(
                            testPlatformProvider,
                            buildJob,
                            testTask,
                            testPlatform,
                            testVariants,
                            projectVariables
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

        void setPlatform(final BuildPlatformConfig buildPlatformConfig) {
            buildPlatform = platformsMap.get(buildPlatformConfig.getId());
        }

        void setPlatform(final PlatformConfig platformConfig) {
            if (buildPlatform == null) {
                buildPlatform = platformsMap.get(platformConfig.getId());
                buildPlatformProvider = platformConfig.getProvider();
                return;
            }
            if (testPlatform == null) {
                testPlatform = platformsMap.get(platformConfig.getId());
                testPlatformProvider = platformConfig.getProvider();
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
                    final TaskVariant variant = buildVariantsMap.get(key);
                    if (variant == null) {
                        throw new RuntimeException("Unknown variant type " + key + " from " + String.join(";", buildVariantsMap.keySet().toArray(new String[0])));
                    }
                    final TaskVariantValue variantValue = variant.getVariants().get(value);
                    if (variantValue == null) {
                        throw new RuntimeException("Unknown variant value " + value + " from " + String.join(";", variant.getVariants().keySet().toArray(new String[0])));
                    }
                    buildVariants.put(variant, variantValue);
                });
                if (projectType == Project.ProjectType.JDK_PROJECT) {
                    buildBuildJob();
                }
                return;
            }
            if (testVariants == null) {
                testVariants = new HashMap<>(variants.size());
                variants.forEach((String key, String value) -> {
                    final TaskVariant variant = testVariantsMap.get(key);
                    if (variant == null) {
                        throw new RuntimeException("Unknown variant type " + key + " from " + String.join(";", testVariantsMap.keySet().toArray(new String[0])));
                    }
                    final TaskVariantValue variantValue = variant.getVariants().get(value);
                    if (variantValue == null) {
                        throw new RuntimeException("Unknown variant value " + value + " from " + String.join(";", variant.getVariants().keySet().toArray(new String[0])));
                    }
                    testVariants.put(variant, variantValue);
                });
                buildTestJob();
                return;
            }
            throw new RuntimeException("Setting platform error");
        }

        void setProjectVariables(List<OToolVariable> variables) {
            this.projectVariables = variables;
        }
    }

}
