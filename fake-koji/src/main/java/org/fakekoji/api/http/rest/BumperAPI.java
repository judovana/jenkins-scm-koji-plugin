package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobModifier;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.PlatformBumper;
import org.fakekoji.jobmanager.ProductBumper;
import org.fakekoji.jobmanager.TaskVariantAdder;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.jobmanager.project.ReverseJDKProjectParser;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.BUMP;
import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.PLATFORMS;
import static org.fakekoji.api.http.rest.OToolService.PRODUCTS;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValue;
import static org.fakekoji.api.http.rest.RestUtils.extractProducts;
import static org.fakekoji.api.http.rest.RestUtils.extractProjectIds;

public class BumperAPI implements EndpointGroup {
    private static final String ADD_VARIANT = "/addVariant";

    private final ConfigManager configManager;
    private final File buildsRoot;
    private final JobUpdater jobUpdater;
    private final JDKProjectParser parser;
    private final ReverseJDKProjectParser reverseParser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final ConfigReader<JDKVersion> jdkVersionConfigReader;
    private final ConfigReader<Platform> platformConfigReader;

    BumperAPI(final AccessibleSettings settings) {
        this.configManager = settings.getConfigManager();
        buildsRoot = settings.getDbFileRoot();
        this.jobUpdater = settings.getJobUpdater();
        this.parser = settings.getJdkProjectParser();
        this.reverseParser = settings.getReverseJDKProjectParser();
        this.jdkProjectManager = settings.getConfigManager().jdkProjectManager;
        this.jdkTestProjectManager = settings.getConfigManager().jdkTestProjectManager;
        jdkVersionConfigReader = new ConfigReader<>(settings.getConfigManager().jdkVersionManager);
        platformConfigReader = new ConfigReader<>(settings.getConfigManager().platformManager);
    }

    private Result<List<Project>, OToolError> checkProjectIds(final List<String> projectIds) {
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

    Result<JobUpdateResults, OToolError> modifyJobs(final Collection<Project> projects, final JobModifier jobModifier) {
        final Set<Job> jobs = new HashSet<>();
        try {
            for (final Project project : projects) {
                final Set<Job> projectJobs = parser.parse(project);
                jobs.addAll(projectJobs);
            }
            final Set<Tuple<Job, Optional<Job>>> jobTuples = jobs.stream()
                    .map(jobModifier.getTransformFunction())
                    .collect(Collectors.toSet());
            final Set<Tuple<Job, Job>> jobsToBump = jobTuples.stream()
                    .filter(jobTuple -> jobTuple.y.isPresent())
                    .map(jobTuple -> new Tuple<>(jobTuple.x, jobTuple.y.get()))
                    .collect(Collectors.toSet());
            final List<JobUpdateResult> checkResults = jobUpdater.checkBumpJobs(jobsToBump);
            if (!checkResults.isEmpty()) {
                return Result.ok(new JobUpdateResults(
                        checkResults,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
            }
            final Set<Job> finalJobs = jobTuples.stream()
                    .map(tuple -> tuple.y.orElseGet(() -> tuple.x))
                    .collect(Collectors.toSet());
            final Map<String, Set<Job>> jobMap = new HashMap<>();
            for (final Job job : finalJobs) {
                final String projectName = job.getProjectName();
                if (!jobMap.containsKey(projectName)) {
                    jobMap.put(projectName, new HashSet<>());
                }
                final Set<Job> projectJobs = jobMap.get(projectName);
                projectJobs.add(job);
            }
            final List<Project> assembledProjects = new ArrayList<>();
            for (final Set<Job> projectJobs : jobMap.values()) {
                final Result<Project, String> result = reverseParser.parseJobs(projectJobs);
                if (result.isError()) {
                    return Result.err(new OToolError(result.getError(), 500));
                } else {
                    assembledProjects.add(result.getValue());
                }
            }
            for (final Project project : assembledProjects) {
                final String id = project.getId();
                switch (project.getType()) {
                    case JDK_PROJECT:
                        jdkProjectManager.update(id, (JDKProject) project);
                        break;
                    case JDK_TEST_PROJECT:
                        jdkTestProjectManager.update(id, (JDKTestProject) project);
                        break;
                }
            }
            JenkinsJobUpdater.wakeUpJenkins();
            return Result.ok(jobUpdater.bump(jobsToBump));

        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }
    }

    private Result<JobUpdateResults, OToolError> bumpPlatform(Map<String, List<String>> paramsMap) {
        final Optional<String> fromOptional = extractParamValue(paramsMap, "from");
        final Optional<String> toOptional = extractParamValue(paramsMap, "to");
        final Optional<String> projectsOptional = extractParamValue(paramsMap, "projects");
        if (!fromOptional.isPresent()) {
            return Result.err(new OToolError("Id of 'from' platform is missing", 400));
        }
        if (!toOptional.isPresent()) {
            return Result.err(new OToolError("Id of 'to' platform is missing", 400));
        }
        if (!projectsOptional.isPresent()) {
            return Result.err(new OToolError("projects are mandatory. Use get/projects?as=list to get them all", 400));
        }
        final String fromId = fromOptional.get();
        final String toId = toOptional.get();
        final List<String> projectIds = new ArrayList<>(Arrays.asList(projectsOptional.get().split(",")));
        return platformConfigReader.read(fromId).flatMap(fromPlatform ->
                platformConfigReader.read(toId).flatMap(toPlatform ->
                        checkProjectIds(projectIds).flatMap(projects -> modifyJobs(
                                projects,
                                new PlatformBumper(fromPlatform, toPlatform))
                        )
                )
        );
    }

    private Result<JobUpdateResults, OToolError> bumpProduct(final Map<String, List<String>> paramsMap) {
        return extractProducts(paramsMap).flatMap(products -> {
            final Product fromProduct = products.x;
            final Product toProduct = products.y;
            return getJDKVersion(fromProduct).flatMap(fromJDK ->
                    getJDKVersion(toProduct).flatMap(toJDK ->
                            extractProjectIds(paramsMap).flatMap(projectIds ->
                                    checkProjectIds(projectIds).flatMap(projects ->
                                            modifyJobs(
                                                    projects,
                                                    new ProductBumper(
                                                            fromProduct.getPackageName(),
                                                            toProduct.getPackageName(),
                                                            fromJDK,
                                                            toJDK
                                                    ))
                                    )
                            )
                    )
            );
        });
    }

    Result<JDKVersion, OToolError> getJDKVersion(final Product product) {
        final String jdkVersionId = product.getJdk();
        final String packageName = product.getPackageName();
        return jdkVersionConfigReader.read(jdkVersionId).flatMap(jdkVersion -> {
            if (!jdkVersion.getPackageNames().contains(packageName)) {
                return Result.err(new OToolError(
                        "JDK version " + jdkVersion.getId() + " doesn't contain package name: " + packageName,
                        400
                ));
            }
            return Result.ok(jdkVersion);
        });
    }

    public static String getHelp() {
        final String prefix = MISC + '/' + BUMP;
        return "\n"
                + prefix + PRODUCTS + "?from=[jdkVersionId,packageName]&to=[jdkVersionId,packageName]&projects=[projectsId1,projectId2,..projectIdN]\n"
                + prefix + PLATFORMS + "?from=[platformId]&to=[platformId]&projects=[projectsId1,projectId2,..projectIdN]\n"
                + MISC + ADD_VARIANT + "?name=[variantName]&type=[BUILD|TEST]&defaultValue=[defualtvalue]&values=[value1,value2,...,valueN]\n";
    }

    Result<JobUpdateResults, OToolError> addTaskVariant(final Map<String, List<String>> params) {
        final Result<String, OToolError> nameResult = RestUtils.extractMandatoryParamValue(params, "name");
        if (nameResult.isError()) {
            return Result.err(nameResult.getError());
        }
        final Result<String, OToolError> typeResult = RestUtils.extractMandatoryParamValue(params, "type");
        if (typeResult.isError()) {
            return Result.err(typeResult.getError());
        }
        final Result<String, OToolError> valuesResult = RestUtils.extractMandatoryParamValue(params, "values");
        if (valuesResult.isError()) {
            return Result.err(valuesResult.getError());
        }
        final Result<String, OToolError> defaultValueResult = RestUtils.extractMandatoryParamValue(params, "defaultValue");
        if (defaultValueResult.isError()) {
            return Result.err(defaultValueResult.getError());
        }
        final String name = nameResult.getValue();
        final Result<Task.Type, String> typeParseResult = Task.Type.parse(typeResult.getValue());
        if (typeParseResult.isError()) {
            return Result.err(new OToolError(typeParseResult.getError(), 400));
        }
        final Task.Type type = typeParseResult.getValue();
        final Collection<TaskVariant> taskVariants;
        final int order;
        try {
            taskVariants = configManager.taskVariantManager.readAll();
            order = taskVariants.stream()
                    .filter(taskVariant -> taskVariant.getType().equals(type))
                    .max(Comparator.comparingInt(TaskVariant::getOrder))
                    .orElseThrow(() -> new StorageException("Error while getting last taskVariant's order"))
                    .getOrder() + 1;
        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        }
        final Set<String> taskVariantValues = taskVariants.stream()
                .flatMap(taskVariant -> taskVariant.getVariants().keySet().stream())
                .collect(Collectors.toSet());

        final List<String> valuesList = Arrays
                .stream(valuesResult.getValue().split(","))
                .collect(Collectors.toList());
        final Set<String> tmp = new HashSet<>();
        for (final String value : valuesList) {
            if (taskVariantValues.contains(value)) {
                return Result.err(new OToolError("Value " + value + " already exists in another task variant", 400));
            }
            if (!tmp.add(value)) {
                return Result.err(new OToolError("Duplicate value: " + value, 400));
            }
        }
        final Map<String, TaskVariantValue> values = valuesList.stream()
                .collect(Collectors.toMap(value -> value, value -> new TaskVariantValue(value, value)));
        final String defaultValue = defaultValueResult.getValue();
        if (!values.containsKey(defaultValue)) {
            return Result.err(new OToolError("Default value '" + defaultValue + "' is not defined in values", 500));
        }
        final TaskVariant taskVariant = new TaskVariant(
                name,
                name,
                type,
                defaultValue,
                order,
                values,
                false
        );
        try {
            configManager.taskVariantManager.create(taskVariant);
        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }
        final Collection<Project> projects;
        try {
            final ConfigCache configCache = new ConfigCache(configManager);
            projects = configCache.getProjects();
        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        }
        if (taskVariant.getType().equals(Task.Type.BUILD)) {
            updateBuildDirs();
        }
        return modifyJobs(projects, new TaskVariantAdder(taskVariant));
    }

    private void updateBuildDirs() {
        Arrays.stream(Objects.requireNonNull(buildsRoot.listFiles())).forEach(packageDir ->
                Arrays.stream(Objects.requireNonNull(packageDir.listFiles())).forEach(versionDir ->
                        Arrays.stream(Objects.requireNonNull(versionDir.listFiles())).forEach(buildDir ->
                                updateBuildDir(packageDir, versionDir, buildDir)
                        )
                )
        );
    }

    private void updateBuildDir(final File packageDir, final File versionDir, final File buildDir) {
        final Optional<File> logsDirOptional;
        final File dataDir = new File(buildDir, "data");
        if (dataDir.exists()) {
            final File logsDir = new File(dataDir, "logs");
            if (logsDir.exists()) {
                logsDirOptional = Optional.of(logsDir);
            } else {
                logsDirOptional = Optional.empty();
            }
        } else {
            logsDirOptional = Optional.empty();
        }
        Arrays.stream(Objects.requireNonNull(buildDir.listFiles())).forEach(archiveDir -> {
            final String archiveName = String.join(
                    "-",
                    packageDir.getName(),
                    versionDir.getName(),
                    buildDir.getName() + "." + archiveDir.getName() + ".tarxz"
            );
            final File archive = new File(archiveDir, archiveName);
            if (!archive.exists()) {
                return;
            }
            final Result<OToolArchive, String> parseResult = OToolParser.create(configManager)
                    .flatMap(parser -> parser.parseArchive(archiveName));
            if (parseResult.isError()) {
                return;
            }
            final OToolArchive oToolArchive = parseResult.getValue();
            final File destFile = new File(archiveDir, oToolArchive.toNiceString());
            final File destDir = new File(buildDir, oToolArchive.getDirectoryName());
            try {
                Files.move(archive.toPath(), destFile.toPath());
                Files.move(archiveDir.toPath(), destDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            logsDirOptional.ifPresent(logsDir -> {
                final File archiveLogsDir = new File(logsDir, archiveDir.getName());
                if (!archiveLogsDir.exists()) {
                    return;
                }
                final File destLogDir = new File(logsDir, oToolArchive.getDirectoryName());
                try {
                    Files.move(archiveLogsDir.toPath(), destLogDir.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void addEndpoints() {
        get(ADD_VARIANT, context -> {
            final Result<JobUpdateResults, OToolError> result = addTaskVariant(context.queryParamMap());
            if (result.isError()) {
                final OToolError error = result.getError();
                context.result(error.message).status(error.code);
            } else {
                context.json(result.getValue());
            }
        });
        get(PLATFORMS, context -> {
            final Result<JobUpdateResults, OToolError> result = bumpPlatform(context.queryParamMap());
            if (result.isError()) {
                final OToolError error = result.getError();
                context.result(error.message).status(error.code);
            } else {
                context.json(result.getValue());
            }
        });
        get(PRODUCTS, context -> {
            final Result<JobUpdateResults, OToolError> result = bumpProduct(context.queryParamMap());
            if (result.isError()) {
                final OToolError error = result.getError();
                context.result(error.message).status(error.code);
            } else {
                context.json(result.getValue());
            }
        });
    }

    static class ConfigReader<T> {

        private final Manager<T> manager;

        ConfigReader(final Manager<T> manager) {
            this.manager = manager;
        }

        boolean contains(final String id) {
            return manager.contains(id);
        }

        Result<T, OToolError> read(final String id) {
            try {
                return Result.ok(manager.read(id));
            } catch (StorageException e) {
                return Result.err(new OToolError(e.getMessage(), 400));
            } catch (ManagementException e) {
                return Result.err(new OToolError(e.getMessage(), 500));
            }
        }
    }
}
