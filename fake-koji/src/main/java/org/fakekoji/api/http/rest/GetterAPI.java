package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.*;
import org.fakekoji.storage.StorageException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class GetterAPI implements EndpointGroup {

    private static final String ERROR_PARAMETERS_EXPECTED = "Parameters expected";
    private static final String ERROR_PROJECT_NOT_FOUND = "Project not found";

    private static final String ARCHIVE = "archive";
    private static final String BUILD = "build";
    private static final String BUILDS = "builds";
    private static final String CONFIGS = "configs";
    private static final String FILE_DOWNLOAD = "fileDownload";
    private static final String JDK_VERSION = "jdkVersion";
    private static final String JENKINS_JOBS = "jenkinsJobs";
    private static final String JENKINS_JOB_ARCHIVE = "jenkinsJobArchive";
    private static final String JOBS = "jobs";
    private static final String TASKS = "tasks";
    private static final String PATH = "path";
    private static final String PORT = "port";
    private static final String PORTS = "ports"; // TODO
    private static final String PRODUCT = "product";
    private static final String PRODUCTS = "products";
    private static final String PROJECT = "project";
    private static final String PROJECTS = "projects";
    private static final String ADDITIONAL_RULES = "rules";
    private static final String FILENAME_PARSER = "filenameParser";
    private static final String LEGACY_PARSER = "legacyParser";
    private static final String PLATFORMS = "platforms";
    private static final String PLATFORMS_DETAILS = "platformDetails";
    private static final String PLATFORM_ID = "id";
    private static final String KOJI_ARCHES = "kojiArches";
    private static final String REPOS = "repos";
    private static final String ROOT = "root";
    private static final String ROOTS = "roots"; // TODO
    private static final String SERVICE = "service";
    private static final String SSH = "ssh";
    private static final String TYPE = "type";
    private static final String NVR = "nvr";
    private static final String AS_REGEX_LIST = "as";
    private static final String UNSAFE = "unsafe";
    private static final String WEBAPP = "webapp";
    private static final String XML_RPC = "xmlRpc";

    private static final String JDK_VERSIONS = "jdkVersions";
    private static final String HELP = "help";

    private final AccessibleSettings settings;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;
    private final PlatformManager platformManager;
    private final TaskManager taskManager;

    public GetterAPI(
            final AccessibleSettings settings,
            final JDKProjectManager jdkProjectManager,
            final JDKTestProjectManager jdkTestProjectManager,
            final JDKVersionManager jdkVersionManager,
            final TaskVariantManager taskVariantManager,
            final PlatformManager platformManager,
            final TaskManager taskManager
    ) {
        this.settings = settings;
        this.jdkProjectManager = jdkProjectManager;
        this.jdkTestProjectManager = jdkTestProjectManager;
        this.jdkVersionManager = jdkVersionManager;
        this.taskVariantManager = taskVariantManager;
        this.platformManager = platformManager;
        this.taskManager = taskManager;
    }

    private Optional<String> extractParamValue(Map<String, List<String>> paramsMap, String param) {
        return Optional.ofNullable(paramsMap.get(param))
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0));
    }


    private QueryHandler getJobsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> paramsMap) throws StorageException, ManagementException {
                final Optional<String> allInJenkinsOpt = extractParamValue(paramsMap, "allJenkins");
                final Optional<String> allInOtoolOpt = extractParamValue(paramsMap, "allOtool");
                final Optional<String> urlParam = extractParamValue(paramsMap, "URL");
                final Optional<String> orphansOnJenkinsParam = extractParamValue(paramsMap, "orphansJenkins");
                final Optional<String> orphansOnOtoolParam = extractParamValue(paramsMap, "orphansOtool");
                final Optional<String> testParam = extractParamValue(paramsMap, "jdkTestProjects");
                final Optional<String> jdkParam = extractParamValue(paramsMap, "jdkProjects");
                final Optional<String> excludeParam = extractParamValue(paramsMap, "exclude");
                final Optional<String> includeParam = extractParamValue(paramsMap, "include");
                final Optional<String> projectParam = extractParamValue(paramsMap, "project");

                final String url = urlParam.orElse(AccessibleSettings.master.baseUrl + ":" + settings.getJenkinsPort() + "/job/");

                List<String> onJenkins = new ArrayList<>();
                List<String> testProjects = new ArrayList<>();
                List<String> jdkProjects = new ArrayList<>();

                if (allInJenkinsOpt.isPresent() || orphansOnJenkinsParam.isPresent() || orphansOnOtoolParam.isPresent()) {
                    try {
                        onJenkins = getAllJenkinsJobs();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

                if (allInOtoolOpt.isPresent() || testParam.isPresent() || orphansOnJenkinsParam.isPresent() || orphansOnOtoolParam.isPresent()) {
                    testProjects = getAllJdkTestJobs(settings, jdkTestProjectManager, projectParam);
                }

                if (allInOtoolOpt.isPresent() || jdkParam.isPresent() || orphansOnJenkinsParam.isPresent() || orphansOnOtoolParam.isPresent()) {
                    jdkProjects = getAllJdkJobs(settings, jdkProjectManager, projectParam);
                }

                if (excludeParam.isPresent()) {
                    onJenkins = onJenkins.stream().filter(new BlacklistPredicate(excludeParam.get())).collect(Collectors.toList());
                    jdkProjects = jdkProjects.stream().filter(new BlacklistPredicate(excludeParam.get())).collect(Collectors.toList());
                    testProjects = testProjects.stream().filter(new BlacklistPredicate(excludeParam.get())).collect(Collectors.toList());
                }

                if (includeParam.isPresent()) {
                    onJenkins = onJenkins.stream().filter(new BlacklistPredicate(includeParam.get()).negate()).collect(Collectors.toList());
                    jdkProjects = jdkProjects.stream().filter(new BlacklistPredicate(includeParam.get()).negate()).collect(Collectors.toList());
                    testProjects = testProjects.stream().filter(new BlacklistPredicate(includeParam.get()).negate()).collect(Collectors.toList());
                }

                if (allInJenkinsOpt.isPresent()) {
                    return Result.ok(String.join("\n", onJenkins.stream().map(c -> url + c).collect(Collectors.toList())) + "\n");
                }
                if (jdkParam.isPresent()) {
                    return Result.ok(String.join("\n", jdkProjects.stream().map(c -> url + c).collect(Collectors.toList())) + "\n");
                }
                if (testParam.isPresent()) {
                    return Result.ok(String.join("\n", testProjects.stream().map(c -> url + c).collect(Collectors.toList())) + "\n");
                }
                if (allInOtoolOpt.isPresent()) {
                    return Result.ok(String.join("\n", Stream.concat(testProjects.stream(), jdkProjects.stream()).map(c -> url + c).collect(Collectors.toList())) + "\n");
                }
                if (orphansOnJenkinsParam.isPresent()) {
                    return Result.ok(String.join("\n", Stream.concat(testProjects.stream(), jdkProjects.stream()).filter(new RemoveIfFound(onJenkins)).collect(Collectors.toList())) + "\n");
                }
                if (orphansOnOtoolParam.isPresent()) {
                    return Result.ok(String.join("\n", onJenkins.stream().filter(new RemoveIfFound(Stream.concat(testProjects.stream(), jdkProjects.stream()).collect(Collectors.toList()))).collect(Collectors.toList())) + "\n");
                }
                return Result.err("Wrong/missing parameters");
            }

            @Override
            public String about() {
                return "/jobs?[one of:[" + String.join(
                        "|",
                        "allJenkins /*all jobs on jenkins*/", //all on jenkins
                        "orphansJenkins /*jobs missing on jenkins*/", //ison jenkins, not in otool
                        "orphansOtool /*jobs redundant on jekins*/", //is on otool, not in jenkins
                        "allOtool /*all jobs possible by curent setup of jenkins*/", //all otooled (comb of two below)
                        "jdkTestProjects",
                        "jdkProjects] + one times optionalls[",
                        "URL=<prefix>",
                        "exclude=<regex1>,<regex2>...",
                        "include=<regex1>,<regex2>...",
                        "project=<projectName>]"
                ) + "]";
            }
        };
    }

    private static boolean checkProjectName(String id, Optional<String> projectParam) {
        if (projectParam.isPresent()) {
            return id.equals(projectParam.get());
        } else {
            return true;
        }
    }

    public static List<String> getAllJdkTestJobs(AccessibleSettings settings, JDKTestProjectManager jdkTestProjectManager, Optional<String> projectFilter) throws StorageException, ManagementException {
        List<String> testProjects = new ArrayList<>();
        final JDKProjectParser jdkProjectParser = new JDKProjectParser(
                ConfigManager.create(settings.getConfigRoot().getAbsolutePath()),
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        for (final JDKTestProject jdkTestProject : jdkTestProjectManager.readAll()) {
            if (checkProjectName(jdkTestProject.getId(), projectFilter)) {
                Set<Job> testJobsSet = jdkProjectParser.parse(jdkTestProject);
                for (Job j : testJobsSet) {
                    testProjects.add(j.getName());
                }
            }
        }
        Collections.sort(testProjects);
        return testProjects;
    }

    public static List<String> getAllJdkJobs(AccessibleSettings settings, JDKProjectManager jdkProjectManager, Optional<String> projectFilter) throws StorageException, ManagementException {
        List<String> jdkProjects = new ArrayList<>();
        final JDKProjectParser jdkProjectParser = new JDKProjectParser(
                ConfigManager.create(settings.getConfigRoot().getAbsolutePath()),
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        for (final JDKProject jdkProject : jdkProjectManager.readAll()) {
            if (checkProjectName(jdkProject.getId(), projectFilter)) {
                Set<Job> tr = jdkProjectParser.parse(jdkProject);
                for (Job j : tr) {
                    jdkProjects.add(j.getName());
                }
            }
        }
        Collections.sort(jdkProjects);
        return jdkProjects;
    }


    public static List<String> getAllJenkinsJobs() throws Exception {
        try {
            List<String> onJenkins = new ArrayList<>();
            String[] tr = JenkinsCliWrapper.getCli().listJobsToArray();
            for (String s : tr) {
                onJenkins.add(s);
            }
            Collections.sort(onJenkins);
            return onJenkins;
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    private QueryHandler getJDKVersionHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> paramsMap) throws StorageException, ManagementException {
                Optional<String> productOpt = extractParamValue(paramsMap, PRODUCT);
                if (productOpt.isPresent()) {
                    final String product = productOpt.get();
                    final List<JDKVersion> jdkVersions = jdkVersionManager.readAll();
                    final Optional<JDKVersion> optionalJDKVersion = jdkVersions.stream()
                            .filter(jdkVersion -> jdkVersion.getPackageNames().contains(product))
                            .findFirst();
                    if (optionalJDKVersion.isPresent()) {
                        final JDKVersion jdkVersion = optionalJDKVersion.get();
                        return Result.ok(jdkVersion.getId());
                    }
                }
                final Optional<String> projectOpt = extractParamValue(paramsMap, PROJECT);
                if (projectOpt.isPresent()) {
                    final String projectName = projectOpt.get();
                    final Project project;
                    if (jdkProjectManager.contains(projectName)) {
                        project = jdkProjectManager.read(projectName);
                    } else if (jdkTestProjectManager.contains(projectName)) {
                        project = jdkTestProjectManager.read(projectName);
                    } else {
                        return Result.err("Project " + projectName + " doesn't exist");
                    }
                    return Result.ok(project.getProduct().getJdk());
                }
                return Result.err("Wrong/missing parameters");
            }

            @Override
            public String about() {
                return "/jdkVersion?[" + String.join(
                        "|",
                        PRODUCT + "=<packageName>",
                        PROJECT + "=<projectName>"
                ) + "]";
            }
        };
    }

    private QueryHandler getJDKVersionsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> paramsMap) throws StorageException {
                List<String> jdkVersions = jdkVersionManager.readAll()
                        .stream()
                        .map(JDKVersion::getId)
                        .sorted(String::compareTo)
                        .collect(Collectors.toList());
                return Result.ok(String.join("\n", jdkVersions));
            }

            @Override
            public String about() {
                return "/jdkVersions";
            }
        };
    }

    private QueryHandler getPortHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> paramsMap) {
                final Optional<String> serviceOpt = extractParamValue(paramsMap, SERVICE);
                if (serviceOpt.isPresent()) {
                    final String service = serviceOpt.get();
                    final int port;
                    switch (service) {
                        case XML_RPC:
                            port = settings.getXmlRpcPort();
                            break;
                        case FILE_DOWNLOAD:
                            port = settings.getFileDownloadPort();
                            break;
                        case SSH:
                            port = settings.getSshPort();
                            break;
                        case WEBAPP:
                            port = settings.getWebappPort();
                            break;
                        default:
                            return Result.err("Unknown service: " + service);
                    }
                    return Result.ok(String.valueOf(port));
                }
                return Result.err(ERROR_PARAMETERS_EXPECTED);
            }

            @Override
            public String about() {
                return "/port?service=[" + String.join(
                        "|",
                        SSH,
                        XML_RPC,
                        WEBAPP,
                        FILE_DOWNLOAD
                ) + "]";
            }
        };
    }

    private QueryHandler getProductHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> archiveOpt = extractParamValue(queryParams, ARCHIVE);
                if (archiveOpt.isPresent()) {
                    final String archive = archiveOpt.get();
                    return new OToolParser(
                            jdkProjectManager.readAll(),
                            jdkVersionManager.readAll(),
                            taskVariantManager.getBuildVariants()
                    )
                            .parseArchive(archive)
                            .map(OToolBuild::getPackageName);
                }
                final Optional<String> projectOpt = extractParamValue(queryParams, PROJECT);
                if (projectOpt.isPresent()) {
                    final String project = projectOpt.get();
                    final List<JDKProject> jdkProjects = jdkProjectManager.readAll();
                    final Optional<JDKProject> optionalJDKProject = jdkProjects.stream()
                            .filter(proj -> proj.getId().equals(project)).findFirst();
                    if (optionalJDKProject.isPresent()) {
                        final JDKProject jdkProject = optionalJDKProject.get();
                        return Result.ok(jdkProject.getProduct().getPackageName());

                    }
                    final List<JDKTestProject> jdkTestProjects = jdkTestProjectManager.readAll();
                    final Optional<JDKTestProject> optionalJDKTestProject = jdkTestProjects.stream()
                            .filter(proj -> proj.getId().equals(project)).findFirst();
                    if (optionalJDKTestProject.isPresent()) {
                        final JDKTestProject jdkTestProject = optionalJDKTestProject.get();
                        return Result.ok(jdkTestProject.getProduct().getPackageName());
                    }
                    return Result.err(ERROR_PROJECT_NOT_FOUND);
                }
                final Optional<String> buildOpt = extractParamValue(queryParams, BUILD);
                if (buildOpt.isPresent()) {
                    final String build = buildOpt.get();
                    return new OToolParser(
                            jdkProjectManager.readAll(),
                            jdkVersionManager.readAll(),
                            taskVariantManager.getBuildVariants()
                    )
                            .parseBuild(build)
                            .map(OToolBuild::getPackageName);
                }
                return Result.err(ERROR_PARAMETERS_EXPECTED);
            }

            @Override
            public String about() {
                return "/product?[" + String.join(
                        "|",
                        ARCHIVE + "=<NVRA>",
                        BUILD + "=<NVR>",
                        PROJECT + "=projectName"
                ) + "]";
            }
        };
    }

    private QueryHandler getProductsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final List<String> allProducts = jdkVersionManager.readAll()
                        .stream()
                        .flatMap(jdkVersion -> jdkVersion.getPackageNames().stream())
                        .sorted(String::compareTo)
                        .collect(Collectors.toList());
                return Result.ok(String.join("\n", allProducts));
            }

            @Override
            public String about() {
                return "/products";
            }
        };
    }

    private QueryHandler getProjectHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> archiveOpt = extractParamValue(queryParams, ARCHIVE);
                final Optional<String> buildOpt = extractParamValue(queryParams, BUILD);
                final OToolParser parser = new OToolParser(
                        jdkProjectManager.readAll(),
                        jdkVersionManager.readAll(),
                        taskVariantManager.getBuildVariants()
                );

                if (archiveOpt.isPresent()) {
                    final String archive = archiveOpt.get();
                    return parser.parseArchive(archive).map(OToolArchive::getProjectName);
                }

                if (buildOpt.isPresent()) {
                    final String build = buildOpt.get();
                    return parser.parseBuild(build).map(OToolBuild::getProjectName);
                }
                return Result.err(ERROR_PARAMETERS_EXPECTED);
            }

            @Override
            public String about() {
                return "/project?[" + String.join(
                        "|",
                        ARCHIVE + "=<NVRA>",
                        BUILD + "=<NVR>"
                ) + "]";
            }
        };
    }

    private QueryHandler getFileNameParserHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> archiveOpt = extractParamValue(queryParams, ARCHIVE);
                final Optional<String> buildOpt = extractParamValue(queryParams, BUILD);
                final OToolParser parser = new OToolParser(
                        jdkProjectManager.readAll(),
                        jdkVersionManager.readAll(),
                        taskVariantManager.getBuildVariants()
                );

                if (archiveOpt.isPresent()) {
                    final String archive = archiveOpt.get();
                    Result<String, String> r = parser.parseArchive(archive).map(oToolArchive -> oToolArchive.toString("\n"));
                    return r;
                }

                if (buildOpt.isPresent()) {
                    final String build = buildOpt.get();
                    Result<String, String> r = parser.parseBuild(build).map(oToolBuild -> oToolBuild.toString("\n"));
                    return r;
                }
                return Result.err(ERROR_PARAMETERS_EXPECTED);
            }

            @Override
            public String about() {
                return "/filenameParser?[" + String.join(
                        "|",
                        ARCHIVE + "=<NVRA>",
                        BUILD + "=<NVR>"
                ) + "]";
            }
        };
    }

    private QueryHandler getProjectsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> typeOpt = extractParamValue(queryParams, TYPE);
                final Optional<String> nvrOpt = extractParamValue(queryParams, NVR);
                final Optional<String> rulesOpt = extractParamValue(queryParams, ADDITIONAL_RULES);
                final List<String[]> rulesPairList;
                if (rulesOpt.isPresent()) {
                    String[] rules = rulesOpt.get().split("[\\s,]+");
                    if (rules.length == 0 || rules.length % 2 == 1) {
                        return Result.err("if " + ADDITIONAL_RULES + " is used, then it must be >0 and mus be even number of them. You have " + rules.length);
                    }
                    rulesPairList = new ArrayList<>(rules.length / 2);
                    for (int i = 0; i < rules.length; i += 2) {
                        rulesPairList.add(new String[]{rules[i], rules[i + 1]});
                    }
                } else {
                    rulesPairList = Collections.emptyList();
                }
                final String asRegex = extractParamValue(queryParams, AS_REGEX_LIST).orElse(null);
                final boolean unsafe = Boolean.valueOf(extractParamValue(queryParams, UNSAFE).orElse("false"));
                final String prep;
                final String join;
                final String post;
                if ("regex".equals(asRegex)) {
                    prep = ".*-";
                    join = "-.*|.*-";
                    post = "-.*";
                } else if ("list".equals(asRegex)) {
                    prep = "";
                    join = ",";
                    post = "";
                } else {
                    prep = "";
                    join = "\n";
                    post = "\n";
                }
                if (typeOpt.isPresent()) {
                    final String type = typeOpt.get();
                    final Stream<Project> projects;
                    switch (Project.ProjectType.valueOf(type)) {
                        case JDK_PROJECT:
                            projects = jdkProjectManager.readAll()
                                    .stream()
                                    .map(project -> project);
                            break;
                        case JDK_TEST_PROJECT:
                            projects = jdkTestProjectManager.readAll()
                                    .stream()
                                    .map(project -> project);
                            break;
                        default:
                            return Result.err("Unknown project type");
                    }
                    return Result.ok(prep + projects
                            .map(Project::getId)
                            .sorted(String::compareTo)
                            .collect(Collectors.joining(join)) + post);
                } else if (nvrOpt.isPresent()) {
                    final OToolParser parser = new OToolParser(
                            jdkProjectManager.readAll(),
                            jdkVersionManager.readAll(),
                            taskVariantManager.getBuildVariants()
                    );
                    final String archive = nvrOpt.get();
                    Result r;
                    try {
                        r = parser.parseArchive(archive).map(oToolArchive -> prep + oToolArchive.getProjectName() + post);
                    } catch (Exception ex) {
                        //ex.printStackTrace(); we do not care, falling back to
                        r = Result.err("Not and parse-able project");
                    }
                    if (r.isOk()) {
                        return r;
                    } else {
                        //ok, it is not jdkProject, so it must be jdkTestProject
                        String nv = archive.substring(0, archive.lastIndexOf("-"));
                        String n = nv.substring(0, nv.lastIndexOf("-"));
                        String vr = archive.replaceFirst(n + "-", "");
                        Pattern tmpRuleMatcher = Pattern.compile(".*");
                        List<JDKTestProject> testProjects = jdkTestProjectManager.readAll();
                        for (String[] rule : rulesPairList) {
                            if (vr.matches(rule[0])) {
                                tmpRuleMatcher = Pattern.compile(rule[1]);
                                break;
                            }
                        }
                        final Pattern ruleMatcher = tmpRuleMatcher;
                        List<String> results = testProjects.stream()
                                .filter(testproject -> testproject.getProduct().getPackageName().equals(n))
                                .filter(testproject -> ruleMatcher.matcher(testproject.getId()).matches())
                                .map(testproject -> testproject.getId())
                                .sorted()
                                .collect(Collectors.toList());
                        if (results.isEmpty()) {
                            if (unsafe) {
                                results = jdkTestProjectManager.readAll()
                                        .stream()
                                        .filter(testproject -> ruleMatcher.matcher(testproject.getId()).matches())
                                        .map(Project::getId)
                                        .sorted(String::compareTo)
                                        .collect(Collectors.toList());
                                if (results.isEmpty()) {
                                    //without rules
                                    results = jdkTestProjectManager.readAll()
                                            .stream()
                                            .map(Project::getId)
                                            .sorted(String::compareTo)
                                            .collect(Collectors.toList());

                                }
                                return Result.ok(prep + String.join(join, results) + post);
                            } else {
                                return Result.err("");
                            }
                        } else {
                            return Result.ok(prep + String.join(join, results) + post);
                        }
                    }
                } else {
                    final Stream<Project> projects = Stream.of(
                            jdkProjectManager.readAll(),
                            jdkTestProjectManager.readAll()
                    ).flatMap(List::stream);
                    Optional<String> productOpt = extractParamValue(queryParams, PRODUCT);
                    if (productOpt.isPresent()) {
                        final String product = productOpt.get();
                        return Result.ok(prep + projects
                                .filter(project -> project.getProduct().getPackageName().equals(product))
                                .map(Project::getId)
                                .collect(Collectors.joining(join)) + post);
                    }
                    return Result.ok(prep + projects.map(Project::getId)
                            .sorted(String::compareTo)
                            .collect(Collectors.joining(join)) + post);
                }
            }

            @Override
            public String about() {
                return "/projects?[ one of \n" +
                        "\t" + TYPE + "=[" + Project.ProjectType.JDK_PROJECT + "|" + Project.ProjectType.JDK_TEST_PROJECT + "]\n" +
                        "\t" + NVR + "=nvr with optional as=<regex|list> and usnafe=true (will return all testOnly on failure)\n" +
                        "\t" + ADDITIONAL_RULES + "=space or comma sepparated list of pairs eg Pa1 Pb1,Pa2 Pb2,...PaN PBn \n" +
                        "\t" + "each pair is if VR matches Pa then use Pb matching projects. Firs matched, first served \n" +
                        "]";
            }
        };
    }

    private QueryHandler getPathHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) {
                final Optional<String> rootOpt = extractParamValue(queryParams, ROOT);
                if (rootOpt.isPresent()) {
                    final String root = rootOpt.get();
                    final String rootPath;
                    switch (root) {
                        case BUILDS:
                            rootPath = settings.getDbFileRoot().getAbsolutePath();
                            break;
                        case CONFIGS:
                            rootPath = settings.getConfigRoot().getAbsolutePath();
                            break;
                        case JENKINS_JOBS:
                            rootPath = settings.getJenkinsJobsRoot().getAbsolutePath();
                            break;
                        case JENKINS_JOB_ARCHIVE:
                            rootPath = settings.getJenkinsJobArchiveRoot().getAbsolutePath();
                            break;
                        case REPOS:
                            rootPath = settings.getLocalReposRoot().getAbsolutePath();
                            break;
                        default:
                            return Result.err("Unknown root: " + root);
                    }
                    return Result.ok(rootPath);
                }
                return Result.err(ERROR_PARAMETERS_EXPECTED);
            }

            @Override
            public String about() {
                return "/path?root=[" + String.join("|",
                        BUILDS, CONFIGS, JENKINS_JOBS, JENKINS_JOB_ARCHIVE, REPOS
                ) + "]";
            }
        };
    }

    private QueryHandler getPlatformDetailsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> id = extractParamValue(queryParams, PLATFORM_ID);
                List<Platform> platforms = platformManager.readAll();
                String kojiArches = platforms.stream()
                        .filter(platform -> {
                            if (id.isPresent()) {
                                return platform.getId().equals(id.get());
                            } else {
                                return true;
                            }
                        })
                        .map(platform -> platform.toString("\n"))
                        .sorted()
                        .collect(Collectors.joining("\n"));
                return Result.ok(kojiArches + "\n");
            }

            @Override
            public String about() {
                return "/platformDetails?id=optionalSelecto";
            }
        };
    }

    private QueryHandler getTasksHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                List<Task> tasks = taskManager.readAll();
                String kojiArches = tasks.stream()
                        .map(task -> task.getId())
                        .sorted()
                        .collect(Collectors.joining("\n"));
                return Result.ok(kojiArches + "\n");
            }

            @Override
            public String about() {
                return "/tasks";
            }
        };
    }

    private QueryHandler getPlatformsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                List<Platform> platforms = platformManager.readAll();
                String kojiArches = platforms.stream()
                        .map(platform -> platform.getId())
                        .sorted()
                        .collect(Collectors.joining("\n"));
                return Result.ok(kojiArches + "\n");
            }

            @Override
            public String about() {
                return "/platforms";
            }
        };
    }

    private QueryHandler getKojiArchesHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                List<Platform> platforms = platformManager.readAll();
                String kojiArches = platforms.stream()
                        .map(platform -> platform.getKojiArch().orElse(platform.getArchitecture()))
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining("\n"));

                return Result.ok(kojiArches + "\n");
            }

            @Override
            public String about() {
                return "/kojiArches";
            }
        };
    }


    private QueryHandler getLegacyParserHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> nvr = extractParamValue(queryParams, NVR);
                if (!nvr.isPresent()) {
                    return Result.err(NVR + "=nvr expected");
                }
                final Optional<String> type = extractParamValue(queryParams, TYPE);
                if (!type.isPresent() || "NVR".equals(type.get())) {
                    return Result.ok(nvr.get());
                }
                String nv = nvr.get().substring(0, nvr.get().lastIndexOf("-"));
                String n = nv.substring(0, nv.lastIndexOf("-"));
                String[] vr = nvr.get().replaceFirst(n + "-", "").split("-");
                String[] splitted = new String[]{n, vr[0], vr[1]};
                switch (type.get()) {
                    case "N":
                        return Result.ok(splitted[0]);
                    case "V":
                        return Result.ok(splitted[1]);
                    case "R":
                        return Result.ok(splitted[2]);
                    case "NV":
                        return Result.ok(splitted[0] + "-" + splitted[1]);
                    case "VR":
                        return Result.ok(splitted[1] + "-" + splitted[2]);
                    case "NR":
                        return Result.ok(splitted[0] + "-" + splitted[2]);
                    default:
                        return Result.err("Unknown type - " + type.get());
                }
            }

            @Override
            public String about() {
                return "/" + LEGACY_PARSER + " [" + NVR + "=<nvr> + optional " + TYPE + "=<Nand/orVand/orR]";
            }
        };
    }

    private QueryHandler getBuildsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws IOException {
                final Optional<String> type = extractParamValue(queryParams, "type");
                final boolean includeData = Boolean.parseBoolean(extractParamValue(queryParams, "includeData").orElse("false"));
                Set<String> results = new HashSet<>();
                Files.walkFileTree(settings.getDbFileRoot().toPath(), new FileVisitor<Path>() {
                    private String relativize(Path now) {
                        String relativeFile = now.toFile().getAbsolutePath().replace(settings.getDbFileRoot().getAbsolutePath(), "");
                        while ((relativeFile.startsWith("/"))) {
                            relativeFile = relativeFile.substring(1);
                        }
                        return relativeFile;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (includeData) {
                            if (type.isPresent() && type.get().contains("dirs")) {
                                results.add(relativize(dir));
                            }
                            return FileVisitResult.CONTINUE;
                        } else {
                            if (dir.toFile().getName().equals("data")) {
                                return FileVisitResult.SKIP_SUBTREE;
                            } else {
                                if (type.isPresent() && type.get().contains("dirs")) {
                                    results.add(relativize(dir));
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!type.isPresent() || type.get().contains("filenames")) {
                            results.add(file.toFile().getName());
                        } else if (type.get().contains("files")) {
                            results.add(relativize(file));
                        } else if (type.get().contains("nvras")) {
                            String name = file.toFile().getName();
                            int dot = name.lastIndexOf(".");
                            if (dot > 0) {
                                name = name.substring(0, dot);
                                results.add(name);
                            }
                        } else if (type.get().contains("nvrs")) {
                            String name = file.toFile().getName();
                            int dot = name.lastIndexOf(".");
                            if (dot > 0) {
                                name = name.substring(0, dot);
                                dot = name.lastIndexOf(".");
                                if (dot > 0) {
                                    name = name.substring(0, dot);
                                    dot = name.lastIndexOf(".");
                                    if (dot > 0) {
                                        name = name.substring(0, dot);
                                        results.add(name);
                                    }
                                }
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
                List<String> r = new ArrayList<>();
                r.addAll(results);
                String rs = r.stream().sorted()
                        .collect(Collectors.joining("\n"));
                return Result.ok(rs + "\n");
            }

            @Override
            public String about() {
                return "/builds?type={filenames,files,dirs,nvras,nvrs}&includeData=true";
            }
        };
    }

    private Map<String, QueryHandler> getHandlers() {
        return Collections.unmodifiableMap(new HashMap<String, QueryHandler>() {{
            put(JOBS, getJobsHandler());
            put(JDK_VERSION, getJDKVersionHandler());
            put(JDK_VERSIONS, getJDKVersionsHandler());
            put(PORT, getPortHandler());
            put(PRODUCTS, getProductsHandler());
            put(PRODUCT, getProductHandler());
            put(PROJECTS, getProjectsHandler());
            put(PROJECT, getProjectHandler());
            put(PATH, getPathHandler());
            put(PLATFORMS_DETAILS, getPlatformDetailsHandler());
            put(PLATFORMS, getPlatformsHandler());
            put(KOJI_ARCHES, getKojiArchesHandler());
            put(BUILDS, getBuildsHandler());
            put(FILENAME_PARSER, getFileNameParserHandler());
            put(TASKS, getTasksHandler());
            put(LEGACY_PARSER, getLegacyParserHandler());
        }});
    }

    @Override
    public void addEndpoints() {
        final Map<String, QueryHandler> handlers = getHandlers();
        handlers.forEach((endpoint, handler) -> path(endpoint, () -> get(context -> {
            final Result<String, String> result = handler.handle(context.queryParamMap());
            if (result.isError()) {
                final String error = result.getError();
                context.result(error).status(400);
            } else {
                context.result(result.getValue());
            }
        })));
        get(HELP, context -> {
            final String help = handlers.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> entry.getValue().about())
                    .collect(Collectors.joining("\n\n"));
            context.result(help + "\n");
        });
    }

    private interface QueryHandler {

        Result<String, String> handle(
                Map<String, List<String>> queryParams
        ) throws StorageException, ManagementException, IOException;

        String about();
    }

    private class BlacklistPredicate implements Predicate<String> {
        List<Pattern> patterns = new ArrayList<>();

        public BlacklistPredicate(String s) {
            String[] q = s.split(",");
            for (String regex : q) {
                patterns.add(Pattern.compile(regex));
            }
        }

        @Override
        public boolean test(String o) {
            for (Pattern p : patterns) {
                if (p.matcher(o).matches()) {
                    return false;
                }
            }
            return true;
        }
    }

    private class RemoveIfFound implements Predicate<String> {
        private final List<String> anotherList;

        public RemoveIfFound(List<String> anotherList) {
            this.anotherList = anotherList;
        }

        @Override
        public boolean test(String o) {
            for (String s : anotherList) {
                if (Objects.equals(o, s)) {
                    return false;
                }
            }
            return true;
        }
    }
}
