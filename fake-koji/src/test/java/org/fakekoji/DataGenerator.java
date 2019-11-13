package org.fakekoji;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobConfiguration;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class DataGenerator {

    public static final String NEW_PROJECT_NAME = "new_project_name";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_NAME_U = "uName";
    public static final String PROJECT_URL = "https://gitlab.com/fake_jdk/fake_jdk_repo";
    public static final String PROJECT_URL_U = "https://gitlab.com/fake_jdk_u/fake_jdk_repo_u";
    public static final String INVALID_PROJECT_URL = "shttp://gitlab.com/fake_jdk/fake_jdk_repo";

    public static final String JDK_8 = "jdk8";
    public static final String JDK_8_PACKAGE_NAME = "java-1.8.0-openjdk";
    public static final String JDK_11 = "jdk11";
    public static final String JDK_11_PACKAGE_NAME = "java-11-openjdk";


    public static final String BUILD_PROVIDER_1 = "fakekoji-hydra";
    public static final String BUILD_PROVIDER_2 = "brew-brewhub";

    public static final String RHEL_7_X64 = "el7.x86_64";
    public static final String RHEL_7_X64_VAGRANT = "el7.x86_64.vagrant";
    public static final String F_29_X64_VAGRANT = "f29.x86_64.vagrant";

    public static final String BUILD = "build";
    public static final String TCK = "tck";
    public static final String JTREG = "jtreg";

    public static final String JVM = "jvm";

    public static final String HOTSPOT = "hotspot";
    public static final String ZERO = "zero";

    public static final String DEBUG_MODE = "debugMode";

    public static final String RELEASE = "release";
    public static final String FASTDEBUG = "fastdebug";
    public static final String SLOWDEBUG = "slowdebug";

    public static final String GARBAGE_COLLECTOR = "garbageCollector";

    public static final String SHENANDOAH = "shenandoah";
    public static final String ZGC = "zgc";

    public static final String DISPLAY_PROTOCOL = "displayProtocol";

    public static final String X_SERVER = "xServer";
    public static final String WAYLAND = "wayland";

    public static final String SCP_POLL_SCHEDULE = "H/24 * * * *";

    public static final String BUILD_PROVIDER_1_TOP_URL = "http://hydra.brq.redhat.com:XPORT/RPC2/";
    public static final String BUILD_PROVIDER_1_DOWNLOAD_URL = "http://hydra.brq.redhat.com:DPORT/";
    public static final String BUILD_PROVIDER_2_TOP_URL = "brewtopUrl";
    public static final String BUILD_PROVIDER_2_DOWNLOAD_URL = "brewdownloadUrl";

    private static FolderHolder folderHolder;

    public static Product getJDK8Product() {
        return new Product(
                JDK_8,
                "JDK 8",
                "8",
                JDK_8_PACKAGE_NAME
        );
    }

    public static Product getJDK11Product() {
        return new Product(
                JDK_11,
                "JDK 11",
                "11",
                JDK_11_PACKAGE_NAME
        );
    }

    public static Set<String> getBuildProvidersIds() {
        return new HashSet<>(Arrays.asList(
                BUILD_PROVIDER_1,
                BUILD_PROVIDER_2
        ));
    }

    public static Set<BuildProvider> getBuildProviders() {
        return new HashSet<>(Arrays.asList(
                new BuildProvider(
                        BUILD_PROVIDER_1,
                        "Fake Koji @ Hydra",
                        BUILD_PROVIDER_1_TOP_URL,
                        BUILD_PROVIDER_1_DOWNLOAD_URL
                ),
                new BuildProvider(
                        BUILD_PROVIDER_2,
                        "Brew @ Brewhub",
                        BUILD_PROVIDER_2_TOP_URL,
                        BUILD_PROVIDER_2_DOWNLOAD_URL
                )
        ));
    }

    public static TaskVariantValue getHotspotVariant() {
        return new TaskVariantValue(
                HOTSPOT,
                "Hotspot"
        );
    }

    public static TaskVariantValue getZeroVariant() {
        return new TaskVariantValue(
                ZERO,
                "Zero"
        );
    }

    public static TaskVariantValue getReleaseVariant() {
        return new TaskVariantValue(
                RELEASE,
                "Release"
        );
    }

    public static TaskVariantValue getFastdebugVariant() {
        return new TaskVariantValue(
                FASTDEBUG,
                "Fastdebug"
        );
    }

    public static TaskVariantValue getSlowdebugVariant() {
        return new TaskVariantValue(
                SLOWDEBUG,
                "Slowdebug"
        );
    }

    public static TaskVariantValue getShenandoahVariant() {
        return new TaskVariantValue(
                SHENANDOAH,
                "Shenandoah"
        );
    }

    public static TaskVariantValue getZGCVariant() {
        return new TaskVariantValue(
                ZGC,
                "ZGC"
        );
    }

    public static TaskVariantValue getWaylandVariant() {
        return new TaskVariantValue(
                WAYLAND,
                "Wayland"
        );
    }

    public static TaskVariantValue getXServerVariant() {
        return new TaskVariantValue(
                X_SERVER,
                "X Server"
        );
    }

    public static TaskVariant getJvmVariant() {
        return new TaskVariant(
                JVM,
                "JVM",
                Task.Type.BUILD,
                1,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue hotspot = getHotspotVariant();
                            put(hotspot.getId(), hotspot);
                            final TaskVariantValue zero = getZeroVariant();
                            put(zero.getId(), zero);
                        }}
                )
        );
    }

    public static TaskVariant getDebugModeVariant() {
        return new TaskVariant(
                DEBUG_MODE,
                "Debug mode",
                Task.Type.BUILD,
                0,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue release = getReleaseVariant();
                            put(release.getId(), release);
                            final TaskVariantValue fastdebug = getFastdebugVariant();
                            put(fastdebug.getId(), fastdebug);
                            final TaskVariantValue slowdebug = getSlowdebugVariant();
                            put(slowdebug.getId(), slowdebug);
                        }}
                )
        );
    }

    public static TaskVariant getGarbageCollectorCategory() {
        return new TaskVariant(
                GARBAGE_COLLECTOR,
                "Garbage Collector",
                Task.Type.TEST,
                0,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue shenandoah = getShenandoahVariant();
                            put(shenandoah.getId(), shenandoah);
                            final TaskVariantValue zgc = getZGCVariant();
                            put(zgc.getId(), zgc);
                        }}
                )
        );
    }

    public static TaskVariant getDisplayProtocolCategory() {
        return new TaskVariant(
                DISPLAY_PROTOCOL,
                "Display protocol",
                Task.Type.TEST,
                1,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue wayland = getWaylandVariant();
                            put(wayland.getId(), wayland);
                            final TaskVariantValue xServer = getXServerVariant();
                            put(xServer.getId(), xServer);
                        }}
                )
        );
    }

    public static Set<TaskVariant> getTaskVariants() {
        return new HashSet<>(Arrays.asList(
                getJvmVariant(),
                getDebugModeVariant(),
                getDisplayProtocolCategory(),
                getGarbageCollectorCategory()
        ));
    }

    public static Platform getRHEL7x64Vagrant() {
        return new Platform(
                "el",
                "7",
                "x86_64",
                "vagrant",
                "rhel-x64",
                Arrays.asList("Hydra", "Norn"),
                Collections.emptyList(),
                Collections.singletonList("el7-*")
        );
    }

    public static Platform getF29x64Vagrant() {
        return new Platform(
                "f",
                "29",
                "x86_64",
                "vagrant",
                "f29-x64",
                Arrays.asList("Hydra", "Norn"),
                Collections.emptyList(),
                Collections.singletonList("f29-*")
        );
    }

    public static Platform getVmPlatform() {
        return new Platform(
                "el",
                "7",
                "x86_64",
                "vagrant",
                "rhel7-x64",
                Arrays.asList("Hydra", "Norn"),
                Collections.emptyList(),
                Collections.singletonList("rhel-7.*-candidate")
        );
    }

    public static Platform getHwPlatform() {
        return new Platform(
                "el",
                "7",
                "aarch64",
                "vagrant",
                "rhel7-x64",
                Collections.emptyList(),
                Arrays.asList("Odin", "Tyr"),
                Collections.singletonList("rhel-7.*-candidate")
        );
    }

    public static Map<TaskVariant, TaskVariantValue> getBuildVariants() {
        return Collections.unmodifiableMap(
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }}
        );
    }

    public static Map<TaskVariant, TaskVariantValue> getTestVariants() {
        return Collections.unmodifiableMap(
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getGarbageCollectorCategory(), getShenandoahVariant());
                    put(getDisplayProtocolCategory(), getWaylandVariant());
                }}
        );
    }

    public static Task getBuildTask() {
        return new Task(
                BUILD,
                "/path/build.sh",
                Task.Type.BUILD,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.VM,
                new Task.Limitation<>(Arrays.asList("a", "b"), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        true,
                        Task.BinaryRequirements.NONE
                ),
                BUILD_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        "",
                        null
                )
        );
    }

    public static Task getTestTask() {
        return new Task(
                TCK,
                "/path/test.sh",
                Task.Type.TEST,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.VM,
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        "",
                        null
                )
        );
    }

    public static Task getHwTestTask() {
        return new Task(
                TCK,
                "/path/test.sh",
                Task.Type.TEST,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.HW,
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        "",
                        null
                )
        );
    }

    public static Task getTestTaskRequiringSourcesAndBinary() {
        return new Task(
                JTREG,
                "/path/test.sh",
                Task.Type.TEST,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.HW,
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        true,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        "",
                        null
                )
        );
    }

    public static Task getTestTaskRequiringSourcesAndBinaries() {
        return new Task(
                TCK,
                "/path/test.sh",
                Task.Type.TEST,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.HW,
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        true,
                        Task.BinaryRequirements.BINARIES
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        "",
                        null
                )
        );
    }

    public static Task getTCK() {
        return getTestTask();
    }

    public static Task getJTREG() {
        return getTestTaskRequiringSourcesAndBinary();
    }

    public static final String BUILD_POST_BUILD_TASK =
            "        <hudson.tasks.ArtifactArchiver>\n" +
            "            <artifacts>**.tarxz,**.log, **.html,*.sh,other_logs.tar.gz</artifacts>\n" +
            "            <allowEmptyArchive>false</allowEmptyArchive>\n" +
            "            <onlyIfSuccessful>false</onlyIfSuccessful>\n" +
            "            <fingerprint>false</fingerprint>\n" +
            "            <defaultExcludes>true</defaultExcludes>\n" +
            "            <caseSensitive>true</caseSensitive>\n" +
            "        </hudson.tasks.ArtifactArchiver>\n";

    public static final String TEST_POST_BUILD_TASK =
            "        <hudson.tasks.ArtifactArchiver>\n" +
            "            <artifacts>tck/*</artifacts>\n" +
            "            <allowEmptyArchive>false</allowEmptyArchive>\n" +
            "            <onlyIfSuccessful>false</onlyIfSuccessful>\n" +
            "            <fingerprint>false</fingerprint>\n" +
            "            <defaultExcludes>true</defaultExcludes>\n" +
            "            <caseSensitive>true</caseSensitive>\n" +
            "        </hudson.tasks.ArtifactArchiver>\n" +
            "        <hudson.plugins.report.rpms.RpmsReportPublisher plugin=\"jenkins-report-rpms@0.1-SNAPSHOT\">\n" +
            "            <command>cat tck/rpms.txt</command>\n" +
            "        </hudson.plugins.report.rpms.RpmsReportPublisher>\n" +
            "        <hudson.plugins.report.jck.JckReportPublisher plugin=\"jenkins-report-jck@0.1-SNAPSHOT\">\n" +
            "            <reportFileGlob>report-{runtime,devtools,compiler}.xml.gz</reportFileGlob>\n" +
            "            <resultsBlackList/>\n" +
            "            <resultsWhiteList/>\n" +
            "            <maxBuilds>10</maxBuilds>\n" +
            "        </hudson.plugins.report.jck.JckReportPublisher>\n" +
            "        <hudson.plugins.report.genericchart.GenericChartPublisher plugin=\"jenkins-report-generic-chart-column@0.1-SNAPSHOT\">\n" +
            "            <charts>\n" +
            "                <hudson.plugins.report.genericchart.ChartModel>\n" +
            "                    <title>failures rpms</title>\n" +
            "                    <fileNameGlob>cached-summ-results.properties</fileNameGlob>\n" +
            "                    <key>jrp.failedAndErrors</key>\n" +
            "                    <limit>20</limit>\n" +
            "                    <resultsBlackList>.*upstream.* .*static.*</resultsBlackList>\n" +
            "                    <resultsWhiteList/>\n" +
            "                    <chartColor>#DF7401</chartColor>\n" +
            "                </hudson.plugins.report.genericchart.ChartModel>\n" +
            "                <hudson.plugins.report.genericchart.ChartModel>\n" +
            "                    <title>failures upstream</title>\n" +
            "                    <fileNameGlob>cached-summ-results.properties</fileNameGlob>\n" +
            "                    <key>jrp.failedAndErrors</key>\n" +
            "                    <limit>20</limit>\n" +
            "                    <resultsBlackList>.*el.* .*fc.*</resultsBlackList>\n" +
            "                    <resultsWhiteList/>\n" +
            "                    <chartColor>#FE9A2E</chartColor>\n" +
            "                </hudson.plugins.report.genericchart.ChartModel>\n" +
            "            </charts>\n" +
            "        </hudson.plugins.report.genericchart.GenericChartPublisher>\n";

    public static Set<JDKProject> getJDKProjects() {
        return new HashSet<>(Arrays.asList(
                getJDKProject(),
                getJDKProjectU()
        ));
    }

    public static JDKProject getJDKProject() {
        return getJDKProject(JDKProject.RepoState.NOT_CLONED);
    }

    public static JDKProject getJDKProject(boolean urlValid) {
        return getJDKProject(urlValid, JDKProject.RepoState.NOT_CLONED);
    }

    public static JDKProject getJDKProject(final JDKProject.RepoState repoState) {
        return getJDKProject(PROJECT_NAME, true, repoState);
    }

    public static JDKProject getJDKProject(boolean urlValid, JDKProject.RepoState repoState) {
        return getJDKProject(PROJECT_NAME, urlValid, repoState);
    }

    public static JDKProject getJDKProject(String projectName, boolean urlValid, JDKProject.RepoState repoState) {
        return new JDKProject(
                projectName,
                Project.ProjectType.JDK_PROJECT,
                repoState,
                urlValid ? PROJECT_URL : INVALID_PROJECT_URL,
                DataGenerator.getBuildProvidersIds(),
                JDK_8,
                new JobConfiguration(
                        Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                            put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                    Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                        put(BUILD, new TaskConfig(
                                                Collections.singletonList(
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, RELEASE),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        )
                                                )
                                        ));
                                    }})
                            ));
                        }})
                )
        );
    }

    public static JDKProject getJDKProjectU() {
        return new JDKProject(
                PROJECT_NAME_U,
                Project.ProjectType.JDK_PROJECT,
                JDKProject.RepoState.CLONED,
                PROJECT_URL_U,
                DataGenerator.getBuildProvidersIds(),
                JDK_8,
                new JobConfiguration(
                        Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                            put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                    Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                        put(BUILD, new TaskConfig(
                                                Arrays.asList(
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, RELEASE),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        ),
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, FASTDEBUG),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        ),
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, SLOWDEBUG),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(RHEL_7_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        )
                                                )
                                        ));
                                    }})
                            ));
                            put(F_29_X64_VAGRANT, new PlatformConfig(
                                    Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                        put(BUILD, new TaskConfig(
                                                Arrays.asList(
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, RELEASE),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(F_29_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        ),
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, FASTDEBUG),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(F_29_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        ),
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, SLOWDEBUG),
                                                                Collections.unmodifiableMap(new HashMap<String, PlatformConfig>() {{
                                                                    put(F_29_X64_VAGRANT, new PlatformConfig(
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }})
                                                                    ));
                                                                }})
                                                        )
                                                )
                                        ));
                                    }})
                            ));
                        }})
                )
        );
    }

    private static VariantsConfig getBuildVariantConfig(Map<String, String> map, Map<String, PlatformConfig> platforms) {
        return new VariantsConfig(map, platforms);
    }

    private static Map<String, String> getBuildVariantsMap(String jvm, String debugMode) {
        return Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(JVM, jvm);
            put(DEBUG_MODE, debugMode);
        }});
    }

    private static Map<String, String> getTestVariantsMap(String garbageCollector, String displayProtocol) {
        return Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(GARBAGE_COLLECTOR, garbageCollector);
            put(DISPLAY_PROTOCOL, displayProtocol);
        }});
    }

    public static Set<Product> getProducts() {
        return new HashSet<>(Arrays.asList(
                getJDK8Product(),
                getJDK11Product()
        ));
    }

    public static Set<Platform> getPlatforms() {
        return new HashSet<>(Arrays.asList(
                getRHEL7x64Vagrant(),
                getF29x64Vagrant()
        ));
    }

    public static Set<Task> getTasks() {
        return new HashSet<>(Arrays.asList(
                getBuildTask(),
                getTCK(),
                getJTREG()
        ));
    }

    public static Job get_pull_jdk8_project() {
        return new PullJob(
                PROJECT_NAME,
                getJDK8Product(),
                folderHolder.reposRoot,
                folderHolder.scriptsRoot
        );
    }

    public static Job get_build_jdk8_project_el7_x86_64_vagrant_hotspot_release() {
        return new BuildJob(
                PROJECT_NAME,
                getJDK8Product(),
                getBuildProviders(),
                getBuildTask(),
                getRHEL7x64Vagrant(),
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot
        );
    }

    public static Job get_build_jdk8_project_el7_x86_64_vagrant_hotspot_fastdebug() {
        return new BuildJob(
                PROJECT_NAME,
                getJDK8Product(),
                getBuildProviders(),
                getBuildTask(),
                getRHEL7x64Vagrant(),
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getFastdebugVariant());
                }},
                folderHolder.scriptsRoot
        );
    }

    public static Job get_build_jdk8_project_f29_x86_64_vagrant_hotspot_release() {
        return new BuildJob(
                PROJECT_NAME,
                getJDK8Product(),
                getBuildProviders(),
                getBuildTask(),
                getF29x64Vagrant(),
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot
        );
    }

    public static Job get_tck_jdk8_project_el7_x86_64_hotspot_release_el7_x86_64_vagrant_shenandoah_xServer() {
        final Platform platform = getRHEL7x64Vagrant();
        return new TestJob(
                PROJECT_NAME,
                getJDK8Product(),
                getBuildProviders(),
                getTCK(),
                platform,
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getGarbageCollectorCategory(), getShenandoahVariant());
                    put(getDisplayProtocolCategory(), getXServerVariant());
                }},
                platform,
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot
        );
    }

    public static Job get_tck_jdk8_project_el7_x86_64_hotspot_release_el7_x86_64_vagrant_zgc_xServer() {
        final Platform platform = getRHEL7x64Vagrant();
        return new TestJob(
                PROJECT_NAME,
                getJDK8Product(),
                getBuildProviders(),
                getTCK(),
                platform,
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getGarbageCollectorCategory(), getZGCVariant());
                    put(getDisplayProtocolCategory(), getXServerVariant());
                }},
                platform,
                new HashMap<TaskVariant, TaskVariantValue>(){{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot
        );
    }

    public static final String SUFFIX = ".tarxz";

    public static final String VERSION_1 = "version1";
    public static final String VERSION_2 = "version2";
    public static final String RELEASE_1 = "release1";
    public static final String RELEASE_2 = "re.l.ease.2";
    public static final String SOURCES = "src";

    public static final List<String> notBuilt = Arrays.asList(
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.fastdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.fastdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz"
    );

    public static final String[] versions = new String[] { VERSION_1, VERSION_2 };

    public static final String[] releases = new String[] { RELEASE_1, RELEASE_2 };

    public static void initBuildsRoot(final File buildsRoot) throws IOException {
        final Set<Platform> platforms = DataGenerator.getPlatforms();
        final Set<Product> products = DataGenerator.getProducts();
        for (final JDKProject jdkProject : DataGenerator.getJDKProjects()) {
            final Product product = products.stream()
                    .filter(p -> p.getId().equals(jdkProject.getProduct()))
                    .findFirst().get();
            final File productDir = new File(buildsRoot, product.getPackageName());
            productDir.mkdirs();
            for (int versionIndex = 0; versionIndex < versions.length; versionIndex++) {
                final String version = versions[versionIndex];
                final File versionDir = new File(productDir, version);
                versionDir.mkdirs();
                for (int releaseIndex = 0; releaseIndex < releases.length; releaseIndex++) {
                    final String release = releases[releaseIndex];
                    final String releaseName = release + '.' + jdkProject.getId();
                    final File releaseDir = new File(versionDir, releaseName);
                    releaseDir.mkdirs();
                    final String baseName = product.getPackageName() + '-' + version + '-' + releaseName + '.';
                    final File srcDir = new File(releaseDir, SOURCES);
                    srcDir.mkdirs();
                    final String srcName = baseName + SOURCES + SUFFIX;
                    final File srcFile = new File(srcDir, srcName);
                    Files.write(srcFile.toPath(), srcName.getBytes());
                    for (final Map.Entry<String, PlatformConfig> platformConfig: jdkProject.getJobConfiguration().getPlatforms().entrySet()) {
                        for (final  Map.Entry<String, TaskConfig> buildTaskConfig : platformConfig.getValue().getTasks().entrySet()) {
                            for (final VariantsConfig variantsConfig : buildTaskConfig.getValue().getVariants()) {
                                final String platformId = platformConfig.getKey();
                                final Platform platform = platforms.stream().filter(p -> p.getId().equals(platformId)).findFirst().get();
                                final String archName = variantsConfig.getMap()
                                        .entrySet()
                                        .stream()
                                        .sorted(Comparator.comparing(Map.Entry::getKey))
                                        .map(Map.Entry::getValue)
                                        .collect(Collectors.joining("."))
                                        + '.' + platform.assembleString();
                                final File platformDir = new File(releaseDir, archName);
                                final String archiveFileName = baseName + archName + SUFFIX;
                                if (!notBuilt.contains(archiveFileName)) {
                                    final File archiveFile = new File(platformDir, archiveFileName);
                                    platformDir.mkdirs();
                                    Files.write(archiveFile.toPath(), archiveFileName.getBytes());
                                    if (!archiveFile.setLastModified(((versionIndex + 1) * 10000) * ((releaseIndex + 1) * 1000))) {
                                        throw new RuntimeException("Failed to set lastModified of file " + releaseDir.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void initConfigsRoot(final File configsRoot) throws IOException {
        final String root = configsRoot.getAbsolutePath();

        final File products = Paths.get(root, "products").toFile();
        final File platforms = Paths.get(root, "platforms").toFile();
        final File tasks = Paths.get(root, "tasks").toFile();
        final File buildProviders = Paths.get(root, "buildProviders").toFile();
        final File platformProviders = Paths.get(root, "platformProviders").toFile();
        final File taskVariantCategories = Paths.get(root, "taskVariants").toFile();
        final File jdkProjects = Paths.get(root, "projects").toFile();

        final List<File> configFiles = Arrays.asList(
                products, platforms, tasks, buildProviders, platformProviders, taskVariantCategories, jdkProjects
        );

        for (final File configFile : configFiles) {
            if (!configFile.mkdir()) {
                throw new RuntimeException("Couldn't create " + configFile.getAbsolutePath());
            }
        }

        final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

        for (final Product product : getProducts()) {
            Utils.writeToFile(
                    Paths.get(products.getAbsolutePath(), product.getId() + ".json"),
                    writer.writeValueAsString(product)
            );
        }

        for (final Platform platform : getPlatforms()) {
            Utils.writeToFile(
                    Paths.get(platforms.getAbsolutePath(), platform.getId() + ".json"),
                    writer.writeValueAsString(platform)
            );
        }

        for (final TaskVariant taskVariant : getTaskVariants()) {
            Utils.writeToFile(
                    Paths.get(taskVariantCategories.getAbsolutePath(), taskVariant.getId() + ".json"),
                    writer.writeValueAsString(taskVariant)
            );
        }

        for (final Task task : getTasks()) {
            Utils.writeToFile(
                    Paths.get(tasks.getAbsolutePath(), task.getId() + ".json"),
                    writer.writeValueAsString(task)
            );
        }

        for (final JDKProject jdkProject : getJDKProjects()) {
            Utils.writeToFile(
                    Paths.get(jdkProjects.getAbsolutePath(), jdkProject.getId() + ".json"),
                    writer.writeValueAsString(jdkProject)
            );
        }

        final Set<BuildProvider> buildProviderSet = getBuildProviders();
        for (final BuildProvider buildProvider : buildProviderSet) {
            Utils.writeToFile(
                    Paths.get(buildProviders.getAbsolutePath(), buildProvider.getId() + ".json"),
                    writer.writeValueAsString(buildProvider)
            );
        }
    }

    public static void initScriptsRoot(final File scriptsRoot) throws IOException {
        final File oTools = Paths.get(scriptsRoot.getAbsolutePath(), "otool").toFile();
        if (!oTools.mkdirs()) {
            throw new RuntimeException("Couldn't create " + oTools.getAbsolutePath());
        }
        final String cloningScript = "#!/bin/sh\n" +
                "JDK_VERSION=$1\n" +
                "SRC_URL=$2\n" +
                "DEST_PATH=$3\n" +
                "sleep 5s\n" +
                "[ \"$SRC_URL\" == \"" + INVALID_PROJECT_URL + "\" ] && exit 1\n" +
                "echo \"$JDK_VERSION\n$SRC_URL\n\" > $DEST_PATH\n";
        Utils.writeToFile(
                Paths.get(oTools.getAbsolutePath(), "clone_repo.sh"),
                cloningScript
        );
    }

    public static FolderHolder initFolders(TemporaryFolder temporaryFolder) throws IOException {
        final File buildsRoot = temporaryFolder.newFolder("builds");
        final File configsRoot = temporaryFolder.newFolder("configs");
        final File scriptsRoot = temporaryFolder.newFolder("scripts");
        initConfigsRoot(configsRoot);
        initScriptsRoot(scriptsRoot);
        folderHolder = new FolderHolder(
                buildsRoot,
                scriptsRoot,
                temporaryFolder.newFolder("repos"),
                temporaryFolder.newFolder("jenkinsJobs"),
                temporaryFolder.newFolder("jenkinsJobArchive"),
                configsRoot
        );
        return folderHolder;
    }

    public static FolderHolder initFolders(File root) throws IOException {
        final String rootPath = root.getAbsolutePath();
        final File buildsRoot = Paths.get(rootPath, "builds").toFile();
        final File configsRoot = Paths.get(rootPath, "configs").toFile();
        final File scriptsRoot = Paths.get(rootPath,"scripts").toFile();
        final File reposRoot = Paths.get(rootPath, "repos").toFile();
        final File jenkinsJobsRoot = Paths.get(rootPath, "jenkinsJobs").toFile();
        final File jenkinsJobArchiveRoot = Paths.get(rootPath, "jenkinsJobArchive").toFile();
        Arrays.asList(
                configsRoot,
                scriptsRoot,
                reposRoot,
                jenkinsJobsRoot,
                jenkinsJobArchiveRoot
        ).forEach(file -> {
            if (!file.mkdir()) {
                throw new RuntimeException("Couldn't create file " + file.getAbsolutePath());
            }
        });
        initConfigsRoot(configsRoot);
        initScriptsRoot(scriptsRoot);
        initBuildsRoot(buildsRoot);
        folderHolder = new FolderHolder(
                buildsRoot,
                scriptsRoot,
                reposRoot,
                jenkinsJobsRoot,
                jenkinsJobArchiveRoot,
                configsRoot
        );
        return folderHolder;
    }

    public static class FolderHolder {
        public final File buildsRoot;
        public final File scriptsRoot;
        public final File reposRoot;
        public final File jenkinsJobsRoot;
        public final File jenkinsJobArchiveRoot;
        public final File configsRoot;

        private FolderHolder(
                File buildsRoot,
                File scriptsRoot,
                File reposRoot,
                File jenkinsJobsRoot,
                File jenkinsJobArchiveRoot,
                File configsRoot
        ) {
            this.buildsRoot = buildsRoot;
            this.scriptsRoot = scriptsRoot;
            this.reposRoot = reposRoot;
            this.jenkinsJobsRoot = jenkinsJobsRoot;
            this.jenkinsJobArchiveRoot = jenkinsJobArchiveRoot;
            this.configsRoot = configsRoot;
        }
    }

    public static AccessibleSettings getSettings(FolderHolder folderHolder) throws MalformedURLException, UnknownHostException {
        return new AccessibleSettings(
                folderHolder.buildsRoot,
                folderHolder.reposRoot,
                folderHolder.configsRoot,
                folderHolder.jenkinsJobsRoot,
                folderHolder.jenkinsJobArchiveRoot,
                folderHolder.scriptsRoot,
                9848,
                9849,
                9826,
                8080,
                8888
        );
    }
}
