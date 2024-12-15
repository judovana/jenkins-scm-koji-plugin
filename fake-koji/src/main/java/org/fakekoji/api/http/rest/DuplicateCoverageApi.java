package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValue;

public class DuplicateCoverageApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String DUPLICATE = "duplicate";
    public static final String DUPLICATE_TASK = "task";
    public static final String DUPLICATE_SOURCE = "source";
    public static final String DUPLICATE_TARGET = "target";
    public static final String DUPLICATE_COPY_JOBS = "copyJobs";
    //without do, jsut listing
    public static final String DUPLICATE_DO = "do";
    //for scratching only

    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;
    private final TaskManager taskManager;

    DuplicateCoverageApi(final AccessibleSettings settings) {
        this.parser = settings.getJdkProjectParser();
        final ConfigManager configManager = settings.getConfigManager();
        this.jdkProjectManager = configManager.jdkProjectManager;
        this.jdkTestProjectManager = configManager.jdkTestProjectManager;
        this.taskManager = configManager.taskManager;
        this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + DUPLICATE + "/task " + DUPLICATE_SOURCE + " taskNameIn " + DUPLICATE_TARGET + " taskNameOut [" + DUPLICATE_COPY_JOBS + " true]\n"
                + "  Will apply full shared filter, and then select all matching taskNameIn, and create same set of jobs for taskNameOut\n"
                + "  both taskNameIn taskNameOut must beexisting tasks. Note that if you exclude taskNameIn via shared filter, you will get empty output set.\n"
                + "  " + DUPLICATE_COPY_JOBS + " when set to true, will also copy jenkins job (if it exists). Sometimes you use this api to split job, and then it may be worthy to persists history.\n"
                + "  without do=true will just list as usually\n"
                + RedeployApiWorkerBase.THIS_API_IS_USING_SHARED_FILTER;
    }

    private static class TaskBasedInitialLoader extends RedeployApiWorkerBase.RedeployApiListingWorker {

        private final String sourceTask;
        Map<Project, List<Job>> jobsToDuplicate;

        public TaskBasedInitialLoader(Context context, String sourceTask) {
            super(context);
            this.sourceTask = sourceTask;
        }

        public Map<Project, List<Job>> process(final JDKProjectManager jdkProjectManager, final JDKTestProjectManager jdkTestProjectManager, JDKProjectParser parser) throws IOException, StorageException, ManagementException {
            jobsToDuplicate = new HashMap<>();
            iterate(jdkProjectManager, jdkTestProjectManager, parser);
            return jobsToDuplicate;
        }


        @Override
        protected void onPass(Job job, Project project) throws IOException {
            if (job instanceof TestJob) {
                TestJob testJob = (TestJob) job;
                if (testJob.getTask().getId().equals(sourceTask)) {
                    List<Job> r = jobsToDuplicate.get(project);
                    if (r == null) {
                        r = new ArrayList<>();
                        jobsToDuplicate.put(project, r);
                    }
                    r.add(testJob);
                }
            }
        }
    }

    private void duplicateTask(Context context) throws StorageException, IOException, ManagementException {
        String doAndHow = context.queryParam(DUPLICATE_DO);
        String source = context.queryParam(DUPLICATE_SOURCE);
        String target = context.queryParam(DUPLICATE_TARGET);
        boolean copyJobs = Boolean.valueOf(extractParamValue(context.queryParamMap(), DUPLICATE_COPY_JOBS).orElse("false"));
        //exit on missing source/target tasks
        if (source == null || target == null) {
            context.status(500).result(DUPLICATE_SOURCE + " and " + DUPLICATE_TARGET + " are mandatory\n");
            return;
        }
        //exit on non-existing source/target tasks
        if (!taskManager.contains(source)) {
            context.status(500).result(source + " is not existing task\n");
            return;
        }
        if (!taskManager.contains(target)) {
            context.status(500).result(target + " is not existing task\n");
            return;
        }
        Task targetTask = taskManager.read(target);
        TaskBasedInitialLoader tbi = new TaskBasedInitialLoader(context, source);
        //exit if pull and build jobs are enabled
        //that will allow us to work only with TestJob classes
        if (tbi.isBuild()) {
            context.status(500).result(" builds are not allowed\n");
            return;
        }
        if (tbi.isPull()) {
            context.status(500).result(" pulls are not allowed\n");
            return;
        }
        Map<Project, List<Job>> jobsToDuplicate = tbi.process(jdkProjectManager, jdkTestProjectManager, parser);
        if ("true".equals(doAndHow)) {
            Map<Project, Collection<Job>> jobsToGenerate = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (Project project : jobsToDuplicate.keySet()) {
                Set<Job> origJobs = parser.parse(project);
                sb.append(project.getId()).append("\n");
                sb.append(" Original number of jobs: " + origJobs.size()).append("\n");
                Set<Job> futureJobs = new TreeSet<>(new Comparator<Job>() {
                    @Override
                    public int compare(Job j1, Job j2) {
                        return j1.getName().compareTo(j2.getName());
                    }
                });
                futureJobs.addAll(origJobs);
                List<Job> sourceJobs = jobsToDuplicate.get(project);
                Set<Job> maxAddedJobs = new HashSet<>(sourceJobs.size());
                sb.append(" jobs to be added: " + sourceJobs.size()).append("\n");
                for (Job job : sourceJobs) {
                    TestJob testJob = (TestJob) job;
                    TestJob futureJob = TestJob.cloneJobForTask(testJob, targetTask);
                    futureJobs.add(futureJob);
                    maxAddedJobs.add(futureJob);
                }
                sb.append(" jobs at the end: " + futureJobs.size()).append("\n");
                if (futureJobs.size() != origJobs.size() + sourceJobs.size()) {
                    sb.append(" WARNING! overlap happened! " + (futureJobs.size() - (origJobs.size() + sourceJobs.size()))).append("\n");
                }
                final Result<Project, String> result = settings.getReverseJDKProjectParser().parseJobs(futureJobs);
                if (result.isError()) {
                    sb.append(" Error! Failed to construct final project " + result.getError()).append("\n");
                } else {
                    Project updatedProject = result.getValue();
                    jobsToGenerate.put(updatedProject, maxAddedJobs);
                    if (!updatedProject.getId().equals(project.getId())) {
                        context.status(500).result(" different project of " + updatedProject.getId() + " was reconstructed from " + project.getId() + "\n");
                        return;
                    }
                    switch (updatedProject.getType()) {
                        case JDK_PROJECT:
                            settings.getConfigManager().jdkProjectManager.update(updatedProject.getId(), (JDKProject) updatedProject);
                            sb.append(" saved as jdk project").append("\n");
                            break;
                        case JDK_TEST_PROJECT:
                            settings.getConfigManager().jdkTestProjectManager.update(updatedProject.getId(), (JDKTestProject) updatedProject);
                            sb.append(" saved as jdk test project").append("\n");
                            break;
                    }
                }
            }
            JenkinsJobUpdater.wakeUpJenkins();
            for (Project project : jobsToGenerate.keySet()) {
                for (Job job : jobsToGenerate.get(project)) {
                    final JobUpdater jenkinsJobUpdater = settings.getJobUpdater();
                    sb.append(" generating: ").append(job.getName()).append("\n");
                    jenkinsJobUpdater.regenerate(project, job.getName());
                }
            }
            context.status(OToolService.OK).result(sb.toString());
        } else {
            StringBuilder sb = new StringBuilder();
            for (Project project : jobsToDuplicate.keySet()) {
                sb.append(project.getId()).append("\n");
                List<Job> sourceJobs = jobsToDuplicate.get(project);
                for (Job job : sourceJobs) {
                    if (job instanceof TestJob) {
                        TestJob futureJob = TestJob.cloneJobForTask((TestJob) job, targetTask);
                        sb.append(" + ").append(futureJob.getName()).append("\n");
                        if (copyJobs) {
                            if (!new File(settings.getJenkinsJobsRoot(), job.getName()).exists()) {
                                sb.append("   Warning, ").append(futureJob.getName()).append(" do not exists!\n");
                            }
                            if (new File(settings.getJenkinsJobsRoot(), futureJob.getName()).exists()) {
                                sb.append("   Big warning, ").append(futureJob.getName()).append(" DO exists!\n");
                            }
                        }
                    } else {
                        sb.append(" skipped, not test job:  ").append(job.getName()).append("\n");
                    }
                }
            }
            context.status(OToolService.OK).result(sb.toString());
        }
    }

    @Override
    public void addEndpoints() {
        get(DUPLICATE_TASK, context -> {
            duplicateTask(context);
        });
    }
}
