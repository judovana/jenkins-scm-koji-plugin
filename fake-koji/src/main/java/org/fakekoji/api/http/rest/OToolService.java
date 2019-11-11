package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.ProductManager;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;

import java.io.File;

public class OToolService {

    private static final String ID = "id";
    private static final String CONFIG_ID = "/:" + ID;
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

    public OToolService(AccessibleSettings settings) {
        this.port = settings.getWebappPort();
        app = Javalin.create(config -> config
                .addStaticFiles("/webapp")
        );
        jenkinsJobUpdater = new JenkinsJobUpdater(settings);
        final ConfigManager configManager = ConfigManager.create(settings.getConfigRoot().getAbsolutePath());
        app.routes(() -> {

            final BuildProviderManager buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
            app.get(BUILD_PROVIDERS, context -> context.json(buildProviderManager.readAll()));

            final ProductManager productManager = new ProductManager(configManager.getProductStorage());
            app.get(PRODUCTS, context -> context.json(productManager.readAll()));

            final PlatformManager platformManager = new PlatformManager(configManager.getPlatformStorage());
            app.get(PLATFORMS, context -> context.json(platformManager.readAll()));

            final TaskManager taskManager = new TaskManager(configManager, jenkinsJobUpdater);
            app.post(TASKS, context -> {
                try {
                    final Task task = context.bodyValidator(Task.class).get();
                    taskManager.create(task);
                    context.status(200);
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
                    taskManager.update(id, task);
                    context.status(200);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(TASK, context -> {
                try {
                    final String id = context.pathParam(ID);
                    taskManager.delete(id);
                    context.status(200);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });

            final TaskVariantManager taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
            app.get(TASK_VARIANTS, context -> context.json(taskVariantManager.readAll()));

            final JDKProjectManager jdkProjectManager = new JDKProjectManager(
                    configManager,
                    jenkinsJobUpdater,
                    settings.getLocalReposRoot(),
                    settings.getScriptsRoot()
            );
            app.post(JDK_PROJECTS, context -> {
                try {
                    final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                    jdkProjectManager.create(jdkProject);
                    context.status(200);
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
                    jdkProjectManager.update(id, jdkProject);
                    context.status(200);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(JDK_PROJECT, context -> {
                try {
                    final String id = context.pathParam(ID);
                    jdkProjectManager.delete(id);
                    context.status(200);
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
}
