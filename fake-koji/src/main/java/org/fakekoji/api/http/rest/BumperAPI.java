package org.fakekoji.api.http.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import org.fakekoji.api.http.rest.args.AddTaskVariantArgs;
import org.fakekoji.api.http.rest.args.BumpPlatformArgs;
import org.fakekoji.api.http.rest.args.ProviderBumpArgs;
import org.fakekoji.api.http.rest.args.RemoveTaskVariantArgs;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.BuildDirUpdater;
import org.fakekoji.jobmanager.bumpers.BumpResult;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.bumpers.impl.PlatformBumper;
import org.fakekoji.jobmanager.bumpers.impl.ProductBumper;
import org.fakekoji.jobmanager.bumpers.impl.TaskVariantAdder;
import org.fakekoji.jobmanager.bumpers.impl.TaskVariantRemover;
import org.fakekoji.jobmanager.bumpers.impl.VariantBumper;
import org.fakekoji.jobmanager.model.JobCollisionAction;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.PlatformBumpVariant;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;

import static org.fakekoji.api.http.rest.OToolService.BUMP;
import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.PLATFORMS;
import static org.fakekoji.api.http.rest.OToolService.PRODUCTS;
import static org.fakekoji.api.http.rest.RestUtils.extractProducts;
import static org.fakekoji.api.http.rest.RestUtils.extractProjectIds;
import static org.fakekoji.core.AccessibleSettings.objectMapper;

public class BumperAPI implements EndpointGroup {

    public static final String BUMP_FROM = "from";
    public static final String BUMP_TO = "to";
    public static final String FILTER = "filter";
    public static final String EXECUTE = "do";
    public static final String JOB_COLLISION_ACTION = "jobCollisionAction";
    public static final String PLATFORM_BUMP_VARIANT = "taskType";
    public static final String PROJECTS = "projects";
    public static final String PROVIDERS = "/providers";

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final String ADD_VARIANT = "/addVariant";
    private static final String REMOVE_VARIANT = "/removeVariant";

    private final AccessibleSettings settings;
    private final ConfigReader<JDKVersion> jdkVersionConfigReader;

    BumperAPI(final AccessibleSettings settings) {
        this.settings = settings;
        jdkVersionConfigReader = new ConfigReader<>(settings.getConfigManager().jdkVersionManager);
    }

    private Result<JobUpdateResults, OToolError> bumpProvider(Map<String, List<String>> paramsMap) throws StorageException {
        ProviderBumpArgs args;
        try{
            args =  new ProviderBumpArgs(paramsMap, settings);
        } catch (Exception ex) {
            return Result.err(new OToolError(
                    ex.getMessage(),
                    400
            ));
        }
        return new VariantBumper(settings, args.originalProvider, args.targetProvider, args.filter).modifyJobs(args.projects, args);
    }

    private Result<JobUpdateResults, OToolError> bumpPlatform(Map<String, List<String>> paramsMap) {
        return BumpPlatformArgs.parse(settings.getConfigManager(), paramsMap).flatMap(args ->
                new PlatformBumper(settings, args.from, args.to, args.variant, args.filter).modifyJobs(args.projects, args)
        );
    }

    private Result<JobUpdateResults, OToolError> bumpProduct(final Map<String, List<String>> paramsMap) {
        return extractProducts(paramsMap).flatMap(products -> {
            final Product fromProduct = products.x;
            final Product toProduct = products.y;
            return getJDKVersion(fromProduct).flatMap(fromJDK ->
                    getJDKVersion(toProduct).flatMap(toJDK ->
                            extractProjectIds(paramsMap).flatMap(projectIds ->
                                    settings.getConfigManager().getProjects(projectIds).flatMap(projects ->
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
        final String jobCollisionActionsHelp = JOB_COLLISION_ACTION + "=[" + JobCollisionAction.stringValues("|") + "]";
        final String platformBumpVariantsHelp = PLATFORM_BUMP_VARIANT + "=[" + PlatformBumpVariant.stringValues("|") + "]";
        final String prefix = MISC + '/' + BUMP;
        final String projectsHelp = PROJECTS + "[projectsId1,projectId2,..projectIdN]";
        return "\n"
                + prefix + PRODUCTS + "?" + BUMP_FROM + "=[jdkVersionId,packageName]&" + BUMP_TO + "=[jdkVersionId,packageName]&" + projectsHelp + "\n"
                + prefix + PLATFORMS + "?" + BUMP_FROM + "=[platformId]&" + BUMP_TO + "=[platformId]&" + projectsHelp + "&" + platformBumpVariantsHelp + "]&" + FILTER + "=[regex]\n"
                + prefix + PROVIDERS + "?" + BUMP_FROM + "=[provider]&" + BUMP_TO + "=[provider]&" + projectsHelp + "&" + FILTER + "=[regex] - be sure to go job by job\n"
                + MISC + ADD_VARIANT + "?name=[variantName]&type=[BUILD|TEST]&defaultValue=[defualtvalue]&values=[value1,value2,...,valueN]\n"
                + MISC + REMOVE_VARIANT + "?name=[variantName]\n"
                + "  for all bumps you can specify " + jobCollisionActionsHelp + ", default=stop and " + EXECUTE + "=[true|false], default=false" + "\n"
                + "    From " + JOB_COLLISION_ACTION + " the " + JobCollisionAction.KEEP_BUMPED.value
                + "  is usually the one you need; X -> Y = " + JobCollisionAction.KEEP_EXISTING.value + " - archived is X ; " + JobCollisionAction.KEEP_BUMPED.value + ": archived is Y and  X is renamed to Y \n";
    }

    Result<BumpResult, OToolError> removeTaskVariant(final Map<String, List<String>> params) {
        final ConfigManager configManager = settings.getConfigManager();
        final File buildsRoot = settings.getDbFileRoot();
        return RemoveTaskVariantArgs.parse(params).flatMap(args -> {
            final TaskVariant taskVariant;
            try {
                taskVariant = configManager.taskVariantManager.read(args.name);
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
                    configManager.taskVariantManager.delete(taskVariant.getId());
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

    private <T> void handleBumpResult(
            final Context context,
            final Result<T, OToolError> result
    ) throws JsonProcessingException {
        if (result.isError()) {
            final OToolError error = result.getError();
            context.result(error.message).status(error.code);
        } else {
            context.result(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(result.getValue()));
        }
    }

    @Override
    public void addEndpoints() {
        get(ADD_VARIANT, context -> handleBumpResult(context, addTaskVariant(context.queryParamMap())));
        get(REMOVE_VARIANT, context -> handleBumpResult(context, removeTaskVariant(context.queryParamMap())));
        get(PLATFORMS, context -> handleBumpResult(context, bumpPlatform(context.queryParamMap())));
        get(PRODUCTS, context -> handleBumpResult(context, bumpProduct(context.queryParamMap())));
        get(PROVIDERS, context -> handleBumpResult(context, bumpProvider(context.queryParamMap())));
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
