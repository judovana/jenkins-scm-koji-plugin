package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.api.http.rest.args.AddTaskVariantArgs;
import org.fakekoji.api.http.rest.args.RemoveTaskVariantArgs;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.BuildDirUpdater;
import org.fakekoji.jobmanager.BumpResult;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.PlatformBumper;
import org.fakekoji.jobmanager.ProductBumper;
import org.fakekoji.jobmanager.TaskVariantAdder;
import org.fakekoji.jobmanager.TaskVariantRemover;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.BUMP;
import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.PLATFORMS;
import static org.fakekoji.api.http.rest.OToolService.PRODUCTS;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValue;
import static org.fakekoji.api.http.rest.RestUtils.extractProducts;
import static org.fakekoji.api.http.rest.RestUtils.extractProjectIds;
import static org.fakekoji.core.AccessibleSettings.objectMapper;

public class BumperAPI implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final String ADD_VARIANT = "/addVariant";
    private static final String REMOVE_VARIANT = "/removeVariant";

    private final AccessibleSettings settings;
    private final ConfigReader<JDKVersion> jdkVersionConfigReader;
    private final ConfigReader<Platform> platformConfigReader;

    BumperAPI(final AccessibleSettings settings) {
        this.settings = settings;
        jdkVersionConfigReader = new ConfigReader<>(settings.getConfigManager().getJdkVersionManager());
        platformConfigReader = new ConfigReader<>(settings.getConfigManager().getPlatformManager());
    }

    private Result<List<Project>, OToolError> checkProjectIds(final List<String> projectIds) {
        final List<Project> projects = new ArrayList<>();

        try {
            final JDKProjectManager jdkProjectManager = settings.getConfigManager().getJdkProjectManager();
            final JDKTestProjectManager jdkTestProjectManager = settings.getConfigManager().getJdkTestProjectManager();
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
                        checkProjectIds(projectIds).flatMap(projects ->
                                new PlatformBumper(settings, fromPlatform, toPlatform).modifyJobs(projects)
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
                                            new ProductBumper(
                                                    settings,
                                                    fromProduct.getPackageName(),
                                                    toProduct.getPackageName(),
                                                    fromJDK,
                                                    toJDK
                                            ).modifyJobs(projects)
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
                + prefix + PLATFORMS + "?from=[platformId]&to=[platformId]&projects=[projectsId1,projectId2,..projectIdN]&filterOrtasks=[todo]\n"
                + MISC + ADD_VARIANT + "?name=[variantName]&type=[BUILD|TEST]&defaultValue=[defualtvalue]&values=[value1,value2,...,valueN]\n"
                + MISC + REMOVE_VARIANT + "?name=[variantName]\n"
                + "for all bumps you can specify jobCollisionAction=[stop|keep_bumped|keep_existing], default=stop and "
                + "execute=[true|false], default=false"
                + "";
    }

    Result<BumpResult, OToolError> removeTaskVariant(final Map<String, List<String>> params) {
        final ConfigManager configManager = settings.getConfigManager();
        final File buildsRoot = settings.getDbFileRoot();
        return RemoveTaskVariantArgs.parse(params).flatMap(args -> {
            final TaskVariant taskVariant;
            try {
                taskVariant = configManager.getTaskVariantManager().read(args.name);
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
            final TaskVariantRemover remover = new TaskVariantRemover(settings, taskVariant);
            return remover.modifyJobs(projects, args).flatMap(jobBumpResults -> {
                final BuildDirUpdater.BuildUpdateSummary buildUpdateSummary;
                if (taskVariant.getType().equals(Task.Type.BUILD)) {
                    final BuildDirUpdater updater = new BuildDirUpdater(buildsRoot, configManager);
                    updater.updateBuildDirs(remover);
                    buildUpdateSummary = updater.getSummary();
                } else {
                    buildUpdateSummary = null;
                }
                String message;
                try {
                    configManager.getTaskVariantManager().delete(taskVariant.getId());
                    message = null;
                } catch (ManagementException | StorageException e) {
                    final String error = "Failed to delete task variant config: " + e.getMessage();
                    LOGGER.log(Level.SEVERE, error, e);
                    message = error;
                }
                return Result.ok(new BumpResult(jobBumpResults, buildUpdateSummary, message));
            });
        });
    }

    Result<BumpResult, OToolError> addTaskVariant(final Map<String, List<String>> params) {
        final ConfigManager configManager = settings.getConfigManager();
        final File buildsRoot = settings.getDbFileRoot();
        return AddTaskVariantArgs.parse(configManager, params).flatMap(args -> {
            final TaskVariant taskVariant = args.taskVariant;
            try {
                configManager.getTaskVariantManager().create(taskVariant);
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
            final TaskVariantAdder adder = new TaskVariantAdder(settings, taskVariant);
            return adder.modifyJobs(projects, args).flatMap(results -> {
                if (taskVariant.getType().equals(Task.Type.BUILD)) {
                    final BuildDirUpdater buildDirUpdater = new BuildDirUpdater(buildsRoot, configManager);
                    buildDirUpdater.updateBuildDirs(adder);
                    return Result.ok(new BumpResult(results, buildDirUpdater.getSummary()));
                } else {
                    return Result.ok(new BumpResult(results));
                }
            });
        });
    }

    @Override
    public void addEndpoints() {
        get(ADD_VARIANT, context -> {
            final Result<BumpResult, OToolError> result = addTaskVariant(context.queryParamMap());
            if (result.isError()) {
                final OToolError error = result.getError();
                context.result(error.message).status(error.code);
            } else {
                context.result(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(result.getValue()));
            }
        });
        get(REMOVE_VARIANT, context -> {
            final Result<BumpResult, OToolError> result = removeTaskVariant(context.queryParamMap());
            if (result.isError()) {
                final OToolError error = result.getError();
                context.result(error.message).status(error.code);
            } else {
                context.result(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(result.getValue()));
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
