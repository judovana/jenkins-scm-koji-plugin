package org.fakekoji.api.http.rest;

import io.javalin.http.Context;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ViewsAppi {

    private final boolean skipEmpty;
    private final Pattern filter;

    ViewsAppi(Context context) {
        this.filter = Pattern.compile(context.queryParam(OToolService.FILTER) == null ? ".*" : context.queryParam(OToolService.FILTER));
        this.skipEmpty = OToolService.notNullBoolean(context, OToolService.SKIP_EMPTY, false);
    }

    @NotNull
    List<JenkinsViewTemplateBuilder> getJenkinsViewTemplateBuilders(JDKTestProjectManager jdkTestProjectManager, JDKProjectManager jdkProjectManager, PlatformManager platformManager, TaskManager taskManager) throws StorageException, IOException {
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
        List<JenkinsViewTemplateBuilder.VersionlessPlatform> versionlessPlatforms = new ArrayList<>(versionlessPlatformsSet);
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
        return jvt.stream().filter(jvtb -> filter.matcher(jvtb.getName()).matches()).collect(Collectors.toList());
    }

    private String getMatches(List<String> jobs, JenkinsViewTemplateBuilder j) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        Pattern pattern = j.getRegex();
        for (String job : jobs) {
            if (((Pattern) pattern).matcher(job).matches()) {
                viewsAndMatchesToPrint.append("  " + job + "\n");
            }
        }
        return viewsAndMatchesToPrint.toString();
    }

    boolean isSkipEmpty() {
        return skipEmpty;
    }

    public String printMatches(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            String name = j.getName() + "\n";
            String matches = this.getMatches(jobs, j);
            if (this.isSkipEmpty()) {
                if (!matches.isEmpty()) {
                    viewsAndMatchesToPrint.append(name + matches);
                }
            } else {
                viewsAndMatchesToPrint.append(name + matches);
            }
        }
       return viewsAndMatchesToPrint.toString();
    }

    public String getNonEmptyXmls(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs ) {
        StringBuilder xmlsToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            String name = "  ***  " + j.getName() + "  ***  \n";
            String matches = this.getMatches(jobs, j);
            if (!matches.isEmpty()) {
                xmlsToPrint.append(name);
                xmlsToPrint.append(j.expand() + "\n");
            }
        }
        return xmlsToPrint.toString();
    }

    public String getXmls(List<JenkinsViewTemplateBuilder> jvt) {
        StringBuilder xmlsToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            xmlsToPrint.append("  ***  " + j.getName() + "  ***  \n");
            xmlsToPrint.append(j.expand() + "\n");
        }
        return xmlsToPrint.toString();
    }

    public  String getDetails(List<JenkinsViewTemplateBuilder> jvt, List<String> allJenkinsJobs, List<String> jobs) {
        StringBuilder detailsToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            Pattern pattern = j.getRegex();
            int jobCounter = 0;
            for (String job : jobs) {
                if (pattern.matcher(job).matches()) {
                    jobCounter++;
                }
            }
            int jenkinsJobCounter = 0;
            for (String jjob : allJenkinsJobs) {
                if (pattern.matcher(jjob).matches()) {
                    jenkinsJobCounter++;
                }
            }
            if (this.isSkipEmpty()) {
                if (jobCounter > 0 && jenkinsJobCounter >= 0) {
                    detailsToPrint.append(j.getName() + " (" + jobCounter + ") (" + jenkinsJobCounter + ") " + j.getRegex() + "\n");
                }
            } else {
                detailsToPrint.append(j.getName() + " (" + jobCounter + ") (" + jenkinsJobCounter + ") " + j.getRegex() + "\n");
            }
        }
        return detailsToPrint.toString();
    }

    @NotNull
    public String listNonEmpty(List<JenkinsViewTemplateBuilder> jvt, List<String> jobs) {
        StringBuilder viewsAndMatchesToPrint = new StringBuilder();
        for (JenkinsViewTemplateBuilder j : jvt) {
            String name = j.getName() + "\n";
            String matches = this.getMatches(jobs, j);
            if (!matches.isEmpty()) {
                viewsAndMatchesToPrint.append(name);
            }
        }
        return viewsAndMatchesToPrint.toString();
    }

    public String list(List<JenkinsViewTemplateBuilder> jvt) {
        return String.join("\n", jvt) + "\n";
    }

    private interface WorkingFunction{
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
        for (JenkinsViewTemplateBuilder j : jvt) {
            String matches = this.getMatches(jobs, j);
            if (this.isSkipEmpty()) {
                if (!matches.isEmpty()) {
                    actOnHit(operation, viewsAndMatchesToPrint, j);
                }
            } else {
                actOnHit(operation, viewsAndMatchesToPrint, j);
            }
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
