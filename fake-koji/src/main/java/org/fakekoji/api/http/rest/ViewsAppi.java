package org.fakekoji.api.http.rest;

import io.javalin.http.Context;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.views.JenkinsViewTemplateBuilder;
import org.fakekoji.jobmanager.views.JenkinsViewTemplateBuilderFactory;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.jobmanager.views.VariantsMultiplier;
import org.fakekoji.jobmanager.views.VersionlessPlatform;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ViewsAppi {

    private final boolean skipEmpty;
    private final Pattern filter;
    private final boolean nested;
    private final JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle nestedColumnsStyle;
    private final JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle listColumnsStyle;
    private final  JenkinsViewTemplateBuilderFactory jvtbFactory;

    ViewsAppi(Context context) {
        this.filter = Pattern.compile(context.queryParam(OToolService.FILTER) == null ? ".*" : context.queryParam(OToolService.FILTER));
        this.skipEmpty = OToolService.notNullBoolean(context, OToolService.SKIP_EMPTY, false);
        this.nested = OToolService.notNullBoolean(context, OToolService.NESTED, false);
        String nestedColumnsValue = context.queryParam(OToolService.NESTED_COLUMNS);
        String listColumnsValue = context.queryParam(OToolService.CUSTOM_COLUMNS);
        nestedColumnsStyle = setColumnsStyle(nestedColumnsValue);
        listColumnsStyle = setColumnsStyle(listColumnsValue);
        jvtbFactory = new JenkinsViewTemplateBuilderFactory(nestedColumnsStyle, listColumnsStyle);
    }

    @NotNull
    private JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle setColumnsStyle(String nestedColumnsValue) {
        if (nestedColumnsValue == null || nestedColumnsValue.trim().isEmpty() || nestedColumnsValue.equals("false")) {
            return new JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle(JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.NONE, 0);
        } else if (nestedColumnsValue.equals("true")) {
            return new JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle(JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.ALL, 0);
        } else {
            return new JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle(JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.AUTO, Integer.parseInt(nestedColumnsValue));
        }
    }

    @NotNull
    List<JenkinsViewTemplateBuilder> getJenkinsViewTemplateBuilders(JDKTestProjectManager jdkTestProjectManager, JDKProjectManager jdkProjectManager, PlatformManager platformManager, TaskManager taskManager, TaskVariantManager variantManager,  JDKVersionManager jdkVersionManager, Optional<List<String>> filterOutViewsAsap) throws StorageException, IOException {
        nestedColumnsStyle.setJobs(filterOutViewsAsap);
        listColumnsStyle.setJobs(filterOutViewsAsap);
        List<TaskVariant> taskVariants = variantManager.readAll();
        List<JDKTestProject> jdkTestProjecs = jdkTestProjectManager.readAll();
        List<JDKVersion> jdkVersions = jdkVersionManager.readAll();
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
        Set<VersionlessPlatform> versionlessPlatformsSet = new HashSet<>();
        for (Platform p : allPlatforms) {
            ossesSet.add(p.getOs());
            ossesVersionedSet.add(p.getOs() + p.getVersion());
            archesSet.add(p.getArchitecture());
            versionlessPlatformsSet.add(new VersionlessPlatform(p.getOs(), p.getArchitecture()));
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
        List<VersionlessPlatform> versionlessPlatforms = new ArrayList<>(versionlessPlatformsSet);
        Collections.sort(osses);
        Collections.sort(ossesVersioned);
        Collections.sort(arches);
        Collections.sort(versionlessPlatforms);
        List<List<String>> subArches = Arrays.asList(osses, ossesVersioned, arches);
        List<JenkinsViewTemplateBuilder> jvt;
        if (nested) {
            jvt = getNestedViews(taskVariants, allPlatforms, allTasks, projects, versionlessPlatforms, jdkVersions, osses, ossesVersioned, arches, isSkipEmpty()?filterOutViewsAsap:Optional.empty());
        } else {
            jvt = getDirectViews(taskVariants, allPlatforms, allTasks, projects, versionlessPlatforms, subArches, jdkVersions);
        }
        return jvt.stream().filter(jvtb -> filter.matcher(jvtb.getName()).matches()).collect(Collectors.toList());
    }


    private List<JenkinsViewTemplateBuilder> getNestedViews(List<TaskVariant> taskVariants, List<Platform> allPlatforms, List<Task> allTasks, List<String> projects, List<VersionlessPlatform> versionlessPlatforms, List<JDKVersion> jdkVersions, List<String> osses, List<String> ossesVersioned, List<String> arches, Optional<List<String>> filterOutViewsAsap) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (String tab : new String[]{"projects", "tasks", "platforms", "jdkVersions", "variants"}) {
            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder projectFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(tab);
            jvt.add(projectFolder);
            if (tab.equals("projects")) {
                projectFolder.addAll(addAllProjects(allPlatforms, projects, Optional.empty()));
                for (String os : osses) {
                    projectFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(os)));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(os);
                    projectFolder.addView(osFolder);
                    for (String osVersioned : ossesVersioned) {
                        if (osVersioned.startsWith(os)) {
                            osFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(osVersioned)));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osVersionedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(osVersioned);
                            osFolder.addView(osVersionedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getOsVersion().equals(osVersioned)) {
                                    osVersionedFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getOs().equals(os)) {
                            osFolder.addAll(addAllProjects(projects, vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            osFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().equals(os)) {
                                    osArchedFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                }
                for (String arch : arches) {
                    projectFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(arch)));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder archFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(arch);
                    projectFolder.addView(archFolder);
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getArch().equals(arch)) {
                            archFolder.addAll(addAllProjects(projects, vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            archFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().startsWith(vp.getOs())) {
                                    osArchedFolder.addAll(addAllProjects(allPlatforms, projects, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                }
            }
            if (tab.equals("tasks")) {
                List<String> jps = jdkVersions.stream().map( q -> q.getId()).collect(Collectors.toList());
                jps.add(0,"all");
                for (String jp: jps ) {
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder taskJpFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(jp);
                    projectFolder.addView(taskJpFolder);
                    if (jp.equals("all")){
                        jp = "";
                    } else {
                        jp = Job.DELIMITER+jp;
                    }
                    taskJpFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.empty(), jp));
                    for (String os : osses) {
                        taskJpFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(os), jp));
                        JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(os);
                        taskJpFolder.addView(osFolder);
                        for (String osVersioned : ossesVersioned) {
                            if (osVersioned.startsWith(os)) {
                                osFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(osVersioned), jp));
                                JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osVersionedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(osVersioned);
                                osFolder.addView(osVersionedFolder);
                                for (Platform fullPlatform : allPlatforms) {
                                    if (fullPlatform.getOsVersion().equals(osVersioned)) {
                                        osVersionedFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(fullPlatform.getId()), jp));
                                    }
                                }
                            }
                        }
                        for (VersionlessPlatform vp : versionlessPlatforms) {
                            if (vp.getOs().equals(os)) {
                                osFolder.addAll(getAllTasks(allTasks, vp, jp));
                                JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                                osFolder.addView(osArchedFolder);
                                for (Platform fullPlatform : allPlatforms) {
                                    if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().equals(os)) {
                                        osArchedFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(fullPlatform.getId()), jp));
                                    }
                                }
                            }
                        }
                    }
                    for (String arch : arches) {
                        taskJpFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(arch), jp));
                        JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder archFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(arch);
                        taskJpFolder.addView(archFolder);
                        for (VersionlessPlatform vp : versionlessPlatforms) {
                            if (vp.getArch().equals(arch)) {
                                archFolder.addAll(getAllTasks(allTasks, vp, jp));
                                JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                                archFolder.addView(osArchedFolder);
                                for (Platform fullPlatform : allPlatforms) {
                                    if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().startsWith(vp.getOs())) {
                                        osArchedFolder.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(fullPlatform.getId()), jp));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (tab.equals("platforms")) {
                for (String os : osses) {
                    projectFolder.addView(jvtbFactory.getPlatformTemplate(os, allPlatforms));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(os);
                    projectFolder.addView(osFolder);
                    for (String osVersioned : ossesVersioned) {
                        if (osVersioned.startsWith(os)) {
                            osFolder.addView(jvtbFactory.getPlatformTemplate(osVersioned, allPlatforms));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osVersionedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(osVersioned);
                            osFolder.addView(osVersionedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getOsVersion().equals(osVersioned)) {
                                    osVersionedFolder.addView(jvtbFactory.getPlatformTemplate(fullPlatform.getId(), allPlatforms));
                                }
                            }
                        }
                    }
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getOs().equals(os)) {
                            osFolder.addView(jvtbFactory.getPlatformTemplate(vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            osFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().equals(os)) {
                                    osArchedFolder.addView(jvtbFactory.getPlatformTemplate(fullPlatform.getId(), allPlatforms));
                                }
                            }
                        }
                    }
                }
                for (String arch : arches) {
                    projectFolder.addView(jvtbFactory.getPlatformTemplate(arch, allPlatforms));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder archFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(arch);
                    projectFolder.addView(archFolder);
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getArch().equals(arch)) {
                            archFolder.addView(jvtbFactory.getPlatformTemplate(vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            archFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().startsWith(vp.getOs())) {
                                    osArchedFolder.addView(jvtbFactory.getPlatformTemplate(fullPlatform.getId(), allPlatforms));
                                }
                            }
                        }
                    }
                }
            }
            if (tab.equals("variants")) {
                if (filterOutViewsAsap.isPresent()) {
                    List<String> combinations = combine(VariantsMultiplier.splitAndExpand(taskVariants));
                    VariantsMultiplier vm = new VariantsMultiplier(jvtbFactory);
                    projectFolder.addAll(vm.getAllCombinedVariantsAsTree(vm.combinedVariantsToTree(combinations, taskVariants, filterOutViewsAsap)));
                } else {
                    jvt.addAll(getAllVariants(taskVariants));
                }
            }
            if (tab.equals("jdkVersions")) {
                projectFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.empty()));
                for (String os : osses) {
                    projectFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(os)));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(os);
                    projectFolder.addView(osFolder);
                    for (String osVersioned : ossesVersioned) {
                        if (osVersioned.startsWith(os)) {
                            osFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(osVersioned)));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osVersionedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(osVersioned);
                            osFolder.addView(osVersionedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getOsVersion().equals(osVersioned)) {
                                    osVersionedFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getOs().equals(os)) {
                            osFolder.addAll(getAllJdkVersions(jdkVersions, vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            osFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().equals(os)) {
                                    osArchedFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                }
                for (String arch : arches) {
                    projectFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(arch)));
                    JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder archFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(arch);
                    projectFolder.addView(archFolder);
                    for (VersionlessPlatform vp : versionlessPlatforms) {
                        if (vp.getArch().equals(arch)) {
                            archFolder.addAll(getAllJdkVersions(jdkVersions, vp));
                            JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder osArchedFolder = jvtbFactory.getJenkinsViewTemplateBuilderFolder(vp.getId());
                            archFolder.addView(osArchedFolder);
                            for (Platform fullPlatform : allPlatforms) {
                                if (fullPlatform.getArchitecture().equals(vp.getArch()) && fullPlatform.getOs().startsWith(vp.getOs())) {
                                    osArchedFolder.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.of(fullPlatform.getId())));
                                }
                            }
                        }
                    }
                }
            }
        }
        jvt.add(createPullView(allPlatforms));
        return jvt;
    }

    private List<String> combine(List<List<List<String>>> lists) {
        if (lists.size() == 0) {
            return new ArrayList<>(0);
        } else if (lists.size() == 1) {
            return combineAndAppend(lists.get(0));
        } else {
            List<String> r = new ArrayList<>();
            for (String l : combineAndAppend(lists.get(0))) {
                List<String> ss = combine(lists.subList(1, lists.size()));
                for (String rr : ss) {
                    r.add(l + "|" + rr);
                }
            }
            return r;
        }
    }

    private List<String> combineAndAppend(List<List<String>> lists) {
        if (lists.size() == 0) {
            return new ArrayList<>(0);
        } else if (lists.size() == 1) {
            return lists.get(0);
        } else {
            List<String> r = new ArrayList<>();
            for (String l : lists.get(0)) {
                List<String> ss = combineAndAppend(lists.subList(1, lists.size()));
                for (String rr : ss) {
                    r.add(l + "." + rr);
                }
            }
            return r;
        }
    }



    @NotNull
    private JenkinsViewTemplateBuilder createPullView(List<Platform> allPlatforms) throws IOException {
        return jvtbFactory.getTaskTemplate("pull", Optional.empty(), Optional.empty(), Optional.of(allPlatforms));
    }

    private List<JenkinsViewTemplateBuilder> getAllJdkVersions(List<Platform> allPlatforms, List<JDKVersion> jdkVersions, Optional<String> platform) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (JDKVersion jp : jdkVersions) {
            jvt.add(jvtbFactory.getJavaPlatformTemplate(jp, platform, Optional.of(allPlatforms)));
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> getAllJdkVersions(List<JDKVersion> jdkVersions, VersionlessPlatform vp) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (JDKVersion jp : jdkVersions) {
            jvt.add(jvtbFactory.getJavaPlatformTemplate(jp, vp));
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> getDirectViews(List<TaskVariant> taskVariants, List<Platform> allPlatforms, List<Task> allTasks, List<String> projects,
                                                            List<VersionlessPlatform> versionlessPlatforms, List<List<String>> subArches, List<JDKVersion> jdkVersions) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        jvt.add(createPullView(allPlatforms));
        jvt.addAll(getAllTasks(allPlatforms, allTasks, Optional.empty(),""));
        jvt.addAll(getAllJdkVersions(allPlatforms, jdkVersions, Optional.empty()));
        jvt.addAll(addAllProjects(allPlatforms, projects, Optional.empty()));
        for (Platform p : allPlatforms) {
            jvt.add(jvtbFactory.getPlatformTemplate(p.getId(), allPlatforms));
        }
        for (VersionlessPlatform p : versionlessPlatforms) {
            jvt.add(jvtbFactory.getPlatformTemplate(p));
        }
        for (List<String> subArch : subArches) {
            for (String s : subArch) {
                jvt.add(jvtbFactory.getPlatformTemplate(s, allPlatforms));
            }
        }
        for (Platform platform : allPlatforms) {
            jvt.addAll(getAllTasks(allPlatforms, allTasks,  Optional.of(platform.getId()),""));
            jvt.addAll(addAllProjects(allPlatforms, projects, Optional.of(platform.getId())));
        }
        for (VersionlessPlatform platform : versionlessPlatforms) {
            jvt.addAll(getAllTasks(allTasks, platform, ""));
            jvt.addAll(addAllProjects(projects, platform));
        }
        for (List<String> subArch : subArches) {
            for (String s : subArch) {
                jvt.addAll(getAllTasks(allPlatforms, allTasks, Optional.of(s),""));
                jvt.addAll(addAllProjects(allPlatforms, projects, Optional.of(s)));
            }
        }
        jvt.addAll(getAllVariants(taskVariants));
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> getAllVariants(List<TaskVariant> taskVariants) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (TaskVariant taskVariant : taskVariants) {
            for (TaskVariantValue taskVariantValue : taskVariant.getVariants().values()) {
                jvt.add(jvtbFactory.getVariantTempalte(taskVariantValue.getId()));
            }
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> getAllTasks(List<Task> allTasks, VersionlessPlatform platform, String jp) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (Task p : allTasks) {
            jvt.add(jvtbFactory.getTaskTemplate(p.getId()+jp, Task.getViewColumnsAsOptional(p), platform));
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> getAllTasks(List<Platform> allPlatforms, List<Task> allTasks, Optional<String> platform, String jp) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (Task p : allTasks) {
            jvt.add(jvtbFactory.getTaskTemplate(p.getId()+jp, Task.getViewColumnsAsOptional(p), platform, Optional.of(allPlatforms)));
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> addAllProjects(List<String> projects, VersionlessPlatform platform) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (String p : projects) {
            jvt.add(jvtbFactory.getProjectTemplate(p, platform));
        }
        return jvt;
    }

    private List<JenkinsViewTemplateBuilder> addAllProjects(List<Platform> allPlatforms, List<String> projects, Optional<String> platform) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (String p : projects) {
            jvt.add(jvtbFactory.getProjectTemplate(p, platform, Optional.of(allPlatforms)));
        }
        return jvt;
    }

    boolean isSkipEmpty() {
        return skipEmpty;
    }

    public String printMatches(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        jvt = filterEmptyIfSelected(jvt, Optional.of(jobs));
        return list(jvt, Optional.of(jobs), true, false);
    }

    private List<JenkinsViewTemplateBuilder> filterEmptyIfSelected(List<JenkinsViewTemplateBuilder> jvt, Optional<List<String>> jobs) {
        if (this.isSkipEmpty()) {
            return jvt.stream().filter(j -> !j.clearOutMatches(jobs)).collect(Collectors.toList());
        }
        return jvt;
    }


    public String getNonEmptyXmls(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) throws IOException {
        StringBuilder xmlsToPrint = new StringBuilder();
        jvt = filterEmptyIfSelected(jvt, Optional.of(jobs));
        for (JenkinsViewTemplateBuilder j : jvt) {
            String name = "  ***  " + j.getName() + "  ***  \n";
            xmlsToPrint.append(name);
            xmlsToPrint.append(j.expand() + "\n");
        }
        return xmlsToPrint.toString();
    }

    public String getXmls(List<JenkinsViewTemplateBuilder> jvt) throws IOException {
        StringBuilder xmlsToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            xmlsToPrint.append("  ***  " + j.getName() + "  ***  \n");
            xmlsToPrint.append(j.expand() + "\n");
        }
        return xmlsToPrint.toString();
    }

    public String getDetails(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        jvt = filterEmptyIfSelected(jvt, Optional.of(jobs));
        return list(jvt, Optional.of(jobs), false, true);
    }

    @NotNull
    public String listNonEmpty(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        jvt = filterEmptyIfSelected(jvt, Optional.of(jobs));
        return list(jvt, Optional.of(jobs), false, false);
    }

    public String list(List<JenkinsViewTemplateBuilder> jvt, Optional<List<String>> jobs, boolean matches, boolean details) {
        return jvt.stream().map(j -> j.toExtendedPrint(jobs, details, matches)).collect(Collectors.joining());
    }

    private interface WorkingFunction {
        JenkinsCliWrapper.ClientResponse work(JenkinsViewTemplateBuilder j);

        String getOp();
    }

    private class CreateFunction implements WorkingFunction {

        @Override
        public JenkinsCliWrapper.ClientResponse work(JenkinsViewTemplateBuilder j) {
            return JenkinsCliWrapper.getCli().createView(j);
        }

        @Override
        public String getOp(){
            return "creating";
        }
    }
    private class RemoveFunction implements WorkingFunction {

        @Override
        public JenkinsCliWrapper.ClientResponse work(JenkinsViewTemplateBuilder j) {
            return JenkinsCliWrapper.getCli().deleteView(j);
        }

        @Override
        public String getOp(){
            return "remving";
        }
    }

    private class UpdateFunction implements WorkingFunction {

        @Override
        public JenkinsCliWrapper.ClientResponse work(JenkinsViewTemplateBuilder j) {
            return JenkinsCliWrapper.getCli().updateView(j);
        }

        @Override
        public String getOp(){
            return "updating";
        }
    }

    private String jenkinsWork(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs, WorkingFunction operation) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        jvt = filterEmptyIfSelected(jvt, Optional.of(jobs));
        for (JenkinsViewTemplateBuilder j : jvt) {
                    actOnHit(operation, viewsAndMatchesToPrint, j);
        }
        return viewsAndMatchesToPrint.toString();
    }

    private void actOnHit(WorkingFunction operation, StringBuilder viewsAndMatchesToPrint, JenkinsViewTemplateBuilder j) {
        JenkinsCliWrapper.ClientResponse result = operation.work(j);
        viewsAndMatchesToPrint.append(operation.getOp() + ": " + j.getName() + " - " + result.simpleVerdict() + " (" + result.toString() + ")\n");
    }

    public String create(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        return jenkinsWork(jvt, jobs, new CreateFunction());
    }

    public String delete(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        return jenkinsWork(jvt, jobs, new RemoveFunction());
    }

    public String update(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        return jenkinsWork(jvt, jobs, new UpdateFunction());
    }
}
