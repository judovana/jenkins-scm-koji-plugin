package org.fakekoji.api.http.rest;

import org.fakekoji.jobmanager.JenkinsViewTemplateBuilder;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class ViewsAppi {

    @NotNull
    static  List<JenkinsViewTemplateBuilder> getJenkinsViewTemplateBuilders(JDKTestProjectManager jdkTestProjectManager, JDKProjectManager jdkProjectManager, PlatformManager platformManager, TaskManager taskManager) throws StorageException, IOException {
        List<JDKTestProject> jdkTestProjecs = jdkTestProjectManager.readAll();
        List<JDKProject> jdkProjects = jdkProjectManager.readAll();
        List<Platform> allPlatforms = platformManager.readAll();
        List<Task> allTasks = taskManager.readAll();
        List<String> projects = new ArrayList<>();
        for (JDKTestProject p : jdkTestProjecs) {
            projects.add(p.getId());
        }
        for (JDKProject p : jdkProjects) {
            projects.add(p.getId());
        }
        Set<String> ossesSet = new HashSet<>();
        Set<String> ossesVersionedSet = new HashSet<>();
        Set<String> archesSet = new HashSet<>();
        Set<JenkinsViewTemplateBuilder.VersionlessPlatform> versionlessPlatformsSet = new HashSet<>();
        for (Platform p : allPlatforms) {
            ossesSet.add(p.getOs());
            ossesVersionedSet.add(p.getOs() + p.getVersion());
            archesSet.add(p.getArchitecture());
            versionlessPlatformsSet.add(new JenkinsViewTemplateBuilder.VersionlessPlatform(p.getOs(), p.getArchitecture()));
        }
        //jenkins will resort any way, however..
        Collections.sort(projects);
        Collections.sort(allTasks, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        Collections.sort(allPlatforms, new Comparator<Platform>() {
            @Override
            public int compare(Platform o1, Platform o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        List<String> osses = new ArrayList<>(ossesSet);
        List<String> ossesVersioned = new ArrayList<>(ossesVersionedSet);
        List<String> arches = new ArrayList<>(archesSet);
        List<JenkinsViewTemplateBuilder.VersionlessPlatform> versionlessPlatforms= new ArrayList<>(versionlessPlatformsSet);
        Collections.sort(osses);
        Collections.sort(ossesVersioned);
        Collections.sort(arches);
        Collections.sort(versionlessPlatforms);
        List<List<String>> subArches = Arrays.asList(osses, ossesVersioned, arches);
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        jvt.add(JenkinsViewTemplateBuilder.getTaskTemplate("pull", Optional.empty(), Optional.empty(), Optional.of(allPlatforms)));
        for (Task p : allTasks) {
            jvt.add(JenkinsViewTemplateBuilder.getTaskTemplate(p.getId(), Task.getViewColumnsAsOptional(p), Optional.empty(), Optional.of(allPlatforms)));
        }
        for (String p : projects) {
            jvt.add(JenkinsViewTemplateBuilder.getProjectTemplate(p, Optional.empty(), Optional.of(allPlatforms)));
        }
        for (Platform p : allPlatforms) {
            jvt.add(JenkinsViewTemplateBuilder.getPlatformTemplate(p.getId(), allPlatforms));
        }
        for (JenkinsViewTemplateBuilder.VersionlessPlatform p : versionlessPlatforms) {
            jvt.add(JenkinsViewTemplateBuilder.getPlatformTemplate(p));
        }
        for (List<String> subArch : subArches) {
            for (String s : subArch) {
                jvt.add(JenkinsViewTemplateBuilder.getPlatformTemplate(s, allPlatforms));
            }
        }
        for (Platform platform : allPlatforms) {
            for (Task p : allTasks) {
                jvt.add(JenkinsViewTemplateBuilder.getTaskTemplate(p.getId(), Task.getViewColumnsAsOptional(p), Optional.of(platform.getId()), Optional.of(allPlatforms)));
            }
            for (String p : projects) {
                jvt.add(JenkinsViewTemplateBuilder.getProjectTemplate(p, Optional.of(platform.getId()), Optional.of(allPlatforms)));
            }
        }
        for (JenkinsViewTemplateBuilder.VersionlessPlatform platform : versionlessPlatforms) {
            for (Task p : allTasks) {
                jvt.add(JenkinsViewTemplateBuilder.getTaskTemplate(p.getId(), Task.getViewColumnsAsOptional(p), platform));
            }
            for (String p : projects) {
                jvt.add(JenkinsViewTemplateBuilder.getProjectTemplate(p, platform));
            }
        }
        for (List<String> subArch : subArches) {
            for (String s : subArch) {
                for (Task p : allTasks) {
                    jvt.add(JenkinsViewTemplateBuilder.getTaskTemplate(p.getId(), Task.getViewColumnsAsOptional(p), Optional.of(s), Optional.of(allPlatforms)));
                }
                for (String p : projects) {
                    jvt.add(JenkinsViewTemplateBuilder.getProjectTemplate(p, Optional.of(s), Optional.of(allPlatforms)));
                }
            }
        }
        return jvt;
    }
}
