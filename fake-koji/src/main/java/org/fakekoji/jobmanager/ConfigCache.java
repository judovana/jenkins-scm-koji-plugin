package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.StorageException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigCache {

    private final Map<String, BuildProvider> buildProviderMap;
    private final Map<String, JDKProject> jdkProjectMap;
    private final Map<String, JDKTestProject> jdkTestProjectMap;
    private final Map<String, JDKVersion> jdkVersionMap;
    private final Map<String, Platform> platformMap;
    private final Map<String, Task> taskMap;
    private final Map<String, TaskVariant> testVariantMap;
    private final Map<String, TaskVariant> buildVariantMap;


    public ConfigCache(final ManagerWrapper managerWrapper) throws StorageException {
        buildProviderMap = managerWrapper.buildProviderManager
                .readAll()
                .stream()
                .collect(Collectors.toMap(BuildProvider::getId, buildProvider -> buildProvider));
        buildVariantMap = managerWrapper.taskVariantManager
                .readAll()
                .stream()
                .filter(variant -> variant.getType().equals(Task.Type.BUILD))
                .collect(Collectors.toMap(TaskVariant::getId, taskVariant -> taskVariant));
        jdkProjectMap = managerWrapper.jdkProjectManager
                .readAll()
                .stream()
                .collect(Collectors.toMap(JDKProject::getId, jdkProject -> jdkProject));
        jdkTestProjectMap = managerWrapper.jdkTestProjectManager
                .readAll()
                .stream()
                .collect(Collectors.toMap(JDKTestProject::getId, jdkTestProject-> jdkTestProject));
        jdkVersionMap = managerWrapper.jdkVersionManager
                .readAll()
                .stream()
                .collect(Collectors.toMap(JDKVersion::getId, jdkVersion -> jdkVersion));
        platformMap = managerWrapper.platformManager
                .readAll()
                .stream()
                .collect(Collectors.toMap(Platform::getId, platform -> platform));
        taskMap = managerWrapper.taskManager.readAll()
                .stream()
                .collect(Collectors.toMap(Task::getId, task -> task));
        testVariantMap = managerWrapper.taskVariantManager
                .readAll()
                .stream()
                .filter(variant -> variant.getType().equals(Task.Type.TEST))
                .collect(Collectors.toMap(TaskVariant::getId, taskVariant -> taskVariant));

    }

    public Collection<BuildProvider> getBuildProviders() {
        return buildProviderMap.values();
    }

    public Optional<BuildProvider> getBuildProvider(final String id) {
        return Optional.ofNullable(buildProviderMap.get(id));
    }

    public Collection<JDKProject> getJdkProjects() {
        return jdkProjectMap.values();
    }

    public Optional<JDKProject> getJdkProjects(final String id) {
        return Optional.ofNullable(jdkProjectMap.get(id));
    }

    public Collection<JDKTestProject> getJdkTestProjects() {
        return jdkTestProjectMap.values();
    }

    public Optional<JDKTestProject> getJdkTestProject(final String id) {
        return Optional.ofNullable(jdkTestProjectMap.get(id));
    }

    public Collection<Project> getProjects() {
        return Stream.concat(
                getJdkProjects().stream(),
                getJdkTestProjects().stream()
        ).collect(Collectors.toList());
    }

    public Collection<JDKVersion> getJdkVersions() {
        return jdkVersionMap.values();
    }

    public Optional<JDKVersion> getJdkVersion(final String id) {
        return Optional.ofNullable(jdkVersionMap.get(id));
    }

    public Collection<Platform> getPlatforms() {
        return platformMap.values();
    }

    public Optional<Platform> getPlatform(final String id) {
        return Optional.ofNullable(platformMap.get(id));
    }

    public Collection<Task> getTasks() {
        return taskMap.values();
    }

    public Optional<Task> getTask(final String id) {
        return Optional.ofNullable(taskMap.get(id));
    }

    public Collection<TaskVariant> getBuildTaskVariants() {
        return buildVariantMap.values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public Optional<TaskVariant> getBuildTaskVariant(final String id) {
        return Optional.ofNullable(buildVariantMap.get(id));
    }

    public Collection<TaskVariant> getTestTaskVariants() {
        return testVariantMap.values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public Optional<TaskVariant> getTestTaskVariant(final String id) {
        return Optional.ofNullable(testVariantMap.get(id));
    }

    public List<TaskVariant> getTaskVariants() {
        return Stream.concat(getTestTaskVariants().stream(), getBuildTaskVariants().stream())
                .collect(Collectors.toList());
    }
}
