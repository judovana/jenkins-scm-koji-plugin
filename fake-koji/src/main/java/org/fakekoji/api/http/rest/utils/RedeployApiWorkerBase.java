package org.fakekoji.api.http.rest.utils;

import io.javalin.http.Context;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RedeployApiWorkerBase {
    //other details for selection, all can be coma separated lists
    private static final String REDEPLOY_project = "project";
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
    //pull and build
    private static final String REDEPLOY_pull = "pull"; //false
    private static final String REDEPLOY_build = "build"; //false

    protected final Matcher project;
    protected final Matcher os;
    protected final Matcher arch;
    protected final Matcher version;
    protected final Matcher task;
    protected final Matcher variant;
    protected final Matcher provider;
    protected final Matcher jp;
    protected final Matcher bos;
    protected final Matcher barch;
    protected final Matcher bversion;
    protected final Matcher bvariant;
    protected final Pattern blacklist;
    protected final Pattern whitelist;
    protected String pull;
    protected String build;

    public RedeployApiWorkerBase(
            Matcher project,
            Matcher os,
            Matcher arch,
            Matcher version,
            Matcher task,
            Matcher variant,
            Matcher provider,
            Matcher jp,
            Pattern blacklist,
            Pattern whitelist,
            String pull,
            String build) {
        this(project, os, arch, version, task, variant, provider, jp, null, null, null, null, blacklist, whitelist, pull, build);
    }

    public RedeployApiWorkerBase(
            Matcher project,
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
            Pattern whitelist,
            String pull,
            String build) {
        this.project = project;
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
        this.pull = pull;
        this.build = build;
    }

    public RedeployApiWorkerBase(Context context) {
        this(new Matcher(context.queryParam(REDEPLOY_project)),
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
                context.queryParam(REDEPLOY_whitelist) == null ? Pattern.compile(".*") : Pattern.compile(context.queryParam(REDEPLOY_whitelist)),
                context.queryParam(REDEPLOY_pull),
                context.queryParam(REDEPLOY_build)
        );
    }

    public static String getHelp() {
        return "  You can narrow your search by [" + REDEPLOY_os + "," + REDEPLOY_arch + "," + REDEPLOY_version + "," + REDEPLOY_task + "," + REDEPLOY_variant + "," + REDEPLOY_provider + "," + REDEPLOY_jp + "," + REDEPLOY_project + "]\n"
                + "  For test-task only, to narrow by its build: [" + REDEPLOY_bos + "," + REDEPLOY_barch + "," + REDEPLOY_bversion + "," + REDEPLOY_bvariant + "]\n"
                + "    Those are coma separated lists. eg variant=shenandoah,zgc&bvarinat=jre,fastdebug&bos=el&bversion=6,7&version=8\n"
                + "  you can use " + REDEPLOY_whitelist + "=regex and " + REDEPLOY_blacklist + "=regex to do some more wide/narrow filtering.\n"
                + "  by default only the warhorses are listed. To include also pull and build jobs use " + REDEPLOY_pull + "=true and " + REDEPLOY_build + "=true.\n";
    }

    public boolean blacklisted(Job job) {
        return blacklist.matcher(job.getName()).matches();
    }

    public boolean whitelisted(Job job) {
        return whitelist.matcher(job.getName()).matches();
    }

    public boolean isNotMyBuildJob(BuildJob bjob) {
        return
                !project.matches(bjob.getProjectName()) ||
                        !os.matches(bjob.getPlatform().getOs()) ||
                        !version.matches(bjob.getPlatform().getVersion()) ||
                        !arch.matches(bjob.getPlatform().getArchitecture()) ||
                        !provider.matches(bjob.getPlatformProvider()) ||
                        !variant.matchesTaskVariants(bjob.getVariants());
    }

    public boolean isNotMyTestJob(TestJob tjob) {
        return
                !project.matches(tjob.getProjectName()) ||
                        !os.matches(tjob.getPlatform().getOs()) ||
                        !version.matches(tjob.getPlatform().getVersion()) ||
                        !arch.matches(tjob.getPlatform().getArchitecture()) ||
                        !provider.matches(tjob.getPlatformProvider()) ||
                        !task.matches(tjob.getTask().getId()) ||
                        !jp.matches(tjob.getJdkVersion().getId()) ||
                        !variant.matchesTaskVariants(tjob.getVariants()) ||
                        !bos.matches(tjob.getBuildPlatform().getOs()) ||
                        !barch.matches(tjob.getBuildPlatform().getArchitecture()) ||
                        !bversion.matches(tjob.getBuildPlatform().getVersion()) ||
                        !bvariant.matchesTaskVariants(tjob.getBuildVariants());

    }


    public static abstract class RedeployApiListingWorker extends RedeployApiWorkerBase {

        public RedeployApiListingWorker(Context context) {
            super(context);
        }

        protected abstract void onPass(Job job) throws IOException;

        public void iterate(final JDKProjectManager jdkProjectManager, final JDKTestProjectManager jdkTestProjectManager, JDKProjectParser parser) throws IOException, StorageException, ManagementException {
            List<Project> allProjects = new ArrayList<>();
            allProjects.addAll(jdkProjectManager.readAll());
            allProjects.addAll(jdkTestProjectManager.readAll());
            iterate(allProjects, parser);
        }

        public void iterate(final List<Project> allProjects, final JDKProjectParser parser) throws IOException, StorageException, ManagementException {
            for (Project project : allProjects) {
                Set<Job> jobs = parser.parse(project);
                iterate(jobs);
            }
        }

        public void iterate(final Collection<Job> jobs) throws IOException {
            for (Job job : jobs) {
                if (blacklisted(job)) {
                    continue;
                }
                if (!whitelisted(job)) {
                    continue;
                }
                if (job instanceof BuildJob) {
                    BuildJob bjob = (BuildJob) job;
                    if (isNotMyBuildJob(bjob)) {
                        continue;
                    }
                }
                if (job instanceof TestJob) {
                    TestJob tjob = (TestJob) job;
                    if (isNotMyTestJob(tjob)) {
                        continue;
                    }
                }
                if (job instanceof PullJob) {
                    if (!"true".equals(pull)) {
                        continue;
                    }
                }
                if (job instanceof BuildJob) {
                    if (!"true".equals(build)) {
                        continue;
                    }
                }
                onPass(job);
            }
        }
    }

    public static class RedeployApiJobListing extends RedeployApiListingWorker {
        List<Job> results;

        public RedeployApiJobListing(Context context) {
            super(context);
        }

        public List<Job> process(final JDKProjectManager jdkProjectManager, final JDKTestProjectManager jdkTestProjectManager, JDKProjectParser parser) throws IOException, StorageException, ManagementException {
            results = new ArrayList<>();
            iterate(jdkProjectManager, jdkTestProjectManager, parser);
            return results;
        }

        @Override
        protected void onPass(Job job) {
            results.add(job);
        }
    }

    public static class RedeployApiStringListing extends RedeployApiListingWorker {
        protected List<String> results;

        public RedeployApiStringListing(Context context) {
            super(context);
        }

        public List<String> process(final JDKProjectManager jdkProjectManager, final JDKTestProjectManager jdkTestProjectManager, JDKProjectParser parser) throws IOException, StorageException, ManagementException {
            results = new ArrayList<>();
            iterate(jdkProjectManager, jdkTestProjectManager, parser);
            return results;
        }

        @Override
        protected void onPass(Job job) {
            results.add(job.getName());
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
}