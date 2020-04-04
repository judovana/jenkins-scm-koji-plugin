package org.fakekoji.jobmanager.project;

import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Parser;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.BuildPlatformConfig;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobConfiguration;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.model.TestJobConfiguration;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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

        jobBuilder.buildPullJob(project.getUrl());
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

    private Consumer<TaskConfig> getTasksConsumer() {
        return ManagementUtils.managementConsumerWrapper(
                (TaskConfig taskConfig) -> {
                    jobBuilder.setTask(taskConfig.getId());
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

        private void buildPullJob(final String repoUrl) {
            jobs.add(new PullJob(
                    projectName,
                    repoUrl,
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
                            buildPlatformProvider,
                            buildTask,
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

    private static <T> Optional<T> findConfig(Set<T> configs, Predicate<T> predicate) {
        return configs.stream()
                .filter(predicate)
                .findFirst();
    }

    private static Optional<PlatformConfig> findPlatform(Set<PlatformConfig> configs, String id, String provider) {
        return findConfig(configs, config -> config.getId().equals(id) && config.getProvider().equals(provider));
    }

    private static PlatformConfig findOrCreatePlatform(Set<PlatformConfig> configs, String id, String provider) {
        final Optional<PlatformConfig> configOptional = findPlatform(configs, id, provider);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            final PlatformConfig config = new PlatformConfig(id, new HashSet<>(), provider);
            configs.add(config);
            return config;
        }
    }

    private static Optional<BuildPlatformConfig> findPlatform(Set<BuildPlatformConfig> configs, String id) {
        return findConfig(configs, config -> config.getId().equals(id));
    }

    private static BuildPlatformConfig findOrCreatePlatform(Set<BuildPlatformConfig> configs, String id) {
        final Optional<BuildPlatformConfig> configOptional = findPlatform(configs, id);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            final BuildPlatformConfig config = new BuildPlatformConfig(id, new HashSet<>());
            configs.add(config);
            return config;
        }
    }

    private static Optional<TaskConfig> findTask(Set<TaskConfig> configs, String id) {
        return findConfig(configs, config -> config.getId().equals(id));
    }

    private static TaskConfig findOrCreateTask(Set<TaskConfig> configs, String id) {
        final Optional<TaskConfig> configOptional = findTask(configs, id);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            final TaskConfig config = new TaskConfig(id, new HashSet<>());
            configs.add(config);
            return config;
        }
    }

    private static Optional<VariantsConfig> findVariants(Set<VariantsConfig> configs, Map<String, String> map) {
        return findConfig(configs, config -> config.getMap().equals(map));
    }

    private static VariantsConfig findOrCreateVariants(Set<VariantsConfig> configs, Map<String, String> map) {
        final Optional<VariantsConfig> configOptional = findVariants(configs, map);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            final VariantsConfig config = new VariantsConfig(map, new HashSet<>());
            configs.add(config);
            return config;
        }
    }

    private static Map<String, String> variantsMapToStringMap(Map<TaskVariant, TaskVariantValue> variants) {
        return variants.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue().getId()));
    }

    private static String validateJobField(final String label, final Object o1, final Object o2) {
        return o1.equals(o2)
                ? ""
                : label + " " + o1.toString() + " doesn't match " + o2.toString() + '\n';
    }

    private static Function<TaskJob, Function<TaskJob, String>> getTaskJobValidator() {
        return randomJob -> job -> validateJobField("project", job.getProjectName(), randomJob.getProjectName()) +
                validateJobField("product", job.getProduct(), randomJob.getProduct()) +
                validateJobField("jdk version", job.getJdkVersion(), randomJob.getJdkVersion()) +
                validateJobField("build providers", job.getBuildProviders(), randomJob.getBuildProviders()) +
                validateJobField("variables", job.getProjectVariables(), randomJob.getProjectVariables()) +
                validateJobField("scripts root", job.getScriptsRoot(), randomJob.getScriptsRoot());
    }

    private static Function<TestJob, Function<TestJob, String>> getTestJobValidator() {
        return randomJob -> {
            final Function<TaskJob, String> taskJobValidator = getTaskJobValidator().apply(randomJob);
            return job -> taskJobValidator.apply(job) +
                    validateJobField(
                            "project subpackage blacklist",
                            job.getProjectSubpackageBlacklist(),
                            randomJob.getProjectSubpackageBlacklist()
                    ) +
                    validateJobField(
                            "project subpackage whitelist",
                            job.getProjectSubpackageWhitelist(),
                            randomJob.getProjectSubpackageWhitelist()
                    );
        };
    }

    private static <T extends Job> Result<T, String> getRandomJob(Set<T> jobs) {
        final Optional<T> randomJobOptional = jobs.stream().findFirst();
        return randomJobOptional
                .<Result<T, String>>map(Result::ok)
                .orElseGet(() -> Result.err("Set of jobs is empty"));
    }

    private static <T extends TaskJob> Result<T, String> validateJobs(Set<T> jobs, Function<T, Function<T, String>> validator) {
        return getRandomJob(jobs).flatMap(randomJob -> {
            final List<String> errors = jobs.stream()
                    .map(validator.apply(randomJob))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(Collectors.toList());
            if (errors.isEmpty()) {
                return Result.ok(randomJob);
            }
            return Result.err(
                    String.join("\n", errors)
            );
        });
    }

    public Result<JDKProject, String> parseJDKProjectJobs(Set<Job> jobs) {
        final List<PullJob> pullJobs = jobs
                .stream().filter(j -> j instanceof PullJob)
                .map(j -> (PullJob) j)
                .collect(Collectors.toList());
        final Set<TaskJob> taskJobs = jobs.stream()
                .filter(j -> j instanceof TaskJob)
                .map(j -> (TaskJob) j)
                .collect(Collectors.toSet());
        final Set<BuildJob> buildJobs = jobs.stream()
                .filter(j -> j instanceof BuildJob)
                .map(j -> (BuildJob) j)
                .collect(Collectors.toSet());
        final Set<TestJob> testJobs = jobs.stream()
                .filter(j -> j instanceof TestJob)
                .map(j -> (TestJob) j)
                .collect(Collectors.toSet());
        if (pullJobs.isEmpty()) {
            return Result.err("Error: pull job not found");
        }
        if (pullJobs.size() > 1) {
            return Result.err("Error: more than one pull job (" + pullJobs.size() + ")");
        }
        final PullJob pullJob = pullJobs.get(0);
        return validateJobs(taskJobs, getTaskJobValidator()).flatMap(projectRepresentingJob -> {
            final Set<PlatformConfig> buildPlatforms = new HashSet<>();
            for (final BuildJob buildJob : buildJobs) {
                final String platformProvider = buildJob.getPlatformProvider();
                final Platform buildPlatform = buildJob.getPlatform();
                final Map<String, String> buildVariants = variantsMapToStringMap(buildJob.getVariants());
                final Task buildTask = buildJob.getTask();

                final PlatformConfig buildPlatformConfig = findOrCreatePlatform(
                        buildPlatforms,
                        buildPlatform.getId(),
                        platformProvider
                );
                final TaskConfig buildTaskConfig = findOrCreateTask(buildPlatformConfig.getTasks(), buildTask.getId());

                final VariantsConfig buildVariantsConfig;
                final Optional<VariantsConfig> bvOptional = findVariants(buildTaskConfig.getVariants(), buildVariants);
                if (bvOptional.isPresent()) {
                    return Result.err("Duplicate build job: " + buildJob.getName());
                } else {
                    buildVariantsConfig = new VariantsConfig(buildVariants, new HashSet<>());
                    buildTaskConfig.getVariants().add(buildVariantsConfig);
                }
            }
            for (final TestJob testJob : testJobs) {
                final String platformProvider = testJob.getPlatformProvider();
                final String buildPlatformProvider = testJob.getBuildPlatformProvider();
                final Platform testPlatform = testJob.getPlatform();
                final Map<String, String> buildVariants = variantsMapToStringMap(testJob.getBuildVariants());
                final Task buildTask = testJob.getBuildTask();
                final Platform buildPlatform = testJob.getBuildPlatform();
                final Map<String, String> testVariants = variantsMapToStringMap(testJob.getVariants());
                final Task testTask = testJob.getTask();

                final PlatformConfig buildPlatformConfig;
                final TaskConfig buildTaskConfig;
                final VariantsConfig buildVariantsConfig;

                final Optional<PlatformConfig> buildPlatformConfigOptional = findPlatform(
                        buildPlatforms,
                        buildPlatform.getId(),
                        buildPlatformProvider
                );

                if (buildPlatformConfigOptional.isPresent()) {
                    buildPlatformConfig = buildPlatformConfigOptional.get();
                } else {
                    return Result.err("Could not find build platform " +
                            buildPlatform.getId() +
                            " for job: " +
                            testJob.getName());
                }

                final Optional<TaskConfig> buildTaskConfigOptional = findTask(
                        buildPlatformConfig.getTasks(),
                        buildTask.getId()
                );

                if (buildTaskConfigOptional.isPresent()) {
                    buildTaskConfig = buildTaskConfigOptional.get();
                } else {
                    return Result.err("Could not find build task " +
                            buildTask.getId() +
                            " for job: " +
                            testJob.getName());
                }

                final Optional<VariantsConfig> buildVariantsOptional = findVariants(
                        buildTaskConfig.getVariants(),
                        buildVariants
                );

                if (buildVariantsOptional.isPresent()) {
                    buildVariantsConfig = buildVariantsOptional.get();
                } else {
                    return Result.err("Could not find build variants for job: " + testJob.getName());
                }


                final PlatformConfig testPlatformConfig = findOrCreatePlatform(
                        buildVariantsConfig.getPlatforms(),
                        testPlatform.getId(),
                        platformProvider
                );
                final TaskConfig testTaskConfig = findOrCreateTask(testPlatformConfig.getTasks(), testTask.getId());

                final Optional<VariantsConfig> testVariantsConfigOptional = findVariants(
                        testTaskConfig.getVariants(),
                        testVariants
                );

                if (testVariantsConfigOptional.isPresent()) {
                    return Result.err("Duplicate test job: " + testJob.getName());
                } else {
                    testTaskConfig.getVariants().add(new VariantsConfig(testVariants, new HashSet<>()));
                }
            }
            return Result.ok(new JDKProject(
                    projectRepresentingJob.getProjectName(),
                    projectRepresentingJob.getProduct(),
                    pullJob.getRepoUrl(),
                    projectRepresentingJob.getBuildProviders()
                            .stream()
                            .map(BuildProvider::getId)
                            .collect(Collectors.toSet()),
                    new JobConfiguration(buildPlatforms),
                    projectRepresentingJob.getProjectVariables()
            ));
        });
    }

    public Result<JDKTestProject, String> parseJDKTestProjectJobs(Set<TestJob> jobs) {
        return validateJobs(jobs, getTestJobValidator()).flatMap(
                projectRepresentingJob -> {
                    final Set<BuildPlatformConfig> buildPlatforms = new HashSet<>();
                    for (final TestJob job : jobs) {
                        final String platformProvider = job.getPlatformProvider();
                        final Platform bp = job.getBuildPlatform();
                        final Map<String, String> bv = variantsMapToStringMap(job.getBuildVariants());
                        final Platform tp = job.getPlatform();
                        final Task tt = job.getTask();
                        final Map<String, String> tv = variantsMapToStringMap(job.getVariants());
                        final VariantsConfig testVariants;

                        final BuildPlatformConfig bpc = findOrCreatePlatform(buildPlatforms, bp.getId());
                        final VariantsConfig buildVariants = findOrCreateVariants(bpc.getVariants(), bv);
                        final PlatformConfig testPlatform = findOrCreatePlatform(buildVariants.getPlatforms(), tp.getId(), platformProvider);
                        final TaskConfig task = findOrCreateTask(testPlatform.getTasks(), tt.getId());

                        final Optional<VariantsConfig> tvOptional = findVariants(task.getVariants(), tv);

                        if (tvOptional.isPresent()) {
                            return Result.err("Duplicate job: " + job.getName());
                        } else {
                            testVariants = new VariantsConfig(tv);
                            task.getVariants().add(testVariants);
                        }
                    }
                    return Result.ok(new JDKTestProject(
                            projectRepresentingJob.getProjectName(),
                            projectRepresentingJob.getProduct(),
                            projectRepresentingJob.getBuildProviders().stream().map(BuildProvider::getId).collect(Collectors.toSet()),
                            projectRepresentingJob.getProjectSubpackageBlacklist(),
                            projectRepresentingJob.getProjectSubpackageWhitelist(),
                            new TestJobConfiguration(buildPlatforms),
                            projectRepresentingJob.getProjectVariables()
                    ));
                });
    }
}
