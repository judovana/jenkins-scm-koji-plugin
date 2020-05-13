package org.fakekoji.api.http.rest;

import hudson.plugins.scm.koji.Constants;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.FakeBuild;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.Platform;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;

public class RedeployApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String REDEPLOY = "re";
    //without list/do just list list waht can be done?
    //eg list of nvras in processed.txt for test
    //eg list new api content of fake koji for build?
    //already filtered via other filters?
    private static final String REDEPLOY_TEST = "test"; //test have sense with all the swithces before, build do not. build needs only 1/2 nvra
    private static final String REDEPLOY_BUILD = "build"; //only jdkProject in addtion to removal VR from processed.txt, it cleans  FAILED, ERROR and smaller then 4bytes files from local-builds
    private static final String REDEPLOY_DO = "do"; //will do the real work if true, by default it will only print what it will affect
    //with?
    private static final String REDEPLOY_NVR = "nvr"; //and enforce platform as separate thing?

    //other details for selection, all can be coma separated lists
    private static final String REDEPLOY_os = "os";
    private static final String REDEPLOY_arch = "arch";
    private static final String REDEPLOY_version = "version";
    private static final String REDEPLOY_task = "task";
    private static final String REDEPLOY_variant = "variant";
    private static final String REDEPLOY_provider = "provider";

    //sometimes we need also build arch to judge , all can be coma separated lists
    private static final String REDEPLOY_jp = "jp";
    private static final String REDEPLOY_bos = "bos";
    private static final String REDEPLOY_barch = "barch";
    private static final String REDEPLOY_bversion = "bversion";
    private static final String REDEPLOY_bvariant = "bvariant";

    //is needed at the end?
    private static final String REDEPLOY_whitelist = "whitelist";
    private static final String REDEPLOY_blacklist = "blacklist";

    private static final String ARCHES_EXPECTED = "archesExpected";
    private static final String ARCHES_EXPECTED_SET = "set";


    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;
    private final PlatformManager platformManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;

    RedeployApi(
            final JDKProjectParser jdkProjectParser,
            final JDKProjectManager jdkProjectManager,
            final JDKTestProjectManager jdkTestProjectManager,
            final PlatformManager platformManager,
            final JDKVersionManager jdkVersionManager,
            final TaskVariantManager taskVariantManager,
            final AccessibleSettings settings
    ) {
        this.parser = jdkProjectParser;
        this.jdkProjectManager = jdkProjectManager;
        this.jdkTestProjectManager = jdkTestProjectManager;
        this.platformManager = platformManager;
        this.jdkVersionManager = jdkVersionManager;
        this.taskVariantManager = taskVariantManager;
        this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_BUILD + "\n"
                + "  Will print out all nvrs in processed.txt of builds.\n"
                + MISC + '/' + REDEPLOY + "/" + REDEPLOY_TEST + "\n"
                + "  Will print out all nvrs in processed.txt of tests.\n"
                + "Shared by both:\n"
                + "  once you set " + REDEPLOY_NVR + "=nvr the jobs affected by this nvr will be printed.\n"
                + "  You can narrow your search by [" + REDEPLOY_os + "," + REDEPLOY_arch + "," + REDEPLOY_version + "," + REDEPLOY_task + "," + REDEPLOY_variant + "," + REDEPLOY_provider + "," + REDEPLOY_jp + "]\n"
                + "  For test-task only, to narrow by its build: [" + REDEPLOY_bos + "," + REDEPLOY_barch + "," + REDEPLOY_bversion + "," + REDEPLOY_bvariant + "," + "]\n"
                + "    Those are coma separated lists. eg variant=shenandoah,zgc&bvarinat=jre,fastdebug&bos=el&bversion=6,7&version=8\n"
                + "  you can use " + REDEPLOY_whitelist + "=regex and " + REDEPLOY_blacklist + "=regex to do some more wide/narrow filtering.\n"
                + "  once you set " + REDEPLOY_DO + "=true, the real work will happen - nvr will be removed from affected jobs.\n"
                + "  For " + REDEPLOY_BUILD + " it also removes the affected NVRA from database. A is deducted  from other params\n"
                + "\n"
                + MISC + '/' + REDEPLOY + '/' + ARCHES_EXPECTED + " will list exisiting " + FakeBuild.archesConfigFileName + " files"
                + "  with " + REDEPLOY_NVR + "  it shows arches expectewd of this NVR (dont forget, that in old api R contan also OS!\n"
                + "  with " + ARCHES_EXPECTED_SET + " and " + REDEPLOY_NVR + "  set arches  for  this NVR\n"
                + "    To set more global arches, you must go via file system, as we have new api, dont do that\n";


    }


    @Override
    public void addEndpoints() {
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
                context.status(400).result(e.getMessage());
            } catch (Exception e) {
                context.status(500).result(e.getMessage());
            }
        });
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

    private class RedeployApiWorker {
        private final Set<String> nvrsInProcessedTxt = new HashSet();
        private final Map<String, Job> allRelevantJobsMap = new HashMap<>();
        private final Map<String, List<String>> nvrsPerJob = new HashMap<>();
        private final Map<String, List<Job>> jobsPerNvr = new HashMap<>();
        private final List<String> sortedNvrs = new ArrayList();
        private boolean called = false;
        private final Matcher os;
        private final Matcher arch;
        private final Matcher version;
        private final Matcher task;
        private final Matcher variant;
        private final Matcher provider;
        private final Matcher jp;
        private final Matcher bos;
        private final Matcher barch;
        private final Matcher bversion;
        private final Matcher bvariant;
        private final Pattern blacklist;
        private final Pattern whitelist;

        public RedeployApiWorker(
                Matcher os,
                Matcher arch,
                Matcher version,
                Matcher task,
                Matcher variant,
                Matcher provider,
                Matcher jp,
                Matcher bos,
                Matcher barch,
                Matcher bversion,
                Matcher bvariant,
                Pattern blacklist,
                Pattern whitelist) {
            this.os = os;
            this.arch = arch;
            this.version = version;
            this.task = task;
            this.variant = variant;
            this.provider = provider;
            this.jp = jp;
            this.bos = bos;
            this.barch = barch;
            this.bversion = bversion;
            this.bvariant = bvariant;
            this.blacklist = blacklist;
            this.whitelist = whitelist;
        }


        public void prepare(Class clazz) throws StorageException, IOException, ManagementException {
            if (called) {
                throw new IOException("No need to call tis twice");
            }
            called = true;
            List<Project> allProjects = new ArrayList<>();
            allProjects.addAll(jdkProjectManager.readAll());
            allProjects.addAll(jdkTestProjectManager.readAll());
            for (Project project : allProjects) {
                Set<Job> jobs = parser.parse(project);
                for (Job job : jobs) {
                    if (blacklist.matcher(job.getName()).matches()) {
                        continue;
                    }
                    if (!whitelist.matcher(job.getName()).matches()) {
                        continue;
                    }
                    if (job instanceof BuildJob) {
                        BuildJob bjob = (BuildJob) job;
                        if (!os.matches(bjob.getPlatform().getOs()) ||
                                !version.matches(bjob.getPlatform().getVersion()) ||
                                !arch.matches(bjob.getPlatform().getArchitecture()) ||
                                !provider.matches(bjob.getPlatformProvider()) ||
                                !variant.matchesTaskVariants(bjob.getVariants())) {
                            continue;
                        }
                    }
                    if (job instanceof TestJob) {
                        TestJob tjob = (TestJob) job;
                        if (!os.matches(tjob.getPlatform().getOs()) ||
                                !version.matches(tjob.getPlatform().getVersion()) ||
                                !arch.matches(tjob.getPlatform().getArchitecture()) ||
                                !provider.matches(tjob.getPlatformProvider()) ||
                                !task.matches(tjob.getTask().getId()) ||
                                !jp.matches(tjob.getJdkVersion().getId()) ||
                                !variant.matchesTaskVariants(tjob.getVariants()) ||
                                !bos.matches(tjob.getBuildPlatform().getOs()) ||
                                !barch.matches(tjob.getBuildPlatform().getArchitecture()) ||
                                !bversion.matches(tjob.getBuildPlatform().getVersion()) ||
                                !bvariant.matchesTaskVariants(tjob.getBuildVariants())) {
                            continue;
                        }
                    }
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
            }
            sortedNvrs.addAll(nvrsInProcessedTxt);
            Collections.sort(sortedNvrs);
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

    private static class Matcher {
        private final String orig;
        private final Set<String> split;

        private Matcher(String orig) {
            this.orig = orig;
            split = new HashSet<String>();
            if (orig != null) {
                split.addAll(Arrays.asList(orig.split(",")));
            }
        }

        public boolean matches(String value) {
            if (orig == null) {
                return true;
            }
            ;
            for (String s : split) {
                if (s.equals(value)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesTaskVariants(Map<TaskVariant, TaskVariantValue> variants) {
            return matchesTaskVariants(variants.values());
        }

        public boolean matchesTaskVariants(Collection<TaskVariantValue> variants) {
            return matches(variants.stream().map(TaskVariantValue::getId).collect(Collectors.toList()));
        }

        public boolean matches(Collection<String> values) {
            if (orig == null) {
                return true;
            }
            int found = 0;
            for (String s : split)
                for (String value : values) {
                    if (s.equals(value)) {
                        found++;
                    }
                }
            return found == split.size();
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
                RedeployApiWorker raw = new RedeployApiWorker(
                        new Matcher(context.queryParam(REDEPLOY_os)),
                        new Matcher(context.queryParam(REDEPLOY_arch)),
                        new Matcher(context.queryParam(REDEPLOY_version)),
                        new Matcher(context.queryParam(REDEPLOY_task)),
                        new Matcher(context.queryParam(REDEPLOY_variant)),
                        new Matcher(context.queryParam(REDEPLOY_provider)),
                        new Matcher(context.queryParam(REDEPLOY_jp)),
                        new Matcher(context.queryParam(REDEPLOY_bos)),
                        new Matcher(context.queryParam(REDEPLOY_barch)),
                        new Matcher(context.queryParam(REDEPLOY_bversion)),
                        new Matcher(context.queryParam(REDEPLOY_bvariant)),
                        context.queryParam(REDEPLOY_blacklist) == null ? Pattern.compile("NothingNeverEverCanMatchMe!") : Pattern.compile(context.queryParam(REDEPLOY_blacklist)),
                        context.queryParam(REDEPLOY_whitelist) == null ? Pattern.compile(".*") : Pattern.compile(context.queryParam(REDEPLOY_whitelist))
                );
                raw.prepare(clazz);
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
                                context.status((delete.y[0] + reschedul.y[0])==0?OToolService.OK:OToolService.BAD).result(result);
                            } else {
                                context.status(OToolService.BAD).result("To much files to delete, verify by listing (remove `do`), and then `do=force`\n");
                            }
                        }
                    }
                }
            } catch (StorageException | ManagementException | IOException e) {
                context.status(400).result(e.getMessage());
            } catch (Exception e) {
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



