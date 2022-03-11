package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JsonMapper;

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
import org.fakekoji.jobmanager.JenkinsUpdateVmTemplateBuilder;
import org.fakekoji.jobmanager.views.JenkinsViewTemplateBuilder;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.ConfigManager;
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
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.fakekoji.core.AccessibleSettings.objectMapper;

public class OToolService {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final int OK = 200;
    public static final int BAD = 400;
    public static final int ERROR = 500;

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
    public static final String RESULTS_DB= "resultsDb";
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

    private final int port;
    private final Javalin app;

    private String getMiscHelp() {
        return ""
                + MISC + "/" + REGENERATE_ALL +  JDK_TEST_PROJECTS + "\n"
                + MISC + "/" + REGENERATE_ALL + JDK_PROJECTS + "\n"
                + "                mandatory arguments project=    to regenerate only single project     \n"
                + "                and whitelist=regex to limit the search even more  \n"
                + "\n"
                + MISC + "/" + UPDATE_JOBS + "/{" + UPDATE_JOBS_LIST + "," + UPDATE_JOBS_XMLS + "," + UPDATE_JOBS_CREATE + "," + UPDATE_JOBS_REMOVE + "," + UPDATE_JOBS_UPDATE + "}\n"
                + "                will affect update machines jobs\n"
                + "                params: " + FILTER + "=regex " + ONLY_HW + "=bool " + ONLY_VM + "=bool\n"
                + "\n"
                + MISC + "/" + VIEWS + "/{" + VIEWS_LIST_OTOOL + "," + VIEWS_DETAILS + "," + VIEWS_XMLS + "," + VIEWS_CREATE + "," + VIEWS_REMOVE + "," + VIEWS_UPDATE + "," + VIEWS_MATCHES + "," + VIEWS_MATCHES_JENKINS + "}\n"
                + "                will affect jenkins views\n"
                + "                params: " + FILTER + "=regex " + SKIP_EMPTY + "=true/false " + NESTED + "=true/false "+NESTED_COLUMNS+"=true/false/number  if number is put, then the default columns(true) are generated for views withh less then number of matches\n"
                + "                similar param is e "+CUSTOM_COLUMNS+"=true/false/number  if number is put, then the CUSTOM columns(true) are generated for list-views with less then number of jobs\n"
                + "                Note, nested=true requires jenkins nested-view-plugin. Warning, in nested-views, filter matches only top level component (tasks, paltforms..)\n"
                + "\n"
                + MISC + "/" + MATRIX + "\n"
                + "  where parameters for matrix are (with defaults):\n"
                + "  " + MATRIX_ORIENTATION + "=1 " + MATRIX_BREGEX + "=.* " + MATRIX_TREGEX + "=.* " + MATRIX_CREGEX + "=.* " + MATRIX_FORMAT + "=baseajax/htmlspan/html/plain/fill (baseajax have optional nvr; fill requires vr=>nvr time=number alsoReport=false and optional chartDir=path>\n"
                + "  " + "tos=true tarch=true tprovider=false tsuite=true tvars=false bos=true barch=true bprovider=false bproject=true bjdk=true bvars=false\n"
                + "  dropRows=true dropColumns=true  project=p1,p2,...,pn /*to generate matrix only for given projects*/\n"
                + "                                                                                                    WARNING! chartDir is directory on SERVER and is deleted if exists!/\n"
                + "  names=true will chage numbers to somehow more describing texts in html formatters. With huge squezing can go wil. Is sometimes buggy with platforms\n"
                + "  explicitcomparsion=url by default null, unused, to load remote file with listed lates released NVRs to comapre with\n"
                + "  baseajax have few runtime switches: automanFilter=index sort=index readOnly=[true(visible)/false(hidden)/total]. Defaults are 0,0,false\n";
    }

    public OToolService(AccessibleSettings settings) {
        this.port = settings.getWebappPort();
        app = Javalin.create( config -> {
                    config.jsonMapper(new JavalinJackson(objectMapper));
                    config.addStaticFiles("/webapp", Location.CLASSPATH);
                }
        );

        final JobUpdater jenkinsJobUpdater = settings.getJobUpdater();
        final GetterAPI getterApi = new GetterAPI(settings);

        final OToolHandlerWrapper wrapper = oToolHandler -> context -> {
            try {
                oToolHandler.handle(context);
            } catch (ManagementException e) {
                LOGGER.log(Level.SEVERE, notNullMessage(e), e);
                context.status(BAD).result(notNullMessage(e));
            } catch (StorageException e) {
                LOGGER.log(Level.SEVERE, notNullMessage(e), e);
                context.status(ERROR).result(notNullMessage(e));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, notNullMessage(e), e);
                context.status(501).result(notNullMessage(e));
            }
        };

        app.routes(() -> {
            final ConfigManager configManager = settings.getConfigManager();
            final JDKTestProjectManager jdkTestProjectManager = configManager.jdkTestProjectManager;
            final JDKProjectManager jdkProjectManager = configManager.jdkProjectManager;
            final PlatformManager platformManager = configManager.platformManager;
            final TaskManager taskManager = configManager.taskManager;
            final JDKVersionManager jdkVersionManager = configManager.jdkVersionManager;
            final BuildProviderManager buildProviderManager = configManager.buildProviderManager;
            final TaskVariantManager taskVariantManager = configManager.taskVariantManager;

            path(MISC, () -> {
                get(HELP, wrapper.wrap(context -> {
                    context.status(OK).result(getMiscHelp()
                            + BumperAPI.getHelp()
                            + ResultsDb.getHelp()
                            + RedeployApi.getHelp()
                            + CancelApi.getHelp());
                }));
                path(RESULTS_DB, new ResultsDb(settings));
                path(BUMP, new BumperAPI(settings));
                path(RedeployApi.REDEPLOY, new RedeployApi(settings));
                path(CancelApi.NO, new CancelApi(settings));
                path(UPDATE_JOBS, () -> {
                    get(UPDATE_JOBS_LIST, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(OK).result(ua.getList(allUpdates) + "\n");
                            }
                    ));
                    get(UPDATE_JOBS_XMLS, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(OK).result(ua.getXmls(allUpdates));
                            }
                    ));
                    get(UPDATE_JOBS_CREATE, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(OK).result(ua.create(allUpdates));
                            }
                    ));
                    get(UPDATE_JOBS_UPDATE, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(OK).result(ua.update(allUpdates));
                            }
                    ));
                    get(UPDATE_JOBS_REMOVE, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(OK).result(ua.remvoe(allUpdates));
                            }
                    ));
                });
                path(VIEWS, () -> {
                    get(VIEWS_CREATE, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                                String results = va.create(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_REMOVE, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager, Optional.of(jobs));
                                String results = va.delete(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_UPDATE, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                String results = va.update(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_LIST_OTOOL, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                if (va.isSkipEmpty()) {
                                    String viewsAndMatchesToPrint = va.listNonEmpty(jvt, jobs);
                                    context.status(OK).result(viewsAndMatchesToPrint);
                                } else {
                                    String viewsAndMatchesToPrint = va.list(jvt, Optional.empty(), false, false);
                                    context.status(OK).result(viewsAndMatchesToPrint);
                                }
                            }
                    ));
                    get(VIEWS_DETAILS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                String details = va.getDetails(jvt, jobs);
                                context.status(OK).result(details);
                            }
                    ));
                    get(VIEWS_XMLS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                if (va.isSkipEmpty()) {
                                    context.status(OK).result(va.getNonEmptyXmls(jvt, jobs));
                                } else {
                                    context.status(OK).result(va.getXmls(jvt));
                                }
                            }
                    ));
                    get(VIEWS_MATCHES, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = getterApi.getAllOtoolJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                context.status(OK).result(va.printMatches(jvt, jobs));
                            }
                    ));
                    get(VIEWS_MATCHES_JENKINS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<String> jobs = GetterAPI.getAllJenkinsJobs();
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager, taskVariantManager, jdkVersionManager,Optional.of(jobs));
                                Collections.sort(jobs);
                                context.status(OK).result(va.printMatches(jvt, jobs));
                            }
                    ));
                });
                path(REGENERATE_ALL, () -> {
                    get(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        String whitelist = context.queryParam("whitelist");
                        if (whitelist == null || project == null){
                            context.status(BAD).result("project and whitelist are mandatory. use ?whitelist=.*&project=ALL if yo are reall sure\n");
                        } else {
                            if (project.equals("ALL")){
                                project = null;
                            }
                            JobUpdateResults r1 = jenkinsJobUpdater.regenerateAll(project, jdkTestProjectManager, whitelist);
                            context.status(OK).json(r1);
                        }
                    }));
                    get(JDK_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        String whitelist = context.queryParam("whitelist");
                        if (whitelist == null || project == null){
                            context.status(BAD).result("project and whitelist are mandatory. use ?whitelist=.*&project=ALL if yo are reall sure\n");
                        } else {
                            if (project.equals("ALL")) {
                                project = null;
                            }
                            JobUpdateResults r2 = jenkinsJobUpdater.regenerateAll(project, jdkProjectManager, whitelist);
                            context.status(OK).json(r2);
                        }
                    }));
                });
                get(MATRIX, wrapper.wrap(context -> {
                    String trex = context.queryParam(MATRIX_TREGEX);
                    String brex = context.queryParam(MATRIX_BREGEX);
                    String crex = context.queryParam(MATRIX_CREGEX);
                    String format = context.queryParam(MATRIX_FORMAT);
                    Boolean namesParam = nullableBooleanObject(context, "names", null);
                    boolean tos = notNullBoolean(context, "tos", true);
                    boolean tarch = notNullBoolean(context, "tarch", true);
                    boolean tprovider = notNullBoolean(context, "tprovider", false);
                    boolean tsuite = notNullBoolean(context, "tsuite", true);
                    boolean tvars = notNullBoolean(context, "tvars", false);
                    boolean bos = notNullBoolean(context, "bos", true);
                    boolean barch = notNullBoolean(context, "barch", true);
                    boolean bprovider = notNullBoolean(context, "bprovider", false);
                    boolean bproject = notNullBoolean(context, "bproject", true);
                    boolean bjdk = notNullBoolean(context, "bjdk", true);
                    boolean bvars = notNullBoolean(context, "bvars", false);
                    boolean dropRows = notNullBoolean(context, "dropRows", true);
                    boolean dropColumns = notNullBoolean(context, "dropColumns", true);
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
                    MatrixGenerator m = new MatrixGenerator(settings, trex, brex, crex, tf, bf, projects);
                    int orieantaion = 1;
                    if (context.queryParam(MATRIX_ORIENTATION) != null) {
                        orieantaion = Integer.valueOf(context.queryParam(MATRIX_ORIENTATION));
                    }
                    if ("baseajax".equals(format)) {
                        context.header("Content-Type", "text/html; charset=UTF-8").status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlAjaxFormatter(names, projects, settings)));
                    } else if ("htmlspan".equals(format)) {
                        context.header("Content-Type", "text/html; charset=UTF-8").status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlSpanningFormatter(names, projects)));
                    } else if ("html".equals(format)) {
                        context.header("Content-Type", "text/html; charset=UTF-8").status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new HtmlFormatter(names, projects)));
                    } else if ("fill".equals(format)) {
                        String vr = context.queryParam("vr");
                        String time = context.queryParam("time");
                        String chartDir = context.queryParam("chartDir");
                        String explicitComparsions = context.queryParam("explicitcomparsion");
                        boolean alsoReport = notNullBoolean(context, "alsoReport", false);
                        if (vr == null || vr.isEmpty()) {
                            context.status(400).result("nvr=<nvr> necessary");
                        } else {
                            final SummaryReportRunner summaryReportRunner = new SummaryReportRunner(settings, vr, time, chartDir, Optional.ofNullable(explicitComparsions), projects);
                            context.header("Content-Type", "text/html; charset=UTF-8").status(OK).result(m.printMatrix(
                                    orieantaion, dropRows, dropColumns,
                                    new HtmlSpanningFillingFormatter(projects, names, vr, alsoReport, summaryReportRunner)));
                        }
                    } else {
                        context.status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new PlainTextFormatter()));
                    }
                }));

            });

            app.post(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final JDKTestProject created = jdkTestProjectManager.create(jdkTestProject);
                final ManagementResult result = new ManagementResult<>(
                        created,
                        jenkinsJobUpdater.update(null, jdkTestProject)
                );
                context.status(OK).json(result);
            }));
            app.get(JDK_TEST_PROJECTS, wrapper.wrap(context -> context.json(jdkTestProjectManager.readAll())));
            app.put(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final String id = context.pathParam(ID);
                final JDKTestProject old = jdkTestProjectManager.read(id);
                final JDKTestProject updated = jdkTestProjectManager.update(id, jdkTestProject);
                final ManagementResult result = new ManagementResult<>(updated, jenkinsJobUpdater.update(
                        old,
                        updated
                ));
                context.status(OK).json(result);
            }));
            app.delete(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final JDKTestProject deleted = jdkTestProjectManager.delete(id);
                final ManagementResult<JDKTestProject> result = new ManagementResult<>(
                        deleted,
                        jenkinsJobUpdater.update(deleted, null)
                );
                context.status(OK).json(result);
            }));
            app.get(BUILD_PROVIDERS, wrapper.wrap(context -> context.json(buildProviderManager.readAll())));
            app.get(JDK_VERSIONS, wrapper.wrap(context -> context.json(jdkVersionManager.readAll())));
            app.get(PLATFORMS, wrapper.wrap(context -> context.json(platformManager.readAll())));
            app.post(PLATFORMS, wrapper.wrap(context -> {
                final Platform platform = context.bodyValidator(Platform.class).get();
                final Platform created = platformManager.create(platform);
                final ManagementResult<Platform> result = new ManagementResult<>(created);
                context.status(OK).json(result);
            }));
            app.put(PLATFORM, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final Platform platform = context.bodyValidator(Platform.class).get();
                final Platform updated = platformManager.update(id, platform);
                final ManagementResult<Platform> result = new ManagementResult<>(
                        updated,
                        jenkinsJobUpdater.update(updated)
                );
                context.status(OK).json(result);
            }));
            app.delete(PLATFORM, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final Platform deleted = platformManager.delete(id);
                final ManagementResult<Platform> result = new ManagementResult<>(deleted);
                context.status(OK).json(result);
            }));
            app.post(TASKS, wrapper.wrap(context -> {
                final Task task = context.bodyValidator(Task.class).get();
                final Task created = taskManager.create(task);
                final ManagementResult<Task> result = new ManagementResult<>(created);
                context.status(OK).json(result);
            }));
            app.get(TASKS, wrapper.wrap(context -> context.json(taskManager.readAll())));
            app.put(TASK, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final Task task = context.bodyValidator(Task.class).get();
                final Task updated = taskManager.update(id, task);
                final ManagementResult<Task> result = new ManagementResult<>(
                        updated,
                        jenkinsJobUpdater.update(updated)
                );
                context.json(result).status(OK);
            }));
            app.delete(TASK, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                taskManager.delete(id); // not supported
            }));
            app.get(TASK_VARIANTS, wrapper.wrap(context -> context.json(taskVariantManager.readAll())));
            app.post(JDK_PROJECTS, wrapper.wrap(context -> {
                final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                final JDKProject created = jdkProjectManager.create(jdkProject);
                final ManagementResult<JDKProject> result = new ManagementResult<>(
                        created,
                        jenkinsJobUpdater.update(null, created)
                );
                context.status(OK).json(result);
            }));
            app.get(JDK_PROJECTS, wrapper.wrap(context -> context.json(jdkProjectManager.readAll())));
            app.put(JDK_PROJECT, wrapper.wrap(context -> {
                final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                final String id = context.pathParam(ID);
                final JDKProject old = jdkProjectManager.read(id);
                final JDKProject updated = jdkProjectManager.update(id, jdkProject);
                final ManagementResult<JDKProject> result = new ManagementResult<>(
                        updated,
                        jenkinsJobUpdater.update(old, updated)
                );
                context.status(OK).json(result);
            }));
            app.delete(JDK_PROJECT, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final JDKProject deleted = jdkProjectManager.delete(id);
                final ManagementResult<JDKProject> result = new ManagementResult<>(
                        deleted,
                        jenkinsJobUpdater.update(deleted, null)
                );
                context.status(OK).json(result);
            }));
            path(GET, getterApi);
        });
    }

    private String notNullMessage(Exception e) {
        if (e == null) {
            return "exception witout exception. Good luck logger.";
        } else {
            if (e.getMessage() == null) {
                return "Exception was thrown, but no message was left. Pray for trace.";
            } else {
                return e.getMessage();
            }
        }
    }

    public static boolean notNullBoolean(Context context, String key, boolean defoult) {
        if (context.queryParam(key) == null) {
            return defoult;
        } else {
            return Boolean.valueOf(context.queryParam(key));
        }
    }

    public static Boolean nullableBooleanObject(Context context, String key, Boolean defoult) {
        if (context.queryParam(key) == null) {
            return defoult;
        } else {
            return Boolean.valueOf(context.queryParam(key));
        }
    }


    public void start() {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }

    public int getPort() {
        return port;
    }

    interface OToolHandler {
        void handle(Context context) throws Exception;
    }

    interface OToolHandlerWrapper {
        Handler wrap(OToolHandler oToolHandler);
    }
}
