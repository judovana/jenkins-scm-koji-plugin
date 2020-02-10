package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJackson;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.MatrixGenerator;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.get;

import static org.fakekoji.core.AccessibleSettings.objectMapper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OToolService {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final String ID = "id";
    private static final String CONFIG_ID = "/:" + ID;
    private static final String BUILD_PROVIDERS = "/buildProviders";
    private static final String BUILD_PROVIDER = BUILD_PROVIDERS + CONFIG_ID;
    private static final String JDK_VERSIONS = "/jdkVersions";
    private static final String JDK_VERSION = JDK_VERSIONS + CONFIG_ID;
    private static final String PLATFORMS = "/platforms";
    private static final String PLATFORM = PLATFORMS + CONFIG_ID;
    private static final String TASKS = "/tasks";
    private static final String TASK = TASKS + CONFIG_ID;
    private static final String TASK_VARIANTS = "/taskVariants";
    private static final String TASK_VARIANT = TASK_VARIANTS + CONFIG_ID;
    private static final String JDK_PROJECTS = "/jdkProjects";
    private static final String JDK_PROJECT = JDK_PROJECTS + CONFIG_ID;
    private static final String JDK_TEST_PROJECTS = "/jdkTestProjects";
    private static final String JDK_TEST_PROJECT = JDK_TEST_PROJECTS + CONFIG_ID;

    private static final String MISC = "misc";
    private static final String REGENERATE_ALL = "regenerateAll";
    private static final String MATRIX = "matrix";

    private final int port;
    private final Javalin app;
    private final JobUpdater jenkinsJobUpdater;

    public OToolService(AccessibleSettings settings) {
        this.port = settings.getWebappPort();
        JavalinJackson.configure(objectMapper);
        app = Javalin.create(config -> config
                .addStaticFiles("/webapp")
        );
        jenkinsJobUpdater = new JenkinsJobUpdater(settings);
        final ConfigManager configManager = ConfigManager.create(settings.getConfigRoot().getAbsolutePath());

        final OToolHandlerWrapper wrapper = oToolHandler -> context -> {
            try {
                oToolHandler.handle(context);
            } catch (ManagementException e) {
                context.status(400).result(e.getMessage());
            } catch (StorageException e) {
                context.status(500).result(e.getMessage());
            } catch (Exception e) {
                context.status(501).result(e.getMessage());
            }
        };

        app.routes(() -> {

            final JDKTestProjectManager jdkTestProjectManager = new JDKTestProjectManager(
                    configManager.getJdkTestProjectStorage(),
                    jenkinsJobUpdater
            );
            final JDKProjectManager jdkProjectManager = new JDKProjectManager(
                    configManager,
                    jenkinsJobUpdater,
                    settings.getLocalReposRoot(),
                    settings.getScriptsRoot()
            );

            path(MISC, () -> {
                path(REGENERATE_ALL, () -> {
                    get(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                        JobUpdateResults r1 = jdkTestProjectManager.regenerateAll();
                        context.status(200).json(r1);
                    }));
                    get(JDK_PROJECTS, wrapper.wrap(context -> {
                        JobUpdateResults r2 = jdkProjectManager.regenerateAll();
                        context.status(200).json(r2);
                    }));
                });
                get(MATRIX, wrapper.wrap(context -> {
                    MatrixGenerator m = new MatrixGenerator(settings, configManager);
                    context.status(200).result(m.printMatrix());
                }));
            });

            app.post(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final ManagementResult result = jdkTestProjectManager.create(jdkTestProject);
                context.status(200).json(result);
            }));
            app.get(JDK_TEST_PROJECTS, wrapper.wrap(context -> context.json(jdkTestProjectManager.readAll())));
            app.put(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final String id = context.pathParam(ID);
                final ManagementResult result = jdkTestProjectManager.update(id, jdkTestProject);
                context.status(200).json(result);
            }));
            app.delete(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final ManagementResult result = jdkTestProjectManager.delete(id);
                context.status(200).json(result);
            }));

            final BuildProviderManager buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
            app.get(BUILD_PROVIDERS, context -> context.json(buildProviderManager.readAll()));

            final JDKVersionManager JDKVersionManager = new JDKVersionManager(configManager.getJdkVersionStorage());
            app.get(JDK_VERSIONS, context -> context.json(JDKVersionManager.readAll()));

            final PlatformManager platformManager = new PlatformManager(configManager.getPlatformStorage(), jenkinsJobUpdater);
            app.get(PLATFORMS, context -> context.json(platformManager.readAll()));
            app.post(PLATFORMS, context -> {
                try {
                    final Platform platform = context.bodyValidator(Platform.class).get();
                    final ManagementResult<Platform> result = platformManager.create(platform);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.put(PLATFORM, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final Platform platform = context.bodyValidator(Platform.class).get();
                    final ManagementResult<Platform> result = platformManager.update(id, platform);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(PLATFORM, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<Platform> result = platformManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });

            final TaskManager taskManager = new TaskManager(configManager.getTaskStorage(), jenkinsJobUpdater);
            app.post(TASKS, context -> {
                try {
                    final Task task = context.bodyValidator(Task.class).get();
                    final ManagementResult<Task> result = taskManager.create(task);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.get(TASKS, context -> context.json(taskManager.readAll()));
            app.put(TASK, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final Task task = context.bodyValidator(Task.class).get();
                    final ManagementResult<Task> result = taskManager.update(id, task);
                    context.json(result).status(200);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(TASK, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<Task> result = taskManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });

            final TaskVariantManager taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
            app.get(TASK_VARIANTS, context -> context.json(taskVariantManager.readAll()));

            app.post(JDK_PROJECTS, context -> {
                try {
                    final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                    final ManagementResult<JDKProject> result = jdkProjectManager.create(jdkProject);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.get(JDK_PROJECTS, context -> context.json(jdkProjectManager.readAll()));
            app.put(JDK_PROJECT, context -> {
                try {
                    final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                    final String id = context.pathParam(ID);
                    final ManagementResult<JDKProject> result = jdkProjectManager.update(id, jdkProject);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(JDK_PROJECT, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<JDKProject> result = jdkProjectManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
        });
    }

    public void start() {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }

    public int getPort() {
        return port;
    }

    interface OToolHandler {
        void handle(Context context) throws ManagementException, StorageException;
    }

    interface OToolHandlerWrapper {
        Handler wrap(OToolHandler oToolHandler);
    }
}
