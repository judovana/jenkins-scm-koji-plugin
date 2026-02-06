package org.fakekoji.api.http.rest;

import static io.javalin.apibuilder.ApiBuilder.path;

import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.BuildEqualityFilter;
import org.fakekoji.core.utils.matrix.MatrixGenerator;
import org.fakekoji.core.utils.matrix.SummaryReportRunner;
import org.fakekoji.core.utils.matrix.TestEqualityFilter;
import org.fakekoji.core.utils.matrix.formatter.HtmlAjaxFormatter;
import org.fakekoji.core.utils.matrix.formatter.HtmlFormatter;
import org.fakekoji.core.utils.matrix.formatter.HtmlSpanningFillingFormatter;
import org.fakekoji.core.utils.matrix.formatter.HtmlSpanningFormatter;
import org.fakekoji.core.utils.matrix.formatter.PlainTextFormatter;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsUpdateVmTemplateBuilder;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.manager.BuildProviderManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.jobmanager.views.JenkinsViewTemplateBuilder;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;

class KojiEndpointGroup implements EndpointGroup {

    private static final String ID = "id";
    private static final String CONFIG_ID = "/{" + ID + "}";
    private static final String BUILD_PROVIDERS = "/buildProviders";
    private static final String BUILD_PROVIDER = BUILD_PROVIDERS + CONFIG_ID;
    private static final String JDK_VERSIONS = "/jdkVersions";
    private static final String JDK_VERSION = JDK_VERSIONS + CONFIG_ID;
    static final String PLATFORMS = "/platforms";
    private static final String PLATFORM = PLATFORMS + CONFIG_ID;
    private static final String TASKS = "/tasks";
    private static final String TASK = TASKS + CONFIG_ID;
    private static final String TASK_VARIANTS = "/taskVariants";
    private static final String TASK_VARIANT = TASK_VARIANTS + CONFIG_ID;
    private static final String JDK_PROJECTS = "/jdkProjects";
    private static final String JDK_PROJECT = JDK_PROJECTS + CONFIG_ID;
    private static final String JDK_TEST_PROJECTS = "/jdkTestProjects";
    private static final String JDK_TEST_PROJECT = JDK_TEST_PROJECTS + CONFIG_ID;
    private static final String GET = "get";
    public static final String BUMP = "bump";
    public static final String RESULTS_DB = "resultsDb";
    public static final String MISC = "misc";
    private static final String HELP = "help";
    public static final String PRODUCTS = "/products";
    static final String FILTER = "filter";
    static final String SKIP_EMPTY = "skipEmpty";
    static final String NESTED = "nested";
    static final String NESTED_COLUMNS = "nestedColumns";
    static final String CUSTOM_COLUMNS = "customColumns";
    static final String ONLY_HW = "onlyHW";
    static final String ONLY_VM = "onlyVM";
    private static final String UPDATE_JOBS = "updateJobs";
    private static final String UPDATE_JOBS_LIST = "list";
    private static final String UPDATE_JOBS_XMLS = "xmls";
    private static final String UPDATE_JOBS_CREATE = "create";
    private static final String UPDATE_JOBS_REMOVE = "remove";
    private static final String UPDATE_JOBS_UPDATE = "update";
    private static final String VIEWS = "views";
    private static final String VIEWS_LIST_OTOOL = "list";
    private static final String VIEWS_DETAILS = "details";
    private static final String VIEWS_MATCHES = "matches";
    private static final String VIEWS_MATCHES_JENKINS = "jenkins";
    private static final String VIEWS_XMLS = "xmls";
    private static final String VIEWS_CREATE = "create";
    private static final String VIEWS_REMOVE = "remove";
    private static final String VIEWS_UPDATE = "update";
    private static final String REGENERATE_ALL = "regenerateAll";
    private static final String MATRIX = "matrix";
    private static final String MATRIX_ORIENTATION = "orientation";
    private static final String MATRIX_BREGEX = "buildRegex";
    private static final String MATRIX_TREGEX = "testRegex";
    private static final String MATRIX_CREGEX = "cellRegex";
    private static final String MATRIX_FORMAT = "format";
    private static final String PROJECT = "project";

    private final AccessibleSettings mSettings;
    private final OToolService.OToolHandlerWrapper mWrapper;
    private final GetterAPI mGetterApi;
    private final JobUpdater mJenkinsJobUpdater;

    private String getMiscHelp() {
        return "" + MISC + "/" + REGENERATE_ALL + JDK_TEST_PROJECTS + "\n" + MISC + "/" + REGENERATE_ALL + JDK_PROJECTS + "\n" + "                mandatory arguments project=    to regenerate only single project     \n" + "                and allowlist=regex to limit the search even more  \n" + "\n" + MISC + "/" + UPDATE_JOBS + "/{" + UPDATE_JOBS_LIST + "," + UPDATE_JOBS_XMLS + "," + UPDATE_JOBS_CREATE + "," + UPDATE_JOBS_REMOVE + "," + UPDATE_JOBS_UPDATE + "}\n" + "                will affect update machines jobs\n" + "                params: " + FILTER + "=regex " + ONLY_HW + "=bool " + ONLY_VM + "=bool\n" + "\n" + MISC + "/" + VIEWS + "/{" + VIEWS_LIST_OTOOL + "," + VIEWS_DETAILS + "," + VIEWS_XMLS + "," + VIEWS_CREATE + "," + VIEWS_REMOVE + "," + VIEWS_UPDATE + "," + VIEWS_MATCHES + "," + VIEWS_MATCHES_JENKINS + "}\n" + "                will affect jenkins views\n" + "                params: " + FILTER + "=regex " + SKIP_EMPTY + "=true/false " + NESTED + "=true/false "
                + NESTED_COLUMNS + "=true/false/number  if number is put, then the default columns(true) are generated for views withh less then number of matches\n" + "                similar param is e " + CUSTOM_COLUMNS + "=true/false/number  if number is put, then the CUSTOM columns(true) are generated for list-views with less then number of jobs\n" + "                Note, nested=true requires jenkins nested-view-plugin. Warning, in nested-views, filter matches only top level component (tasks, paltforms..)\n" + "\n" + MISC + "/" + MATRIX + "\n" + "  where parameters for matrix are (with defaults):\n" + "  " + MATRIX_ORIENTATION + "=1 " + MATRIX_BREGEX + "=.* " + MATRIX_TREGEX + "=.* " + MATRIX_CREGEX + "=.* " + MATRIX_FORMAT + "=baseajax/htmlspan/html/plain/fill (baseajax have optional nvr; fill requires vr=>nvr time=number alsoReport=false and optional chartDir=path>\n" + "  "
                + "tos=true tarch=true tprovider=false tsuite=true tvars=false bos=true barch=true bprovider=false bproject=true bjdk=true bvars=false\n" + "  dropRows=true dropColumns=true  project=p1,p2,...,pn /*to generate matrix only for given projects*/\n" + "                                                                                                    WARNING! chartDir is directory on SERVER and is deleted if exists!/\n" + "  names=true will chage numbers to somehow more describing texts in html formatters. With huge squezing can go wil. Is sometimes buggy with platforms\n" + "  explicitcomparsion=url by default null, unused, to load remote file with listed lates released NVRs to comapre with\n" + "  baseajax have few runtime switches: automanFilter=index sort=index readOnly=[true(visible)/false(hidden)/total]. Defaults are 0,0,false\n";
    }

    public KojiEndpointGroup(AccessibleSettings settings, OToolService.OToolHandlerWrapper wrapper, GetterAPI getterApi, JobUpdater jenkinsJobUpdater) {
        mSettings = settings;
        mWrapper = wrapper;
        mGetterApi = getterApi;
        mJenkinsJobUpdater = jenkinsJobUpdater;
    }

    @Override
    public void addEndpoints() {
        final ConfigManager configManager = mSettings.getConfigManager();
        final JDKTestProjectManager jdkTestProjectManager = configManager.jdkTestProjectManager;
        final JDKProjectManager jdkProjectManager = configManager.jdkProjectManager;
        final PlatformManager platformManager = configManager.platformManager;
        final TaskManager taskManager = configManager.taskManager;
        final JDKVersionManager jdkVersionManager = configManager.jdkVersionManager;
        final BuildProviderManager buildProviderManager = configManager.buildProviderManager;
        final TaskVariantManager taskVariantManager = configManager.taskVariantManager;

        path(MISC, () -> {
            ApiBuilder.get(HELP, mWrapper.wrap(context -> {
                context.status(OToolService.OK).result(getMiscHelp() + BumperAPI.getHelp() + ResultsDb.getHelp() + RedeployApi.getHelp() + CancelApi.getHelp() + PriorityApi.getHelp() + DuplicateCoverageApi.getHelp() + "\n Shared filter:\n" + RedeployApiWorkerBase.getHelp());
            }));
            path(RESULTS_DB, new ResultsDb(mSettings));
            path(BUMP, new BumperAPI(mSettings));
            path(RedeployApi.REDEPLOY, new RedeployApi(mSettings));
            path(CancelApi.NO, new CancelApi(mSettings));
            path(PriorityApi.PRIORITY, new PriorityApi(mSettings));
            path(DuplicateCoverageApi.DUPLICATE, new DuplicateCoverageApi(mSettings));
            path(UPDATE_JOBS, () -> {
                ApiBuilder.get(UPDATE_JOBS_LIST, mWrapper.wrap(context -> {
                    UpdateVmsApi ua = new UpdateVmsApi(context);
                    List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(mSettings, platformManager);
                    context.status(OToolService.OK).result(ua.getList(allUpdates) + "\n");
                }));
                ApiBuilder.get(UPDATE_JOBS_XMLS, mWrapper.wrap(context -> {
                    UpdateVmsApi ua = new UpdateVmsApi(context);
                    List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(mSettings, platformManager);
                    context.status(OToolService.OK).result(ua.getXmls(allUpdates));
                }));
                ApiBuilder.get(UPDATE_JOBS_CREATE, mWrapper.wrap(context -> {
                    UpdateVmsApi ua = new UpdateVmsApi(context);
                    List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(mSettings, platformManager);
                    context.status(OToolService.OK).result(ua.create(allUpdates));
                }));
                ApiBuilder.get(UPDATE_JOBS_UPDATE, mWrapper.wrap(context -> {
                    UpdateVmsApi ua = new UpdateVmsApi(context);
                    List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(mSettings, platformManager);
                    context.status(OToolService.OK).result(ua.update(allUpdates));
                }));
                ApiBuilder.get(UPDATE_JOBS_REMOVE, mWrapper.wrap(context -> {
                    UpdateVmsApi ua = new UpdateVmsApi(context);
                    List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(mSettings, platformManager);
                    context.status(OToolService.OK).result(ua.remvoe(allUpdates));
                }));
            });
            path(VIEWS, () -> {
                ApiBuilder.get(VIEWS_CREATE, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    String results = va.create(jvt, jobs);
                    context.status(OToolService.OK).result(results);
                }));
                ApiBuilder.get(VIEWS_REMOVE, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    String results = va.delete(jvt, jobs);
                    context.status(OToolService.OK).result(results);
                }));
                ApiBuilder.get(VIEWS_UPDATE, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    String results = va.update(jvt, jobs);
                    context.status(OToolService.OK).result(results);
                }));
                ApiBuilder.get(VIEWS_LIST_OTOOL, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    if (va.isSkipEmpty()) {
                        String viewsAndMatchesToPrint = va.listNonEmpty(jvt, jobs);
                        context.status(OToolService.OK).result(viewsAndMatchesToPrint);
                    } else {
                        String viewsAndMatchesToPrint = va.list(jvt, Optional.empty(), false, false);
                        context.status(OToolService.OK).result(viewsAndMatchesToPrint);
                    }
                }));
                ApiBuilder.get(VIEWS_DETAILS, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    String details = va.getDetails(jvt, jobs);
                    context.status(OToolService.OK).result(details);
                }));
                ApiBuilder.get(VIEWS_XMLS, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    if (va.isSkipEmpty()) {
                        context.status(OToolService.OK).result(va.getNonEmptyXmls(jvt, jobs));
                    } else {
                        context.status(OToolService.OK).result(va.getXmls(jvt));
                    }
                }));
                ApiBuilder.get(VIEWS_MATCHES, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = mGetterApi.getAllOtoolJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    context.status(OToolService.OK).result(va.printMatches(jvt, jobs));
                }));
                ApiBuilder.get(VIEWS_MATCHES_JENKINS, mWrapper.wrap(context -> {
                    ViewsAppi va = new ViewsAppi(context);
                    List<String> jobs = GetterAPI.getAllJenkinsJobs();
                    List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                    Collections.sort(jobs);
                    context.status(OToolService.OK).result(va.printMatches(jvt, jobs));
                }));
            });
            path(REGENERATE_ALL, () -> {
                ApiBuilder.get(JDK_TEST_PROJECTS, mWrapper.wrap(context -> {
                    String project = context.queryParam("project");
                    String allowlist = context.queryParam("allowlist");
                    if (allowlist == null || project == null) {
                        context.status(OToolService.BAD).result("project and allowlist are mandatory. use ?allowlist=.*&project=ALL if yo are reall sure\n");
                    } else {
                        if (project.equals("ALL")) {
                            project = null;
                        }
                        JobUpdateResults r1 = mJenkinsJobUpdater.regenerateAll(project, jdkTestProjectManager, allowlist);
                        context.status(OToolService.OK).json(r1);
                    }
                }));
                ApiBuilder.get(JDK_PROJECTS, mWrapper.wrap(context -> {
                    String project = context.queryParam("project");
                    String allowlist = context.queryParam("allowlist");
                    if (allowlist == null || project == null) {
                        context.status(OToolService.BAD).result("project and allowlist are mandatory. use ?allowlist=.*&project=ALL if yo are reall sure\n");
                    } else {
                        if (project.equals("ALL")) {
                            project = null;
                        }
                        JobUpdateResults r2 = mJenkinsJobUpdater.regenerateAll(project, jdkProjectManager, allowlist);
                        context.status(OToolService.OK).json(r2);
                    }
                }));
            });
            ApiBuilder.get(MATRIX, mWrapper.wrap(context -> {
                String trex = context.queryParam(MATRIX_TREGEX);
                String brex = context.queryParam(MATRIX_BREGEX);
                String crex = context.queryParam(MATRIX_CREGEX);
                String format = context.queryParam(MATRIX_FORMAT);
                Boolean namesParam = OToolService.nullableBooleanObject(context, "names", null);
                boolean tos = OToolService.notNullBoolean(context, "tos", true);
                boolean tarch = OToolService.notNullBoolean(context, "tarch", true);
                boolean tprovider = OToolService.notNullBoolean(context, "tprovider", false);
                boolean tsuite = OToolService.notNullBoolean(context, "tsuite", true);
                boolean tvars = OToolService.notNullBoolean(context, "tvars", false);
                boolean bos = OToolService.notNullBoolean(context, "bos", true);
                boolean barch = OToolService.notNullBoolean(context, "barch", true);
                boolean bprovider = OToolService.notNullBoolean(context, "bprovider", false);
                boolean bproject = OToolService.notNullBoolean(context, "bproject", true);
                boolean bjdk = OToolService.notNullBoolean(context, "bjdk", true);
                boolean bvars = OToolService.notNullBoolean(context, "bvars", false);
                boolean dropRows = OToolService.notNullBoolean(context, "dropRows", true);
                boolean dropColumns = OToolService.notNullBoolean(context, "dropColumns", true);
                String project = context.queryParam(PROJECT);
                TestEqualityFilter tf = new TestEqualityFilter(tos, tarch, tprovider, tsuite, tvars);
                BuildEqualityFilter bf = new BuildEqualityFilter(bos, barch, bprovider, bproject, bjdk, bvars);
                String[] projects = project == null ? new String[0] : project.split(",");
                boolean names;
                if (namesParam == null) {
                    if (projects.length == 1) {
                        names = true;
                    } else {
                        names = false;
                    }
                } else {
                    names = namesParam;
                }
                MatrixGenerator m = new MatrixGenerator(mSettings, trex, brex, crex, tf, bf, projects);
                int orieantaion = 1;
                if (context.queryParam(MATRIX_ORIENTATION) != null) {
                    orieantaion = Integer.valueOf(context.queryParam(MATRIX_ORIENTATION));
                }
                if ("baseajax".equals(format)) {
                    context.header("Content-Type", "text/html; charset=UTF-8").status(OToolService.OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlAjaxFormatter(names, projects, mSettings)));
                } else if ("htmlspan".equals(format)) {
                    context.header("Content-Type", "text/html; charset=UTF-8").status(OToolService.OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlSpanningFormatter(names, projects)));
                } else if ("html".equals(format)) {
                    context.header("Content-Type", "text/html; charset=UTF-8").status(OToolService.OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlFormatter(names, projects)));
                } else if ("fill".equals(format)) {
                    String vr = context.queryParam("vr");
                    String time = context.queryParam("time");
                    String chartDir = context.queryParam("chartDir");
                    String explicitComparsions = context.queryParam("explicitcomparsion");
                    boolean alsoReport = OToolService.notNullBoolean(context, "alsoReport", false);
                    if (vr == null || vr.isEmpty()) {
                        context.status(400).result("nvr=<nvr> necessary");
                    } else {
                        final SummaryReportRunner summaryReportRunner = new SummaryReportRunner(mSettings, vr, time, chartDir, Optional.ofNullable(explicitComparsions), projects);
                        context.header("Content-Type", "text/html; charset=UTF-8").status(OToolService.OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlSpanningFillingFormatter(projects, names, vr, alsoReport, summaryReportRunner)));
                    }
                } else {
                    context.status(OToolService.OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new PlainTextFormatter()));
                }
            }));

        });

        ApiBuilder.post(JDK_TEST_PROJECTS, mWrapper.wrap(context -> {
            final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
            final JDKTestProject created = jdkTestProjectManager.create(jdkTestProject);
            final ManagementResult result = new ManagementResult<>(created, mJenkinsJobUpdater.update(null, jdkTestProject));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.get(JDK_TEST_PROJECTS, mWrapper.wrap(context -> context.json(jdkTestProjectManager.readAll())));
        ApiBuilder.put(JDK_TEST_PROJECT, mWrapper.wrap(context -> {
            final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
            final String id = context.pathParam(ID);
            final JDKTestProject old = jdkTestProjectManager.read(id);
            final JDKTestProject updated = jdkTestProjectManager.update(id, jdkTestProject);
            final ManagementResult result = new ManagementResult<>(updated, mJenkinsJobUpdater.update(old, updated));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.delete(JDK_TEST_PROJECT, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            final JDKTestProject deleted = jdkTestProjectManager.delete(id);
            final ManagementResult<JDKTestProject> result = new ManagementResult<>(deleted, mJenkinsJobUpdater.update(deleted, null));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.get(BUILD_PROVIDERS, mWrapper.wrap(context -> context.json(buildProviderManager.readAll())));
        ApiBuilder.get(JDK_VERSIONS, mWrapper.wrap(context -> context.json(jdkVersionManager.readAll())));
        ApiBuilder.get(PLATFORMS, mWrapper.wrap(context -> context.json(platformManager.readAll())));
        ApiBuilder.post(PLATFORMS, mWrapper.wrap(context -> {
            final Platform platform = context.bodyValidator(Platform.class).get();
            final Platform created = platformManager.create(platform);
            final ManagementResult<Platform> result = new ManagementResult<>(created);
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.put(PLATFORM, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            final Platform platform = context.bodyValidator(Platform.class).get();
            final Platform updated = platformManager.update(id, platform);
            final ManagementResult<Platform> result = new ManagementResult<>(updated, mJenkinsJobUpdater.update(updated));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.delete(PLATFORM, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            final Platform deleted = platformManager.delete(id);
            final ManagementResult<Platform> result = new ManagementResult<>(deleted);
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.post(TASKS, mWrapper.wrap(context -> {
            final Task task = context.bodyValidator(Task.class).get();
            final Task created = taskManager.create(task);
            final ManagementResult<Task> result = new ManagementResult<>(created);
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.get(TASKS, mWrapper.wrap(context -> context.json(taskManager.readAll())));
        ApiBuilder.put(TASK, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            final Task task = context.bodyValidator(Task.class).get();
            final Task updated = taskManager.update(id, task);
            final ManagementResult<Task> result = new ManagementResult<>(updated, mJenkinsJobUpdater.update(updated));
            context.json(result).status(OToolService.OK);
        }));
        ApiBuilder.delete(TASK, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            taskManager.delete(id); // not supported
        }));
        ApiBuilder.get(TASK_VARIANTS, mWrapper.wrap(context -> context.json(taskVariantManager.readAll())));
        ApiBuilder.post(JDK_PROJECTS, mWrapper.wrap(context -> {
            final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
            final JDKProject created = jdkProjectManager.create(jdkProject);
            final ManagementResult<JDKProject> result = new ManagementResult<>(created, mJenkinsJobUpdater.update(null, created));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.get(JDK_PROJECTS, mWrapper.wrap(context -> context.json(jdkProjectManager.readAll())));
        ApiBuilder.put(JDK_PROJECT, mWrapper.wrap(context -> {
            final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
            final String id = context.pathParam(ID);
            final JDKProject old = jdkProjectManager.read(id);
            final JDKProject updated = jdkProjectManager.update(id, jdkProject);
            final ManagementResult<JDKProject> result = new ManagementResult<>(updated, mJenkinsJobUpdater.update(old, updated));
            context.status(OToolService.OK).json(result);
        }));
        ApiBuilder.delete(JDK_PROJECT, mWrapper.wrap(context -> {
            final String id = context.pathParam(ID);
            final JDKProject deleted = jdkProjectManager.delete(id);
            final ManagementResult<JDKProject> result = new ManagementResult<>(deleted, mJenkinsJobUpdater.update(deleted, null));
            context.status(OToolService.OK).json(result);
        }));
        path(GET, mGetterApi);
    }
}
