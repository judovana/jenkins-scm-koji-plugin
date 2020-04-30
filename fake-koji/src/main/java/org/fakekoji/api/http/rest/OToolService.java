package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJackson;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.BuildEqualityFilter;
import org.fakekoji.core.utils.matrix.MatrixGenerator;
import org.fakekoji.core.utils.matrix.TableFormatter;
import org.fakekoji.core.utils.matrix.TestEqualityFilter;
import org.fakekoji.jobmanager.*;
import org.fakekoji.jobmanager.manager.*;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.jobmanager.project.ReverseJDKProjectParser;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.fakekoji.core.AccessibleSettings.objectMapper;

public class OToolService {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final int OK = 200;
    private static final int BAD = 400;
    private static final int ERROR = 500;

    private static final String ID = "id";
    private static final String CONFIG_ID = "/:" + ID;
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
    public static final String MISC = "misc";
    private static final String HELP = "help";
    public static final String PRODUCTS = "/products";
    static final String FILTER = "filter";
    static final String SKIP_EMPTY = "skipEmpty";
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
    private static final String MATRIX_FORMAT = "format";
    private static final String PROJECT = "project";

    private static final String ARCHES_EXPECTED = "archesExpected"; //will show or generate arches_expected for NVR. No param - deduct from c
    private static final String ARCHES_EXPECTED_LIST = "list"; //show arches expected for nvr. If no nvr, then list possible arches (from platform's koji arches)
    private static final String ARCHES_EXPECTED_ARCHES = "set"; //coma separated list of arches to put in, note that here we are on  old api only, so koji-arches can be suggested
    private static final String ARCHES_EXPECTED_LIST_NVR = "nvr"; //optional target for list, mandatory target for set; on new api error

    private static final String REDEPLOY = "re";
    private static final String REDEPLOY_TEST = "test";
    private static final String REDEPLOY_BUILD = "build"; //only jdkProject in addtion to removal VR from processed.txt, it cleans  FAILED, ERROR and smaller then 4bytes files from local-builds
    private static final String REDEPLOY_LIST = "list"; //only show what will be done
    private static final String REDEPLOY_DO = "do"; //will do the real work
    //all can be coma separated?
    private static final String REDEPLOY_TYPE = "type"; //jdkProject | jdkTestProject
    private static final String REDEPLOY_PROJECT = "project";
    private static final String REDEPLOY_NVR = "project"; //for jdkProject it shows also type and project, however, can be ommiteed
    //other details for selection
    private static final String REDEPLOY_os = "os";
    private static final String REDEPLOY_arch = "arch";
    private static final String REDEPLOY_version = "version";
    private static final String REDEPLOY_jp = "jp";
    private static final String REDEPLOY_task = "task";
    private static final String REDEPLOY_variants = "variants"; //coma separated list of test variants
    private static final String REDEPLOY_provider = "provider"; //coma separated? list ?of test providers?
    //sometimes we need also build arch to judge
    private static final String REDEPLOY_bos = "bos";
    private static final String REDEPLOY_barch = "barch";
    private static final String REDEPLOY_bversion = "bversion";
    private static final String REDEPLOY_bvariants = "bvariants"; //coma separated list of build variants; for build we do not care about provider?
    private static final String REDEPLOY_bprovider = "bprovider"; //coma separated? list ?of test providers?
    //is needed at the end?
    private static final String REDEPLOY_regex = "regex";


    private final int port;
    private final Javalin app;
    private final JobUpdater jenkinsJobUpdater;

    private String getMiscHelp() {
        return ""
                + MISC + "/" + REGENERATE_ALL + "/" + JDK_TEST_PROJECTS + "\n"
                + MISC + "/" + REGENERATE_ALL + "/" + JDK_PROJECTS + "\n"
                + "                optional argument project=    to regenerate only single project     \n"
                + "                and whitelist=regec to lmit the search even more  \n"
                + MISC + "/" + UPDATE_JOBS + "/{" + UPDATE_JOBS_LIST + "," + UPDATE_JOBS_XMLS + "," + UPDATE_JOBS_CREATE + "," + UPDATE_JOBS_REMOVE + "," + UPDATE_JOBS_UPDATE + "}\n"
                + "                will affect update machines jobs\n"
                + "                params: " + FILTER + "=regex " + ONLY_HW + "=bool " + ONLY_VM + "=bool\n"
                + MISC + "/" + VIEWS + "/{" + VIEWS_LIST_OTOOL + "," + VIEWS_DETAILS + "," + VIEWS_XMLS + "," + VIEWS_CREATE + "," + VIEWS_REMOVE + "," + VIEWS_UPDATE + "," + VIEWS_MATCHES + "," + VIEWS_MATCHES_JENKINS + "}\n"
                + "                will affect jenkins views\n"
                + "                params: " + FILTER + "=regex " + SKIP_EMPTY + "=true/false\n"
                + MISC + "/" + MATRIX + "\n"
                + "  where parameters for matrix are (with defaults):\n"
                + "  " + MATRIX_ORIENTATION + "=1 " + MATRIX_BREGEX + "=.* " + MATRIX_TREGEX + "=.* " + MATRIX_FORMAT + "=htmlspan/html/plain/fill (fill requires vr=>nvr time=number alsoReport=false>\n"
                + "  " + "tos=true tarch=true tprovider=false tsuite=true tvars=false bos=true barch=true bprovider=false bproject=true bjdk=true bvars=false\n"
                + "  dropRows=true dropColumns=true  project=p1,p2,...,pn /*to generate matrix only for given projects*/\n";
    }

    public OToolService(AccessibleSettings settings) {
        this.port = settings.getWebappPort();
        JavalinJackson.configure(objectMapper);
        app = Javalin.create(config -> config
                .addStaticFiles("/webapp")
        );
        jenkinsJobUpdater = new JenkinsJobUpdater(settings);
        final ConfigManager configManager = ConfigManager.create(settings.getConfigRoot().getAbsolutePath());

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

            final JDKTestProjectManager jdkTestProjectManager = new JDKTestProjectManager(
                    configManager.getJdkTestProjectStorage()
            );
            final JDKProjectManager jdkProjectManager = new JDKProjectManager(
                    configManager,
                    settings.getLocalReposRoot(),
                    settings.getScriptsRoot()
            );
            final PlatformManager platformManager = new PlatformManager(configManager.getPlatformStorage());
            final TaskManager taskManager = new TaskManager(configManager.getTaskStorage());
            final JDKVersionManager jdkVersionManager = new JDKVersionManager(configManager.getJdkVersionStorage());

            final JDKProjectParser parser = new JDKProjectParser(
                    ConfigManager.create(settings.getConfigRoot().getAbsolutePath()),
                    settings.getLocalReposRoot(),
                    settings.getScriptsRoot()
            );

            final BumperAPI bumperAPI = new BumperAPI(
                    jenkinsJobUpdater,
                    parser,
                    new ReverseJDKProjectParser(),
                    jdkProjectManager,
                    jdkTestProjectManager,
                    jdkVersionManager,
                    platformManager
            );

            path(MISC, () -> {
                path(BUMP, bumperAPI);
                get(HELP, wrapper.wrap(context -> {
                    context.status(OK).result(getMiscHelp() + bumperAPI.getHelp());
                }));
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
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                String results = va.create(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_REMOVE, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                String results = va.delete(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_UPDATE, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                String results = va.update(jvt, jobs);
                                context.status(OK).result(results);
                            }
                    ));
                    get(VIEWS_LIST_OTOOL, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                if (va.isSkipEmpty()) {
                                    List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                    String viewsAndMatchesToPrint = va.listNonEmpty(jvt, jobs);
                                    context.status(OK).result(viewsAndMatchesToPrint);
                                } else {
                                    String viewsAndMatchesToPrint = va.list(jvt);
                                    context.status(OK).result(viewsAndMatchesToPrint);
                                }
                            }
                    ));
                    get(VIEWS_DETAILS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> allJenkinsJobs = GetterAPI.getAllJenkinsJobs();
                                Collections.sort(allJenkinsJobs);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                String details = va.getDetails(jvt, allJenkinsJobs, jobs);
                                context.status(OK).result(details);
                            }
                    ));
                    get(VIEWS_XMLS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                if (va.isSkipEmpty()) {
                                    List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                    context.status(OK).result(va.getNonEmptyXmls(jvt, jobs));
                                } else {
                                    context.status(OK).result(va.getXmls(jvt));
                                }
                            }
                    ));
                    get(VIEWS_MATCHES, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                context.status(OK).result(va.printMatches(jvt, jobs));
                            }
                    ));
                    get(VIEWS_MATCHES_JENKINS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = GetterAPI.getAllJenkinsJobs();
                                Collections.sort(jobs);
                                context.status(OK).result(va.printMatches(jvt, jobs));
                            }
                    ));
                });
                path(REGENERATE_ALL, () -> {
                    get(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        String whitelist = context.queryParam("whitelist");
                        JobUpdateResults r1 = jenkinsJobUpdater.regenerateAll(project, jdkTestProjectManager, whitelist);
                        context.status(OK).json(r1);
                    }));
                    get(JDK_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        String whitelist = context.queryParam("whitelist");
                        JobUpdateResults r2 = jenkinsJobUpdater.regenerateAll(project, jdkProjectManager, whitelist);
                        context.status(OK).json(r2);

                    }));
                });
                get(MATRIX, wrapper.wrap(context -> {
                    String trex = context.queryParam(MATRIX_TREGEX);
                    String brex = context.queryParam(MATRIX_BREGEX);
                    String format = context.queryParam(MATRIX_FORMAT);
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
                    MatrixGenerator m = new MatrixGenerator(settings, configManager, trex, brex, tf, bf, projects);
                    int orieantaion = 1;
                    if (context.queryParam(MATRIX_ORIENTATION) != null) {
                        orieantaion = Integer.valueOf(context.queryParam(MATRIX_ORIENTATION));
                    }
                    if ("htmlspan".equals(format)) {
                        context.status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new TableFormatter.SpanningHtmlTableFormatter()));
                    } else if ("html".equals(format)) {
                        context.status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new TableFormatter.HtmlTableFormatter()));
                    } else if ("fill".equals(format)) {
                        String vr = context.queryParam("vr");
                        String time = context.queryParam("time");
                        boolean alsoReport = notNullBoolean(context, "alsoReport", false);
                        if (vr == null || vr.isEmpty()) {
                            context.status(400).result("nvr=<nvr> necessary");
                        } else {
                            context.status(OK).result(m.printMatrix(
                                    orieantaion, dropRows, dropColumns,
                                    new TableFormatter.SpanningFillingHtmlTableFormatter(vr, settings, time, alsoReport, projects)));
                        }
                    } else {
                        context.status(OK).result(m.printMatrix(orieantaion, dropRows, dropColumns, new TableFormatter.PlainTextTableFormatter()));
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
                context.status(OK).json(jenkinsJobUpdater.update(deleted, null));
            }));

            final BuildProviderManager buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
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

            final TaskVariantManager taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
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
            path(GET, new GetterAPI(
                    settings,
                    jdkProjectManager,
                    jdkTestProjectManager,
                    jdkVersionManager,
                    taskVariantManager,
                    platformManager,
                    taskManager
            ));
        });
    }

    @NotNull
    private static List<String> getAllOtoolJobs(AccessibleSettings settings, JDKTestProjectManager jdkTestProjectManager, JDKProjectManager jdkProjectManager) throws StorageException, ManagementException {
        List<String> jobs = GetterAPI.getAllJdkJobs(settings, jdkProjectManager, Optional.empty());
        jobs.addAll(GetterAPI.getAllJdkTestJobs(settings, jdkTestProjectManager, Optional.empty()));
        Collections.sort(jobs);
        return jobs;
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
