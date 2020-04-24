package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.JobModifier;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.PlatformBumper;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.jobmanager.project.ReverseJDKProjectParser;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.BUMP;
import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.PLATFORMS;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValue;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValues;

public class BumperAPI implements EndpointGroup {

    private final JobUpdater jobUpdater;
    private final JDKProjectParser parser;
    private final ReverseJDKProjectParser reverseParser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final ConfigReader<Platform> platformConfigReader;

    BumperAPI(
            final JobUpdater jobUpdater,
            final JDKProjectParser jdkProjectParser,
            final ReverseJDKProjectParser reverseParser,
            final JDKProjectManager jdkProjectManager,
            final JDKTestProjectManager jdkTestProjectManager,
            final PlatformManager platformManager
    ) {
        this.jobUpdater = jobUpdater;
        this.parser = jdkProjectParser;
        this.reverseParser = reverseParser;
        this.jdkProjectManager = jdkProjectManager;
        this.jdkTestProjectManager = jdkTestProjectManager;
        platformConfigReader = new ConfigReader<>(platformManager);
    }

    private Result<Tuple<List<JDKProject>, List<JDKTestProject>>, OToolError> checkProjectIds(final List<String> projectIds) {
        final List<JDKProject> jdkProjects = new ArrayList<>();
        final List<JDKTestProject> jdkTestProjects = new ArrayList<>();

        try {
            for (final String projectId : projectIds) {

                if (jdkProjectManager.contains(projectId)) {
                    jdkProjects.add(jdkProjectManager.read(projectId));
                    continue;
                }
                if (jdkTestProjectManager.contains(projectId)) {
                    jdkTestProjects.add(jdkTestProjectManager.read(projectId));
                    continue;
                }
                return Result.err(new OToolError("Unknown project: " + projectId, 400));
            }

        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }

        return Result.ok(new Tuple<>(jdkProjects, jdkTestProjects));
    }

    Result<JobUpdateResults, OToolError> modifyJobs(final Tuple<List<JDKProject>, List<JDKTestProject>> projectsTuple, final JobModifier jobModifier) {
        final List<JDKProject> jdkProjects = projectsTuple.x;
        final List<JDKTestProject> jdkTestProjects = projectsTuple.y;
        final List<JobUpdateResult> jobsBumped = new ArrayList<>();
        try {
            for (final JDKProject jdkProject : jdkProjects) {
                final Set<Tuple<Job, Optional<Job>>> jobs = parser.parse(jdkProject)
                        .stream()
                        .map(jobModifier.getTransformFunction())
                        .collect(Collectors.toSet());
                final Set<Tuple<Job, Job>> jobsToBump = jobs.stream()
                        .filter(jobTuple -> jobTuple.y.isPresent())
                        .map(jobTuple -> new Tuple<>(jobTuple.x, jobTuple.y.get()))
                        .collect(Collectors.toSet());
                final Set<Job> finalJobs = jobs.stream()
                        .map(tuple -> tuple.y.orElseGet(() -> tuple.x))
                        .collect(Collectors.toSet());
                final Result<JDKProject, String> result = reverseParser.parseJDKProjectJobs(finalJobs);
                if (result.isError()) {
                    return Result.err(new OToolError(result.getError(), 400));
                }
                final JDKProject transformedJDKProject = result.getValue();
                jdkProjectManager.update(transformedJDKProject.getId(), transformedJDKProject);
                final JobUpdateResults partialResults = jobUpdater.bump(jobsToBump);
                jobsBumped.addAll(partialResults.jobsCreated);
            }
            for (final JDKTestProject jdkTestProject : jdkTestProjects) {
                final Set<Tuple<Job, Optional<Job>>> jobs = parser.parse(jdkTestProject)
                        .stream()
                        .map(jobModifier.getTransformFunction())
                        .collect(Collectors.toSet());
                final Set<Tuple<Job, Job>> jobsToBump = jobs.stream()
                        .filter(jobTuple -> jobTuple.y.isPresent())
                        .map(jobTuple -> new Tuple<>(jobTuple.x, jobTuple.y.get()))
                        .collect(Collectors.toSet());
                final Set<TestJob> finalJobs = jobs.stream()
                        .map(tuple -> tuple.y.orElseGet(() -> tuple.x))
                        .filter(job -> job instanceof TestJob)
                        .map(job -> (TestJob) job)
                        .collect(Collectors.toSet());
                final Result<JDKTestProject, String> result = reverseParser.parseJDKTestProjectJobs(finalJobs);
                if (result.isError()) {
                    return Result.err(new OToolError(result.getError(), 400));
                }
                final JDKTestProject transformedJDKTestProject = result.getValue();
                jdkTestProjectManager.update(transformedJDKTestProject.getId(), transformedJDKTestProject);
                final JobUpdateResults partialResults = jobUpdater.bump(jobsToBump);
                jobsBumped.addAll(partialResults.jobsCreated);
            }
        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }
        return Result.ok(new JobUpdateResults(
                jobsBumped,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
    }

    private Result<JobUpdateResults, OToolError> bumpPlatform(Map<String, List<String>> paramsMap) {
        final Optional<String> fromOptional = extractParamValue(paramsMap, "from");
        final Optional<String> toOptional = extractParamValue(paramsMap, "to");
        final Optional<List<String>> projectsOptional = extractParamValues(paramsMap, "projects");
        if (!fromOptional.isPresent()) {
            return Result.err(new OToolError("Id of 'from' platform is missing", 400));
        }
        if (!toOptional.isPresent()) {
            return Result.err(new OToolError("Id of 'to' platform is missing", 400));
        }
        if (!projectsOptional.isPresent()) {
            return Result.err(new OToolError("projects and project cannot be set both", 400));
        }
        final String fromId = fromOptional.get();
        final String toId = toOptional.get();
        final List<String> projectIds = projectsOptional.get();
        return platformConfigReader.read(fromId).flatMap(fromPlatform ->
                platformConfigReader.read(toId).flatMap(toPlatform ->
                        checkProjectIds(projectIds).flatMap(projects -> modifyJobs(
                                projects,
                                new PlatformBumper(fromPlatform, toPlatform))
                        )
                )
        );
    }

    public String getHelp() {
        return ""
                + MISC + '/' + BUMP + PLATFORMS + "?from=[platformId]&to=[platformId]&projects=[projectsId1,projectId2]\n";
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
