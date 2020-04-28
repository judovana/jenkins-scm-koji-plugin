package org.fakekoji.jobmanager.project;

import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.BuildPlatformConfig;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobConfiguration;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.model.TestJobConfiguration;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReverseJDKProjectParser {

    public Result<Project, String> parseJobs(Set<Job> jobs) {
        if (jobs.stream().allMatch(job -> job instanceof TestJob)) {
            final Set<TestJob> testJobs = jobs.stream().map(job -> (TestJob) job).collect(Collectors.toSet());
            return parseJDKTestProjectJobs(testJobs).flatMap(project -> Result.ok(((Project) project)));
        } else {
            return parseJDKProjectJobs(jobs).flatMap(project -> Result.ok((Project) project));
        }
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

    /**
     * Searches for config by predicate, return Optional
     *
     */
    static <T> Optional<T> findConfig(Set<T> configs, Predicate<T> predicate) {
        return configs.stream()
                .filter(predicate)
                .findFirst();
    }

    /**
     * Searches for config by predicate, if none is found, creates new with onCreate
     *
     */
    static <T> T findOrCreateConfig(Set<T> configs, Predicate<T> predicate, Supplier<T> onCreate) {
        final Optional<T> configOptional = findConfig(configs, predicate);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            final T t = onCreate.get();
            configs.add(t);
            return t;
        }
    }

    private static Predicate<PlatformConfig> getPlatformConfigPredicate(
            final String platformId,
            final String platformProvider
    ) {
        return config -> config.getId().equals(platformId) && config.getProvider().equals(platformProvider);
    }

    private static Predicate<BuildPlatformConfig> getBuildPlatformConfigPredicate(final String platformId) {
        return config -> config.getId().equals(platformId);
    }

    private static Predicate<TaskConfig> getTaskConfigPredicate(final String taskId) {
        return config -> config.getId().equals(taskId);
    }

    private static Predicate<VariantsConfig> getVariantsConfigPredicate(final Map<String, String> map) {
        return config -> config.getMap().equals(map);
    }

    private static Optional<PlatformConfig> findPlatform(Set<PlatformConfig> configs, String id, String provider) {
        return findConfig(configs, getPlatformConfigPredicate(id, provider));
    }

    private static PlatformConfig findOrCreatePlatform(Set<PlatformConfig> configs, String id, String provider) {
        return findOrCreateConfig(
                configs,
                getPlatformConfigPredicate(id, provider),
                () -> new PlatformConfig(id, new HashSet<>(), provider)
        );
    }

    private static BuildPlatformConfig findOrCreatePlatform(Set<BuildPlatformConfig> configs, String id) {
        return findOrCreateConfig(
                configs,
                getBuildPlatformConfigPredicate(id),
                () -> new BuildPlatformConfig(id, new HashSet<>())
        );
    }

    private static Optional<TaskConfig> findTask(Set<TaskConfig> configs, String id) {
        return findConfig(configs, config -> config.getId().equals(id));
    }

    private static TaskConfig findOrCreateTask(Set<TaskConfig> configs, String id) {
        return findOrCreateConfig(
                configs,
                getTaskConfigPredicate(id),
                () -> new TaskConfig(id, new HashSet<>())
        );
    }

    private static Optional<VariantsConfig> findVariants(Set<VariantsConfig> configs, Map<String, String> map) {
        return findConfig(configs, config -> config.getMap().equals(map));
    }

    private static VariantsConfig findOrCreateVariants(Set<VariantsConfig> configs, Map<String, String> map) {
        return findOrCreateConfig(
                configs,
                getVariantsConfigPredicate(map),
                () -> new VariantsConfig(map, new HashSet<>())
        );
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
}
