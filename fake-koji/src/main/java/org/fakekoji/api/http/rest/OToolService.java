package org.fakekoji.api.http.rest;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJackson;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.BuildEqualityFilter;
import org.fakekoji.core.utils.matrix.MatrixGenerator;
import org.fakekoji.core.utils.matrix.TestEqualityFilter;
import org.fakekoji.jobmanager.*;
import org.fakekoji.jobmanager.manager.*;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
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
import java.util.regex.Pattern;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static org.fakekoji.core.AccessibleSettings.objectMapper;

public class OToolService {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final String ID = "id";
    private static final String CONFIG_ID = "/:" + ID;
    private static final String BUILD_PROVIDERS = "/buildProviders";
    private static final String BUILD_PROVIDER = BUILD_PROVIDERS + CONFIG_ID;
    private static final String JDK_VERSIONS = "/jdkVersions";
    private static final String JDK_VERSION = JDK_VERSIONS + CONFIG_ID;
    private static final String PLATFORMS = "/platforms";
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

    private static final String MISC = "misc";
    private static final String HELP = "help";
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
    private static final String VIEWS_DETAILS= "details";
    private static final String VIEWS_MATCHES= "matches";
    private static final String VIEWS_MATCHES_JENKINS= "jenkins";
    private static final String VIEWS_XMLS= "xmls";
    private static final String VIEWS_CREATE = "create";
    private static final String VIEWS_REMOVE = "remove";
    private static final String VIEWS_UPDATE = "update";
    private static final String REGENERATE_ALL = "regenerateAll";
    private static final String MATRIX = "matrix";
    private static final String MATRIX_ORIENTATION = "orientation";
    private static final String MATRIX_BREGEX = "buildRegex";
    private static final String MATRIX_TREGEX = "testRegex";

    private final int port;
    private final Javalin app;
    private final JobUpdater jenkinsJobUpdater;

    private String getMiscHelp() {
        return ""
                + MISC + "/" + REGENERATE_ALL + "/" + JDK_TEST_PROJECTS + "\n"
                + MISC + "/" + REGENERATE_ALL + "/" + JDK_PROJECTS + "\n"
                + "                optional argument project=         \n"
                + "                to regenerate only single project  \n"
                + MISC + "/" + UPDATE_JOBS+ "/{" + UPDATE_JOBS_LIST+","+UPDATE_JOBS_XMLS+","+UPDATE_JOBS_CREATE+","+ UPDATE_JOBS_REMOVE+","+ UPDATE_JOBS_UPDATE+"}\n"
                + "                will affect update machines jobs\n"
                + "                params: "+FILTER+"=regex "+ONLY_HW+"=bool "+ONLY_VM+"=bool\n"
                + MISC + "/" + VIEWS + "/{" + VIEWS_LIST_OTOOL+","+VIEWS_DETAILS+","+VIEWS_XMLS+","+ VIEWS_CREATE+","+ VIEWS_REMOVE+","+ VIEWS_UPDATE+","+VIEWS_MATCHES+","+VIEWS_MATCHES_JENKINS+ "}\n"
                + "                will affect jenkins views\n"
                + "                params: "+FILTER+"=regex "+SKIP_EMPTY+"=true/false\n"
                + MISC + "/" + MATRIX + "\n"
                + "  where parameters for matrix are (with defaults):\n"
                + "  " + MATRIX_ORIENTATION + "=1 " + MATRIX_BREGEX + "=.* " + MATRIX_TREGEX + "=.* \n"
                + "  " + "tos=true tarch=true tprovider=false tsuite=true tvars=false bos=true barch=true bprovider=false bproject=true bjdk=true bvars=false\n"
                + "  dropRows=true dropColumns=true \n";
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
                context.status(400).result(notNullMessage(e));
            } catch (StorageException e) {
                LOGGER.log(Level.SEVERE, notNullMessage(e), e);
                context.status(500).result(notNullMessage(e));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, notNullMessage(e), e);
                context.status(501).result(notNullMessage(e));
            }
        };

        app.routes(() -> {

            final JDKTestProjectManager jdkTestProjectManager = new JDKTestProjectManager(
                    configManager.getJdkTestProjectStorage(),
                    jenkinsJobUpdater
            );
            final JDKProjectManager jdkProjectManager = new JDKProjectManager(
                    configManager,
                    jenkinsJobUpdater,
                    settings.getLocalReposRoot(),
                    settings.getScriptsRoot()
            );
            final PlatformManager platformManager = new PlatformManager(configManager.getPlatformStorage(), jenkinsJobUpdater);
            final TaskManager taskManager = new TaskManager(configManager.getTaskStorage(), jenkinsJobUpdater);

            path(MISC, () -> {
                get(HELP, wrapper.wrap(context -> {
                    context.status(200).result(getMiscHelp());
                }));
                path(UPDATE_JOBS, () -> {
                    get(UPDATE_JOBS_LIST, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(200).result(ua.getList(allUpdates) + "\n");
                            }
                    ));
                    get(UPDATE_JOBS_XMLS, wrapper.wrap(context -> {
                                UpdateVmsApi ua = new UpdateVmsApi(context);
                                List<JenkinsUpdateVmTemplateBuilder> allUpdates = ua.getJenkinsUpdateVmTemplateBuilders(settings, platformManager);
                                context.status(200).result(ua.getXmls(allUpdates));
                            }
                    ));
                });
                path(VIEWS, () -> {
                    get(VIEWS_LIST_OTOOL, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                if (va.isSkipEmpty()) {
                                    List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                    String viewsAndMatchesToPrint= va.listNonEmpty(jvt, jobs);
                                    context.status(200).result(viewsAndMatchesToPrint);
                                } else {
                                    String viewsAndMatchesToPrint= va.list(jvt);
                                    context.status(200).result(viewsAndMatchesToPrint);
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
                                context.status(200).result(details);
                            }
                    ));
                    get(VIEWS_XMLS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                if (va.isSkipEmpty()) {
                                    List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                    context.status(200).result(va.getNonEmptyXmls(jvt, jobs));
                                } else {
                                    context.status(200).result(va.getXmls(jvt));
                                }
                            }
                    ));
                    get(VIEWS_MATCHES, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = getAllOtoolJobs(settings, jdkTestProjectManager, jdkProjectManager);
                                context.status(200).result(va.printMatches(jvt, jobs));
                            }
                    ));
                    get(VIEWS_MATCHES_JENKINS, wrapper.wrap(context -> {
                                ViewsAppi va = new ViewsAppi(context);
                                List<JenkinsViewTemplateBuilder> jvt = va.getJenkinsViewTemplateBuilders(jdkTestProjectManager, jdkProjectManager, platformManager, taskManager);
                                List<String> jobs = GetterAPI.getAllJenkinsJobs();
                                Collections.sort(jobs);
                                context.status(200).result(va.printMatches(jvt, jobs));
                            }
                    ));
                });
                path(REGENERATE_ALL, () -> {
                    get(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        JobUpdateResults r1 = jdkTestProjectManager.regenerateAll(project);
                        context.status(200).json(r1);
                    }));
                    get(JDK_PROJECTS, wrapper.wrap(context -> {
                        String project = context.queryParam("project");
                        JobUpdateResults r2 = jdkProjectManager.regenerateAll(project);
                        context.status(200).json(r2);
                    }));
                });
                get(MATRIX, wrapper.wrap(context -> {
                    String trex = context.queryParam(MATRIX_TREGEX);
                    String brex = context.queryParam(MATRIX_BREGEX);
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
                    TestEqualityFilter tf = new TestEqualityFilter(tos, tarch, tprovider, tsuite, tvars);
                    BuildEqualityFilter bf = new BuildEqualityFilter(bos, barch, bprovider, bproject, bjdk, bvars);
                    MatrixGenerator m = new MatrixGenerator(settings, configManager, trex, brex, tf, bf);
                    int orieantaion = 1;
                    if (context.queryParam(MATRIX_ORIENTATION) != null) {
                        orieantaion = Integer.valueOf(context.queryParam(MATRIX_ORIENTATION));
                    }
                    context.status(200).result(m.printMatrix(orieantaion, dropRows, dropColumns));
                }));

            });

            app.post(JDK_TEST_PROJECTS, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final ManagementResult result = jdkTestProjectManager.create(jdkTestProject);
                context.status(200).json(result);
            }));
            app.get(JDK_TEST_PROJECTS, wrapper.wrap(context -> context.json(jdkTestProjectManager.readAll())));
            app.put(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final JDKTestProject jdkTestProject = context.bodyValidator(JDKTestProject.class).get();
                final String id = context.pathParam(ID);
                final ManagementResult result = jdkTestProjectManager.update(id, jdkTestProject);
                context.status(200).json(result);
            }));
            app.delete(JDK_TEST_PROJECT, wrapper.wrap(context -> {
                final String id = context.pathParam(ID);
                final ManagementResult result = jdkTestProjectManager.delete(id);
                context.status(200).json(result);
            }));

            final BuildProviderManager buildProviderManager = new BuildProviderManager(configManager.getBuildProviderStorage());
            app.get(BUILD_PROVIDERS, context -> context.json(buildProviderManager.readAll()));

            final JDKVersionManager jdkVersionManager = new JDKVersionManager(configManager.getJdkVersionStorage());
            app.get(JDK_VERSIONS, context -> context.json(jdkVersionManager.readAll()));

            app.get(PLATFORMS, context -> context.json(platformManager.readAll()));
            app.post(PLATFORMS, context -> {
                try {
                    final Platform platform = context.bodyValidator(Platform.class).get();
                    final ManagementResult<Platform> result = platformManager.create(platform);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.put(PLATFORM, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final Platform platform = context.bodyValidator(Platform.class).get();
                    final ManagementResult<Platform> result = platformManager.update(id, platform);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(PLATFORM, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<Platform> result = platformManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });

            app.post(TASKS, context -> {
                try {
                    final Task task = context.bodyValidator(Task.class).get();
                    final ManagementResult<Task> result = taskManager.create(task);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.get(TASKS, context -> context.json(taskManager.readAll()));
            app.put(TASK, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final Task task = context.bodyValidator(Task.class).get();
                    final ManagementResult<Task> result = taskManager.update(id, task);
                    context.json(result).status(200);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(TASK, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<Task> result = taskManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });

            final TaskVariantManager taskVariantManager = new TaskVariantManager(configManager.getTaskVariantStorage());
            app.get(TASK_VARIANTS, context -> context.json(taskVariantManager.readAll()));

            app.post(JDK_PROJECTS, context -> {
                try {
                    final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                    final ManagementResult<JDKProject> result = jdkProjectManager.create(jdkProject);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.get(JDK_PROJECTS, context -> context.json(jdkProjectManager.readAll()));
            app.put(JDK_PROJECT, context -> {
                try {
                    final JDKProject jdkProject = context.bodyValidator(JDKProject.class).get();
                    final String id = context.pathParam(ID);
                    final ManagementResult<JDKProject> result = jdkProjectManager.update(id, jdkProject);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            app.delete(JDK_PROJECT, context -> {
                try {
                    final String id = context.pathParam(ID);
                    final ManagementResult<JDKProject> result = jdkProjectManager.delete(id);
                    context.status(200).json(result);
                } catch (ManagementException e) {
                    context.status(400).result(e.toString());
                } catch (StorageException e) {
                    context.status(500).result(e.toString());
                }
            });
            path(GET, new GetterAPI(
                    settings,
                    jdkProjectManager,
                    jdkTestProjectManager,
                    jdkVersionManager,
                    taskVariantManager
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
