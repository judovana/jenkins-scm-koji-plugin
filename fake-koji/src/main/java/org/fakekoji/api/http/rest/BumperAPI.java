package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.*;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
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
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;

import java.util.*;
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

    private final JobUpdater jobUpdater;
    private final JDKProjectParser parser;
    private final ReverseJDKProjectParser reverseParser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final ConfigReader<JDKVersion> jdkVersionConfigReader;
    private final ConfigReader<Platform> platformConfigReader;

    BumperAPI(
            final JobUpdater jobUpdater,
            final JDKProjectParser jdkProjectParser,
            final ReverseJDKProjectParser reverseParser,
            final JDKProjectManager jdkProjectManager,
            final JDKTestProjectManager jdkTestProjectManager,
            final JDKVersionManager jdkVersionManager,
            final PlatformManager platformManager
    ) {
        this.jobUpdater = jobUpdater;
        this.parser = jdkProjectParser;
        this.reverseParser = reverseParser;
        this.jdkProjectManager = jdkProjectManager;
        this.jdkTestProjectManager = jdkTestProjectManager;
        jdkVersionConfigReader = new ConfigReader<>(jdkVersionManager);
        platformConfigReader = new ConfigReader<>(platformManager);
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

    Result<JobUpdateResults, OToolError> modifyJobs(final List<Project> projects, final JobModifier jobModifier) {
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

    public String getHelp() {
        final String prefix = MISC + '/' + BUMP;
        return "\n"
                + prefix + PRODUCTS + "?from=[jdkVersionId,packageName]&to=[jdkVersionId,packageName]&projects=[projectsId1,projectId2,..projectIdN]\n"
                + prefix + PLATFORMS + "?from=[platformId]&to=[platformId]&projects=[projectsId1,projectId2,..projectIdN]\n";
    }

    @Override
    public void addEndpoints() {
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
