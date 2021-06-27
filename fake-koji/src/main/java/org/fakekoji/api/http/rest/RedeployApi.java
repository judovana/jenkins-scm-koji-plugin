package org.fakekoji.api.http.rest;

import hudson.plugins.scm.koji.Constants;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.fakekoji.Utils;
import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.FakeBuild;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;

public class RedeployApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String REDEPLOY = "re";
    private static final String REPROVIDER = "provider";
    private static final String RESLAVES = "slaves";
    //without list/do just list list waht can be done?
    //eg list of nvras in processed.txt for test
    //eg list new api content of fake koji for build?
    //already filtered via other filters?
    private static final String RELOAD = "load"; //requires jobid, and is there simply to make relaod of job easier
    private static final String REDEPLOY_TEST = "test"; //test have sense with all the swithces before, build do not. build needs only 1/2 nvra
    private static final String REDEPLOY_BUILD = "build"; //only jdkProject in addtion to removal VR from processed.txt, it cleans  FAILED, ERROR and smaller then 4bytes files from local-builds
    private static final String REDEPLOY_RUN = "run"; //job=jobName&build=id resore archve/changelog.xml as build.xml and execute build now
    private static final String REDEPLOY_CHECKOUT = "checkout"; //job=jobName removes build.xml and execute build now
    private static final String REDEPLOY_CHECKOUT_ALL = "checkoutall"; //no job name, but filtering, removes build.xml and execute build now, requires do
    private static final String REDEPLOY_NOW = "now"; //still same filtering, simply pressing "build now" on selected jobs, requires do
    private static final String REDEPLOY_DO = "do"; //will do the real work if true, by default it will only print what it will affect
    //with?
    private static final String REDEPLOY_NVR = "nvr"; //and enforce platform as separate thing?

    private static final String ARCHES_EXPECTED = "archesExpected";
    private static final String ARCHES_EXPECTED_SET = "set";

    private static final String LATEST = "latest";
    private static final String LATEST_count = "count";

    private static final String PROCESSED = "processed";
    private static final String PROCESSED_job = "job";
    private static final String PROCESSED_cmments = "comments";
    private static final String PROCESSED_uniq = "nouniq";
    private static final String PROCESSED_sort = "sort";
    private static final String RERUN_JOB = "job";
    private static final String RERUN_BUILD = "build";


    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;
    private final PlatformManager platformManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;

    RedeployApi(final AccessibleSettings settings) {
        this.parser = settings.getJdkProjectParser();
        final ConfigManager configManager = settings.getConfigManager();
        this.jdkProjectManager = configManager.jdkProjectManager;
        this.jdkTestProjectManager = configManager.jdkTestProjectManager;
        this.platformManager = configManager.platformManager;
        this.jdkVersionManager = configManager.jdkVersionManager;
        this.taskVariantManager = configManager.taskVariantManager;
        this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + REDEPLOY + "/" + REPROVIDER + "\n"
                + "  requires the shred filterig below. Will temporarily (until next regeneration) change run provider of selected jobs (including slaves, excluding job name).\n"
                + "  Except filter, the mandatory parameter is provider=<provider-id>.\n"
                + MISC + '/' + REDEPLOY + "/" + RESLAVES + "\n"
                + "  requires the shred filterig below. Will temporarily (until next regeneration) change slaves/labesl of selected jobs .\n"
                + "  Except filter, the mandatory parameter is salves=<jenkins slaves stringd>. This method hdd no constraints! Use with care!\n"
                + MISC + '/' + REDEPLOY + "/" + RELOAD + "\n"
                + "  requires job=name. Will simply reload this job from disk.\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_RUN + "\n"
                + "  requires job=name&build=number; will rereun given job. It is invoking build now, with fake checkout. Job should not be in queue.\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_CHECKOUT + "\n"
                + "  requires job=name  will execute checkout and build (unlike build now, which rebuilds last nvr). Job should not be in queue\n"
                + "  if there is nothing to checkout (eg due allbuilds in processed.txt, build will fail\n"
                + "  optional parametr nvr=  to remove NVR from prcocessed.txt before removing the build.xml, unelss you set force=true, the record must be in processed.txt\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_CHECKOUT_ALL + "\n"
                + "  Will force " + REDEPLOY_CHECKOUT + " on all jobs, based by filter (See below).\n"
                + "  optional parametr nvr=  to remove NVR from prcocessed.txt before removing the build.xml, unelss you set force=true, the record must be in processed.txt\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_NOW + "\n"
                + "  Will will press `build now` on all jobs, based by filter (See below).\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_BUILD + "\n"
                + "  Will print out all nvrs in processed.txt of builds.\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_TEST + "\n"
                + "  Will print out all nvrs in processed.txt of tests.\n"
                + MISC + '/' + REDEPLOY + "/" + LATEST + "\n"
                + "  Will print out latest value(s) in  processed.txt of based on filter (se below). `do` have no effect.\n"
                + "  Use " + LATEST_count + "=number to get ore then one latest. " + PROCESSED_cmments + " applicable and " + PROCESSED_job + "=true can make sense\n"
                + "Shared by most:\n"
                + "  once you set " + REDEPLOY_NVR + "=nvr the jobs affected by this nvr will be printed.\n"
                + RedeployApiWorkerBase.getHelp()
                + "  once you set " + REDEPLOY_DO + "=true, the real work will happen - nvr will be removed from affected jobs.\n"
                + "  For " + REDEPLOY_BUILD + " it also removes the affected NVRA from database. A is deducted  from other params\n"
                + "\n"
                + MISC + '/' + REDEPLOY + '/' + ARCHES_EXPECTED + " will list exisiting " + FakeBuild.archesConfigFileName + " files"
                + "  with " + REDEPLOY_NVR + "  it shows arches expectewd of this NVR (dont forget, that in old api R contan also OS!\n"
                + "  with " + ARCHES_EXPECTED_SET + " and " + REDEPLOY_NVR + "  set arches  for  this NVR\n"
                + "    To set more global arches, you must go via file system, as we have new api, dont do that\n"
                + "\n"
                + MISC + '/' + REDEPLOY + '/' + PROCESSED + " will show content of " + Constants.PROCESSED_BUILDS_HISTORY + " of given JOB"
                + "  mandatory " + PROCESSED_job + "=jobName optional [" + String.join(",", Arrays.asList(PROCESSED_cmments, PROCESSED_sort, PROCESSED_uniq)) + "]\n";


    }


    @Override
    public void addEndpoints() {
        get(LATEST, context -> {
            String countValue = context.queryParam(LATEST_count);
            String commentsValue = context.queryParam(PROCESSED_cmments);
            String jobsNamesValue = context.queryParam(PROCESSED_job);
            List<String> jobs = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
            Set<String> nvrs = new HashSet<>(jobs.size());
            for (String job : jobs) {
                File processed = new File(new File(settings.getJenkinsJobsRoot(), job), Constants.PROCESSED_BUILDS_HISTORY);
                List<String> raw = new ArrayList<>(0);
                if (processed.exists()) {
                    raw = Utils.readFileToLines(processed, null);
                }
                if (raw.size() > 0) {
                    nvrs.add(raw.get(raw.size() - 1).replaceAll("\\s*#.*", ""));
                }
            }
            context.status(OToolService.OK).result(String.join("\n", nvrs) + "\n");
        });
        get(PROCESSED, context -> {
            String job = context.queryParam(PROCESSED_job);
            if (job != null) {
                try {
                    File processed = new File(new File(settings.getJenkinsJobsRoot(), job), Constants.PROCESSED_BUILDS_HISTORY);
                    List<String> raw = Utils.readFileToLines(processed, null);
                    if (context.queryParam(PROCESSED_cmments) == null) {
                        for (int i = 0; i < raw.size(); i++) {
                            raw.set(i, raw.get(i).replaceAll("\\s*#.*", ""));
                        }
                    }
                    if (context.queryParam(PROCESSED_uniq) == null) {
                        raw = new ArrayList<>(new TreeSet<>(raw));
                    }
                    if (context.queryParam(PROCESSED_sort) != null) {
                        Collections.sort(raw);
                        //Collections.reverse(raw);no!
                    }
                    context.status(OToolService.OK).result(String.join("\n", raw) + "\n");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    context.status(400).result(e.getMessage());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    context.status(500).result(e.getMessage());
                }
            } else {
                context.status(OToolService.BAD).result("job is mandatory\n");
            }
        });
        get(REDEPLOY_CHECKOUT, context -> {
            try {
                String job = context.queryParam(RERUN_JOB);
                String nvr = context.queryParam(REDEPLOY_NVR);
                String force = context.queryParam("force");
                StringBuilder sb = forceCheckoutOnJob(job, nvr, "true".equals(force));
                context.status(OToolService.OK).result(sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });
        get(RELOAD, context -> {
            try {
                String job = context.queryParam(RERUN_JOB);
                if (job == null) {
                    throw new RuntimeException(RERUN_JOB + " is required parameter for " + RELOAD);
                }
                JenkinsCliWrapper.ClientResponse cr = JenkinsCliWrapper.getCli().reloadJob(job);
                try {
                    cr.throwIfNecessary();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), cr.toString());
                    context.status(500).result(ex.getMessage() + ": " + cr.toString());
                }
                context.status(OToolService.OK).result(cr.toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });
        get(REPROVIDER, context -> {
            try {
                String OTOOL_PLATFORM_PROVIDER = JenkinsJobTemplateBuilder.OTOOL_BASH_VAR_PREFIX + JenkinsJobTemplateBuilder.PLATFORM_PROVIDER_VAR;
                List<String> jobs = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
                String doAndHow = context.queryParam(REDEPLOY_DO);
                String nwProvider = context.queryParam(REPROVIDER);
                if (nwProvider == null || nwProvider.trim().isEmpty()){
                    throw new RuntimeException(REPROVIDER+" is mandatory\n");
                }
                Set<String> providers = GetterAPI.getProviders(settings.getConfigManager().platformManager);
                if (!providers.contains(nwProvider)) {
                    throw new RuntimeException(nwProvider + ": is not valid provider. Use one of: " + String.join(",", providers) + "\n");
                }
                StringBuilder sb = new StringBuilder();
                if ("true".equals(doAndHow)) {

                } else {
                    int totalIssues=0;
                    for (String job : jobs) {
                        sb.append(job).append("\n");
                        File jobDir = new File(settings.getJenkinsJobsRoot(), job);
                        File config = new File(jobDir, "config.xml");
                        List<String> lines = Utils.readFileToLines(config, null);
                        int nodesCount = 0;
                        int providersCount = 0;
                        for (String mainline : lines) {
                            String[] xmlLines = mainline.split("&#13;");
                            for (String line: xmlLines) {
                                if (line.contains("<assignedNode>")) {
                                    sb.append(" - ").append(line.trim()).append("\n");
                                    nodesCount++;
                                }
                                if (line.contains(OTOOL_PLATFORM_PROVIDER + "=")) {
                                    sb.append(" - ").append(line.trim()).append("\n");
                                    providersCount++;
                                }
                            }
                        }
                        if (nodesCount != 1){
                            totalIssues++;
                            sb.append(" - ").append("!WARNING! found none or more then one assignedNode!").append("\n");
                        }
                        if (providersCount != 1){
                            totalIssues++;
                            sb.append(" - ").append("!WARNING! found none or more then one " + OTOOL_PLATFORM_PROVIDER + "!").append("\n");
                        }
                    }
                    if (totalIssues > 0){
                        sb.append("Warning! " + totalIssues + " issue found! Do not proceed!\n");
                    } else {
                        sb.append("no issues found, but be careful anyway!\n");
                    }
                }
                context.status(OToolService.OK).result(sb.toString() + "\n");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(RESLAVES, context -> {
            try {
                List<String> jobs = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
                String doAndHow = context.queryParam(REDEPLOY_DO);
                String nwSlaves = context.queryParam(RESLAVES);
                if (nwSlaves == null || nwSlaves.trim().isEmpty()){
                    throw new RuntimeException(RESLAVES+" is mandatory\n");
                }
                StringBuilder sb = new StringBuilder();
                if ("true".equals(doAndHow)) {
                    int totalCountReplacements = 0 ;
                    int totalCountFiles = 0 ;
                    for (String job : jobs) {
                        sb.append(job).append("\n");
                        File jobDir = new File(settings.getJenkinsJobsRoot(), job);
                        File config = new File(jobDir, "config.xml");
                        List<String> lines = Utils.readFileToLines(config, null);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.contains("<assignedNode>")) {
                                String orig=line.trim();
                                String nw="<assignedNode>"+nwSlaves+"</assignedNode>";
                                sb.append(" - "+orig+" -> "+nw+ "\n");
                                lines.set(i,nw);
                                totalCountReplacements++;
                            }
                        }
                        Utils.writeToFile(config, String.join("\n", lines));
                        sb.append(" - written");
                        totalCountFiles++;
                    }
                    sb.append("Written "+totalCountFiles+" of "+jobs.size()+"\n");
                    sb.append("Replaced "+totalCountReplacements+" of "+jobs.size()+"\n");
                } else {
                    int totalIssues = 0;
                    for (String job : jobs) {
                        sb.append(job).append("\n");
                        File jobDir = new File(settings.getJenkinsJobsRoot(), job);
                        File config = new File(jobDir, "config.xml");
                        List<String> lines = Utils.readFileToLines(config, null);
                        int nodesCount = 0;
                        for (String line : lines) {
                                if (line.contains("<assignedNode>")) {
                                    sb.append(" - ").append(line.trim()).append("\n");
                                    nodesCount++;
                                }
                        }
                        if (nodesCount != 1){
                            totalIssues ++ ;
                            sb.append(" - ").append("!WARNING! found none or more then one assignedNode!").append("\n");
                        }
                    }
                    if (totalIssues > 0){
                        sb.append("Warning! " + totalIssues + " issue found! Do not proceed!\n");
                    } else {
                        sb.append("no issues found, but be careful anyway!\n");
                    }
                }
                context.status(OToolService.OK).result(sb.toString() + "\n");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(REDEPLOY_CHECKOUT_ALL, context -> {
            try {
                List<String> jobs = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
                String doAndHow = context.queryParam(REDEPLOY_DO);
                String nvr = context.queryParam(REDEPLOY_NVR);
                String force = context.queryParam("force");
                if ("true".equals(doAndHow)) {
                    List<String> results = new ArrayList<>(jobs.size());
                    for (String job : jobs) {
                        try {
                            forceCheckoutOnJob(job, nvr, "true".equals(force));
                            results.add("ok - checking out -  " + job);
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                            results.add("failed " + job + ex.toString());
                        }
                    }
                    context.status(OToolService.OK).result(String.join("\n", results) + "\n");
                } else {
                    context.status(OToolService.OK).result(String.join("\n", jobs) + "\n");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });
        get(REDEPLOY_NOW, context -> {
            try {
                List<String> jobs = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
                String doAndHow = context.queryParam(REDEPLOY_DO);
                if ("true".equals(doAndHow)) {
                    List<String> results = new ArrayList<>(jobs.size());
                    for (String job : jobs) {
                        try {
                            JenkinsCliWrapper.ClientResponse cr = JenkinsCliWrapper.getCli().scheduleBuild(job);
                            cr.throwIfNecessary();
                            results.add("ok - scheduling -  " + job);
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                            results.add("failed " + job + ex.toString());
                        }
                    }
                    context.status(OToolService.OK).result(String.join("\n", results) + "\n");
                } else {
                    context.status(OToolService.OK).result(String.join("\n", jobs) + "\n");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });
        get(REDEPLOY_RUN, context -> {
            //hmm enable filtering?
            try {
                String job = context.queryParam(RERUN_JOB);
                if (job == null) {
                    throw new RuntimeException(RERUN_JOB + " must be an existing job id, was " + job);
                }
                String id = context.queryParam(RERUN_BUILD);
                if (id == null) {
                    throw new RuntimeException(RERUN_BUILD + " must be an existing build number, was " + id);
                }
                File currentt = new File(settings.getJenkinsJobsRoot().getAbsolutePath() + File.separator + job + File.separator + "build.xml");
                if (!currentt.getParentFile().exists()) {
                    throw new RuntimeException(currentt.getParentFile() + " do not exists. bad job?");
                }
                File archived = new File(settings.getJenkinsJobsRoot().getAbsolutePath() + File.separator + job + File.separator + "builds" + File.separator + id + File.separator + "changelog.xml");
                if (!archived.exists()) {
                    throw new RuntimeException(archived.getAbsolutePath() + " do not exists. failed at checkout? Bad id? bad job?");
                }
                StringBuilder sb = new StringBuilder();
                if (!currentt.exists()) {
                    sb.append(currentt.getAbsolutePath() + " do not exists. bad job? Broken checkou?t No koji-scm job?\n");
                }
                Files.copy(archived.toPath(), currentt.toPath(), StandardCopyOption.REPLACE_EXISTING);
                sb.append("Copied " + archived.getAbsolutePath() + " as " + currentt.getAbsolutePath() + "\n");
                JenkinsCliWrapper.ClientResponse cr = JenkinsCliWrapper.getCli().scheduleBuild(job);
                cr.throwIfNecessary();
                sb.append("scheduled " + job + "\n");
                context.status(OToolService.OK).result(sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(REDEPLOY_BUILD, context -> {
            WarHorse wh = new WarHorse(context);
            wh.workload(new NvrDirOperatorFactory(), BuildJob.class);
        });
        get(REDEPLOY_TEST, context -> {
            WarHorse wh = new WarHorse(context);
            wh.workload(new FakeNvrDirOperatorFactory(), TestJob.class);
        });
        get(ARCHES_EXPECTED, context -> {
            try {
                String nvr = context.queryParam(REDEPLOY_NVR);
                if (nvr != null) {
                    OToolParser.LegacyNVR nvrParsed = new OToolParser.LegacyNVR(nvr);
                    String set = context.queryParam(ARCHES_EXPECTED_SET);
                    if (set == null) {
                        File f = new File(settings.getDbFileRoot().toPath().toFile().getAbsolutePath() + "/" + nvrParsed.getFullPath() + "/data");
                        while (f.getParentFile() != null) {
                            File ae = new File(f, FakeBuild.archesConfigFileName);
                            if (ae.exists()) {
                                break;
                            }
                            f = f.getParentFile();
                        }
                        if (f.getParentFile() == null) {
                            context.status(OToolService.OK).result("No " + FakeBuild.archesConfigFileName + " for " + nvr + " arches expected are old api only!\n");
                        } else {
                            File ae = new File(f, FakeBuild.archesConfigFileName);
                            //by luck, same comments schema is used in processed.txt and arches.expected
                            List<String> archesWithoutCommenrs = Utils.readProcessedTxt(ae);
                            if (ae.length() < 4 || archesWithoutCommenrs.isEmpty()) {
                                context.status(OToolService.BAD).result(ae.getAbsolutePath() + " is emmpty or very small. That will break a lot of stuff!\n");
                            } else {
                                context.status(OToolService.OK).result(String.join(" ", archesWithoutCommenrs.get(0)) + " (" + ae.getAbsolutePath() + ")\n");
                            }
                        }
                    } else {
                        File mainPath = new File(settings.getDbFileRoot().toPath().toFile().getAbsolutePath() + "/" + nvrParsed.getFullPath());
                        if (!mainPath.exists()) {
                            context.status(OToolService.BAD).result(mainPath.getAbsolutePath() + " Do not exists. Invlid NVRos?\n");
                        } else {
                            File data = new File(mainPath, "data");
                            File mainFile = new File(data, FakeBuild.archesConfigFileName);
                            if (mainFile.exists()) {
                                List<String> archesWithoutCommenrs = Utils.readProcessedTxt(mainFile);
                                FakeBuild.generateDefaultArchesFile(mainFile, set);
                                if (archesWithoutCommenrs.isEmpty()) {
                                    context.status(OToolService.OK).result("overwritten " + mainFile + " (was: empty, is`" + set + "`)\n");
                                } else {
                                    context.status(OToolService.OK).result("overwritten " + mainFile + " (was: `" + archesWithoutCommenrs.get(0) + "`, is`" + set + "`)\n");
                                }

                            } else {
                                data.mkdirs();
                                FakeBuild.generateDefaultArchesFile(mainFile, set);
                                context.status(OToolService.OK).result("written " + mainFile + "  (is`" + set + "`)\n");
                            }
                        }
                    }
                } else {
                    ArchesExpectedWorker aw = new ArchesExpectedWorker();
                    aw.prepare();
                    List<Platform> platforms = platformManager.readAll();
                    Set<String> kojiArches = new HashSet<>();
                    for (Platform p : platforms) {
                        kojiArches.add(p.getKojiArch().orElse(p.getArchitecture()));
                    }
                    //filtr affected jobs + places in builds (s neb arches>
                    context.status(OToolService.OK).result(aw.dirsWithArches.entrySet().stream().
                            map((Function<Map.Entry<String, String[]>, String>) e -> String.join(" ", e.getValue()) + " (" + e.getKey() + ")").
                            sorted().
                            collect(Collectors.joining("\n")) + "\n" +
                            "All used: " + aw.usedArches.stream().
                            collect(Collectors.joining(" ")) + "\n" +
                            "All possible: " + kojiArches.stream().
                            collect(Collectors.joining(" ")) + "\n");

                    //better to filter them up rather then here
                }
            } catch (StorageException | IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(400).result(e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getMessage());
            }
        });
    }

    private File deleteBuildXml(String job, StringBuilder sb) throws IOException {
        File currentt = new File(settings.getJenkinsJobsRoot().getAbsolutePath() + File.separator + job + File.separator + "build.xml");
        if (!currentt.getParentFile().exists()) {
            throw new RuntimeException(currentt.getParentFile() + " do not exists. bad job?");
        }
        if (!currentt.exists()) {
            sb.append(currentt.getAbsolutePath() + " do not exists. bad job? Broken checkou?t No koji-scm job?\n");
        }
        Files.delete(currentt.toPath());
        sb.append("deleted " + currentt.getAbsolutePath() + "\n");
        return currentt;
    }


    private StringBuilder forceCheckoutOnJob(String job, String nvr, boolean force) throws IOException {
        if (job == null) {
            throw new RuntimeException(RERUN_JOB + " must be an existing job id, was " + job);
        }
        StringBuilder sb = new StringBuilder();
        boolean removed = true;
        if (nvr != null) {
            File processed = new File(settings.getJenkinsJobsRoot().getAbsolutePath() + File.separator + job + File.separator + Constants.PROCESSED_BUILDS_HISTORY);
            sb.append(job + " - " + nvr + "\n");
            Utils.RemovedNvrsResult details = Utils.removeNvrFromProcessed(processed, nvr);
            if (details.removed() <= 0 || details.removedUniq() <= 0) {
                String ew = "Error";
                if (force) {
                    ew = "Warning";
                }
                sb.append(" " + ew + " ! Nothing removed - " + details.toString() + "\n");
                removed = false;
            } else {
                sb.append("  Ok - " + details.toString() + "\n");
                removed = true;
            }
        }
        if (removed || force) {
            deleteBuildXml(job, sb);
            JenkinsCliWrapper.ClientResponse cr = JenkinsCliWrapper.getCli().scheduleBuild(job);
            cr.throwIfNecessary();
            sb.append("scheduled " + job + "\n");
        } else {
            sb.append("not scheduling " + job + "\n");
        }
        return sb;
    }

    private class ArchesExpectedWorker {

        private final Map<String, String[]> dirsWithArches = new HashMap<>();
        private final Set<String> usedArches = new HashSet<>();
        private boolean called = false;

        public void prepare() throws IOException {
            if (called) {
                throw new IOException("No need to call tis twice");
            }
            called = true;
            Files.walkFileTree(settings.getDbFileRoot().toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File f = file.toFile();
                    if (f.getName().equals(FakeBuild.archesConfigFileName)) {
                        //by luck, same comments schema is used in processed.txt and arches.expected
                        List<String> archesWithoutCommenrs = Utils.readProcessedTxt(f);
                        if (archesWithoutCommenrs.size() > 0 || f.length() <= 4) {
                            dirsWithArches.put(f.getParentFile().getAbsolutePath(), archesWithoutCommenrs.get(0).split("\\s+")); //also usage of this file takes first found, valid line
                            usedArches.addAll(Arrays.asList(archesWithoutCommenrs.get(0).split("\\s+")));
                        } else {
                            dirsWithArches.put(f.getParentFile().getAbsolutePath(), new String[]{"invalid"});
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });

        }
    }

    private class RedeployApiWorker extends RedeployApiWorkerBase.RedeployApiListingWorker {

        private final Set<String> nvrsInProcessedTxt = new HashSet();
        private final Map<String, Job> allRelevantJobsMap = new HashMap<>();
        private final Map<String, List<String>> nvrsPerJob = new HashMap<>();
        private final Map<String, List<Job>> jobsPerNvr = new HashMap<>();
        private final List<String> sortedNvrs = new ArrayList();
        private final Class clazz;
        private boolean called = false;

        public RedeployApiWorker(Context context, Class clazz) {
            super(context);
            this.clazz = clazz;
            if (clazz.equals(BuildJob.class)) {
                this.build = "true";
            }
        }

        public void prepare() throws StorageException, IOException, ManagementException {
            if (called) {
                throw new IOException("No need to call tis twice");
            }
            called = true;
            iterate(jdkProjectManager, jdkTestProjectManager, parser);
            sortedNvrs.addAll(nvrsInProcessedTxt);
            Collections.sort(sortedNvrs);
        }

        @Override
        protected void onPass(Job job) throws IOException {
            if (clazz.isInstance(job)) {
                allRelevantJobsMap.put(job.getName(), job);
                File processed = new File(new File(settings.getJenkinsJobsRoot(), job.getName()), Constants.PROCESSED_BUILDS_HISTORY);
                if (processed.exists()) {
                    List<String> processedNvrsByThisJob = Utils.readProcessedTxt(processed);
                    nvrsPerJob.put(job.getName(), processedNvrsByThisJob);
                    nvrsInProcessedTxt.addAll(processedNvrsByThisJob);
                    for (String nvr : processedNvrsByThisJob) {
                        List<Job> jobsOfThisNvr = jobsPerNvr.get(nvr);
                        if (jobsOfThisNvr == null) {
                            jobsOfThisNvr = new ArrayList<>();
                            jobsPerNvr.put(nvr, jobsOfThisNvr);
                        }
                        jobsOfThisNvr.add(job);
                    }
                }
            }
        }

        public List<Job> getJobsOfThisNvr(String nvr) {
            return jobsPerNvr.get(nvr);
        }

        public Tuple<String, Integer[]> removeNvrFromProcessedOfAffectedJobs(String nvr) {
            StringBuilder result = new StringBuilder();
            int issues = 0;
            int ok = 0;
            for (Job job : jobsPerNvr.get(nvr)) {
                try {
                    File processed = new File(new File(settings.getJenkinsJobsRoot(), job.getName()), Constants.PROCESSED_BUILDS_HISTORY);
                    result.append(job.getName() + " - " + nvr + "\n");
                    Utils.RemovedNvrsResult details = Utils.removeNvrFromProcessed(processed, nvr);
                    if (details.removed() <= 0 || details.removedUniq() <= 0) {
                        result.append("  Error! Nothing removed - " + details.toString() + "\n");
                        issues++;
                    } else {
                        result.append("  Ok - " + details.toString() + "\n");
                        ok++;
                    }

                } catch (Exception ex) {
                    issues++;
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    result.append("  " + ex.toString() + "\n");
                }
            }
            return new Tuple<>(result.toString(), new Integer[]{issues, ok});
        }
    }

    private class NvrDirOperatorFactory {
        public NvrDirOperator createFor(OToolBuild otoolBuild, List<Job> validJobs) {
            return new NvrDirOperator(otoolBuild, validJobs);
        }

        public boolean newApiOnly() {
            return true;
        }
    }

    private class FakeNvrDirOperatorFactory extends NvrDirOperatorFactory {
        @Override
        public NvrDirOperator createFor(OToolBuild otoolBuild, List<Job> validJobs) {
            return new FakeNvrDirOperator();
        }

        @Override
        public boolean newApiOnly() {
            return false;
        }
    }

    private class FakeNvrDirOperator extends NvrDirOperator {

        @Override
        public void walk() throws IOException {
            //no need to waste time
        }

        @Override
        public boolean canDeleteWihtoutForce() {
            return true;
        }

        @Override
        public String toOutput() {
            return "No NVRAs to delete, see affected jobs:";
        }

        @Override
        public Tuple<String, Integer[]> deleteSelected() {
            return new Tuple("No files should be delted during test rescheduling.", new Integer[]{0, 0});
        }
    }

    private class NvrDirOperator {
        private final File mainDir;
        private final OToolBuild build;
        private final List<Path> affectedDirsAndFiles = new ArrayList<>();
        private final List<Path> affectedDirs = new ArrayList<>();
        private final List<Path> affectedFiles = new ArrayList<>();
        private final List<Job> validJobs;

        public NvrDirOperator() {
            this.build = null;
            this.mainDir = null;
            validJobs = null;
        }

        public NvrDirOperator(OToolBuild value, List<Job> validJobs) {
            this.build = value;
            this.mainDir = new File(settings.getDbFileRoot().getAbsolutePath() + "/" + build.toPathStub());
            this.validJobs = validJobs == null ? new ArrayList<>(0) : validJobs;
        }

        public void walk() throws IOException {
            affectedDirsAndFiles.clear();
            affectedDirs.clear();
            affectedFiles.clear();
            Files.walkFileTree(mainDir.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (pairedToList(dir)) {
                        affectedDirsAndFiles.add(dir);
                        affectedDirs.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (pairedToList(file)) {
                        affectedDirsAndFiles.add(file);
                        affectedFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        private boolean pairedToList(Path path) {
            while (path != null) {
                for (Job job : validJobs) {
                    BuildJob bjob = (BuildJob) job; //all the deleting is aimed to build jobs only
                    String variantsString = bjob.getVariants().entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry::getKey))
                            .map(entry -> entry.getValue().getId())
                            .collect(Collectors.joining(Job.VARIANTS_DELIMITER));
                    String dirname = variantsString + Job.VARIANTS_DELIMITER + bjob.getPlatform().getId();
                    if (path.getFileName() != null &&
                            path.getFileName().toString().equals(dirname)) {
                        return true;
                    }
                }
                path = path.getParent();
            }
            return false;
        }

        public boolean canDeleteWihtoutForce() {
            return affectedDirs.size() < 2 && affectedFiles.size() < 2;
        }

        public String toOutput() {
            if (affectedDirsAndFiles.isEmpty()) {
                return "No files affected! bad filter?";
            } else {
                return affectedDirsAndFiles.stream().map(Path::toString).collect(Collectors.joining("\n"));
            }
        }

        public Tuple<String, Integer[]> deleteSelected() {
            StringBuilder result = new StringBuilder();
            int[] ok = new int[]{0};
            int[] issues = new int[]{0};
            deleteFileWithResult(affectedFiles, result, ok, issues);
            deleteFileWithResult(affectedDirs, result, ok, issues);
            return new Tuple<>(result.toString(), new Integer[]{issues[0], ok[0]});
        }

        private void deleteFileWithResult(List<Path> affectedItems, StringBuilder result, int[] ok, int[] issues) {
            for (Path file : affectedItems) {
                try {
                    result.append(file.toString() + " - " + build.toNiceString() + "\n");
                    Files.delete(file);
                    result.append("  Ok - deleted\n");
                    ok[0]++;
                } catch (Exception ex) {
                    issues[0]++;
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    result.append("  " + ex.toString() + "\n");
                }
            }
        }
    }

    private class WarHorse {

        private final Context context;

        public WarHorse(Context context) {
            this.context = context;
        }

        public void workload(NvrDirOperatorFactory nvdf, Class clazz) {
            try {
                RedeployApiWorker raw = new RedeployApiWorker(context, clazz);
                raw.prepare();
                String nvr = context.queryParam(REDEPLOY_NVR);
                if (nvr == null) {
                    context.status(OToolService.OK).result(String.join("\n", raw.sortedNvrs) + "\n");
                } else {
                    //builds are new api only!
                    OToolParser op = new OToolParser(
                            jdkProjectManager.readAll(),
                            jdkVersionManager.readAll(),
                            taskVariantManager.getBuildVariants());
                    //there is an catch
                    //where argument for build job is NVR, and by that we can affect processed.txt
                    //to remvove it from DB, nvra would be better. Or to do that via bos/arch and friends?
                    //if via other "b*" args, then sources should be filtered
                    //warn if more then one file and one dir is deleted, require do=force // and password? YYMMDDHHMM?
                    Result<OToolBuild, String> parsedNvr = op.parseBuild(nvr);
                    if (parsedNvr.isError() && nvdf.newApiOnly()) {
                        throw new RuntimeException("cannot parse " + nvr + " rebuild is new api only.\n");
                    }
                    NvrDirOperator nvd = nvdf.createFor(parsedNvr.getValue(), raw.getJobsOfThisNvr(nvr));
                    nvd.walk();
                    if (raw.getJobsOfThisNvr(nvr) == null) {
                        context.status(OToolService.BAD).result(nvd.toOutput() + "\n" + "jobs which run exact " + nvr + " are null\n");
                    } else {
                        String doAndHow = context.queryParam(REDEPLOY_DO);
                        if (doAndHow == null) {
                            if (raw.getJobsOfThisNvr(nvr).isEmpty()) {
                                context.status(OToolService.BAD).result(nvd.toOutput() + "\n" + "jobs which run exact " + nvr + " are empty\n");
                            } else {
                                context.status(OToolService.OK).result(nvd.toOutput() + "\n" + raw.getJobsOfThisNvr(nvr).stream().map(Job::getName).sorted().collect(Collectors.joining("\n")) + "\n");
                            }
                        } else {
                            if (nvd.canDeleteWihtoutForce() || (doAndHow.equals("force"))) {
                                Tuple<String, Integer[]> delete = nvd.deleteSelected();
                                Tuple<String, Integer[]> reschedul = raw.removeNvrFromProcessedOfAffectedJobs(nvr);
                                String result = delete.x + "\n" + reschedul.x + "\n"
                                        + summUp("Deleted:", delete.y[0], delete.y[1], nvd.affectedDirsAndFiles.size()) + "\n"
                                        + summUp("Rescheduled:", reschedul.y[0], reschedul.y[1], raw.getJobsOfThisNvr(nvr).size()) + "\n"
                                        + finalSentence(delete.y[0] + reschedul.y[0]);
                                context.status((delete.y[0] + reschedul.y[0]) == 0 ? OToolService.OK : OToolService.BAD).result(result);
                            } else {
                                context.status(OToolService.BAD).result("To much files to delete, verify by listing (remove `do`), and then `do=force`\n");
                            }
                        }
                    }
                }
            } catch (StorageException | ManagementException | IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(400).result(e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getMessage());
            }
        }

        private String summUp(String preffix, int issues, int ok, int total) {
            return preffix + " " + ok + " from total " + total + ". Failed " + issues + " that is " + (ok + issues) + "/" + total;
        }

        private String finalSentence(int i) {
            if (i > 0) {
                return "Error: there were " + i + " failures\n";
            } else {
                return "Ok\n";
            }
        }
    }
}



