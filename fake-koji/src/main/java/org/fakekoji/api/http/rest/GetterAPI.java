package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.storage.StorageException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String JOBS = "jobs"; // TODO
    private static final String PATH = "path";
    private static final String PORT = "port";
    private static final String PORTS = "ports"; // TODO
    private static final String PRODUCT = "product";
    private static final String PRODUCTS = "products";
    private static final String PROJECT = "project";
    private static final String PROJECTS = "projects";
    private static final String REPOS = "repos";
    private static final String ROOT = "root";
    private static final String ROOTS = "roots"; // TODO
    private static final String SERVICE = "service";
    private static final String SSH = "ssh";
    private static final String TYPE = "type";
    private static final String WEBAPP = "webapp";
    private static final String XML_RPC = "xmlRpc";

    private static final String JDK_VERSIONS = "jdkVersions";
    private static final String HELP = "help";

    private final AccessibleSettings settings;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;

    public GetterAPI(
            final AccessibleSettings settings,
            final JDKProjectManager jdkProjectManager,
            final JDKTestProjectManager jdkTestProjectManager,
            final JDKVersionManager jdkVersionManager,
            final TaskVariantManager taskVariantManager
    ) {
        this.settings = settings;
        this.jdkProjectManager = jdkProjectManager;
        this.jdkTestProjectManager = jdkTestProjectManager;
        this.jdkVersionManager = jdkVersionManager;
        this.taskVariantManager = taskVariantManager;
    }

    private Optional<String> extractParamValue(Map<String, List<String>> paramsMap, String param) {
        return Optional.ofNullable(paramsMap.get(param))
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0));
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

    private QueryHandler getProjectsHandler() {
        return new QueryHandler() {
            @Override
            public Result<String, String> handle(Map<String, List<String>> queryParams) throws StorageException {
                final Optional<String> typeOpt = extractParamValue(queryParams, TYPE);
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
                    return Result.ok(projects
                            .map(Project::getId)
                            .sorted(String::compareTo)
                            .collect(Collectors.joining("\n"))
                    );
                }
                final Stream<Project> projects = Stream.of(
                        jdkProjectManager.readAll(),
                        jdkTestProjectManager.readAll()
                ).flatMap(List::stream);
                Optional<String> productOpt = extractParamValue(queryParams, PRODUCT);
                if (productOpt.isPresent()) {
                    final String product = productOpt.get();
                    return Result.ok(projects
                            .filter(project -> project.getProduct().getPackageName().equals(product))
                            .map(Project::getId)
                            .collect(Collectors.joining("\n")));
                }
                return Result.ok(projects.map(Project::getId)
                        .sorted(String::compareTo)
                        .collect(Collectors.joining("\n")));
            }

            @Override
            public String about() {
                return "/projects?[" + String.join("|",
                        TYPE + "=[" + Project.ProjectType.JDK_PROJECT + "|"
                                + Project.ProjectType.JDK_TEST_PROJECT + "]") + "]";
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
                return "/paths?root=[" + String.join("|",
                        BUILDS, CONFIGS, JENKINS_JOBS, JENKINS_JOB_ARCHIVE, REPOS
                        ) + "]";
            }
        };
    }

    private Map<String, QueryHandler> getHandlers() {
        return Collections.unmodifiableMap(new HashMap<String, QueryHandler>() {{
            put(JDK_VERSION, getJDKVersionHandler());
            put(JDK_VERSIONS, getJDKVersionsHandler());
            put(PORT, getPortHandler());
            put(PRODUCTS, getProductsHandler());
            put(PRODUCT, getProductHandler());
            put(PROJECTS, getProjectsHandler());
            put(PROJECT, getProjectHandler());
            put(PATH, getPathHandler());
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
            context.result(help);
        });
    }

    private interface QueryHandler {

        Result<String, String> handle(
                Map<String, List<String>> queryParams
        ) throws StorageException, ManagementException;

        String about();
    }
}
