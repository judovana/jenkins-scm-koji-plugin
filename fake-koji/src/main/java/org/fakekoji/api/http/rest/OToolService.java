package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.ProductManager;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.model.Task;

import java.io.File;

public class OToolService {

    private static final String ID = "id";
    private static final String CONFIG_ID = "/:ID";
    private static final String BUILD_PROVIDERS = "/buildProviders";
    private static final String BUILD_PROVIDER = BUILD_PROVIDERS + CONFIG_ID;
    private static final String PRODUCTS = "/products";
    private static final String PRODUCT = PRODUCTS + CONFIG_ID;
    private static final String PLATFORMS = "/platforms";
    private static final String PLATFORM = PLATFORMS + CONFIG_ID;
    private static final String TASKS = "/tasks";
    private static final String TASK = TASKS + CONFIG_ID;
    private static final String TASK_VARIANTS = "/taskVariants";
    private static final String TASK_VARIANT = TASK_VARIANTS + CONFIG_ID;
    private static final String JDK_PROJECTS = "/jdkProjects";
    private static final String JDK_PROJECT = JDK_PROJECTS + CONFIG_ID;

    private final int port;
    private final Javalin app;
    private final JobUpdater jenkinsJobUpdater;

    public OToolService(
            final int port,
            final ConfigManager configManager,
            final File jenkinsJobsRoot,
            final File jenkinsJobArchiveRoot,
            final File reposRoot,
            final File scriptsRoot
            ) {
        this.port = port;
        app = Javalin.create(JavalinConfig::enableCorsForAllOrigins);
        jenkinsJobUpdater = new JenkinsJobUpdater(jenkinsJobsRoot, jenkinsJobArchiveRoot);
        app.routes(() -> {

            final BuildProviderManager buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
            app.get(BUILD_PROVIDERS, context -> context.json(buildProviderManager.readAll()));

            final ProductManager productManager = new ProductManager(configManager.getProductStorage());
            app.get(PRODUCTS, context -> context.json(productManager.readAll()));

            final PlatformManager platformManager = new PlatformManager(configManager.getPlatformStorage());
            app.get(PLATFORMS, context -> context.json(platformManager.readAll()));

            final TaskManager taskManager = new TaskManager(configManager.getTaskStorage());
            app.post(TASKS, context -> {
                final Task task = context.bodyValidator(Task.class).get();
                taskManager.create(task);
                context.status(201);
            });
            app.get(TASKS, context -> context.json(taskManager.readAll()));
            app.put(TASK, context -> {
                final String id = context.pathParam(ID);
                final Task task = context.bodyValidator(Task.class).get();
                taskManager.update(id, task);
            });
            app.delete(TASK, context -> {
                final String id = context.pathParam(ID);
                taskManager.delete(id);
            });

            final TaskVariantManager taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
            app.get(TASK_VARIANTS, context -> context.json(taskVariantManager.readAll()));

            final JDKProjectManager jdkProjectManager = new JDKProjectManager(
                    configManager,
                    jenkinsJobUpdater,
                    reposRoot,
                    scriptsRoot
            );
            app.post(JDK_PROJECTS, context -> {
                final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                System.out.println(jdkProject);
                jdkProjectManager.create(jdkProject);
            });
            app.get(JDK_PROJECTS, context -> context.json(jdkProjectManager.readAll()));
            app.put(JDK_PROJECT, context -> {
                final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                final String id = context.pathParam(ID);
                jdkProjectManager.update(id, jdkProject);
            });
            app.delete(JDK_PROJECT, context -> {
                final String id = context.pathParam(ID);
                jdkProjectManager.delete(id);
            });
        });
    }

    public void start() {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }
}
