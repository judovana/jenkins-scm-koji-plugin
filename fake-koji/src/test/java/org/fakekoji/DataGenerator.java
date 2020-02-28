package org.fakekoji;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.BuildPlatformConfig;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobConfiguration;
import org.fakekoji.jobmanager.model.PlatformConfig;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TaskConfig;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.jobmanager.model.TestJobConfiguration;
import org.fakekoji.jobmanager.model.VariantsConfig;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class DataGenerator {

    public static final String TEST_PROJECT_NAME = "testProject";
    public static final String NEW_PROJECT_NAME = "new_project_name";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_NAME_U = "uName";
    public static final String PROJECT_URL = "https://gitlab.com/fake_jdk/fake_jdk_repo";
    public static final String PROJECT_URL_U = "https://gitlab.com/fake_jdk_u/fake_jdk_repo_u";
    public static final String INVALID_PROJECT_URL = "shttp://gitlab.com/fake_jdk/fake_jdk_repo";

    public static final String JDK_8 = "jdk8";
    public static final String JDK_8_PACKAGE_NAME = "java-1.8.0-openjdk";
    public static final String RANDOM_JDK_8_PACKAGE_NAME = "javarandom-1.8.0-openjdk";
    public static final String JDK_11 = "jdk11";
    public static final String JDK_11_PACKAGE_NAME = "java-11-openjdk";
    public static final String RANDOM_JDK_11_PACKAGE_NAME = "javarandom-11-openjdk";


    public static final String BUILD_PROVIDER_1 = "fakekoji-hydra";
    public static final String BUILD_PROVIDER_2 = "brew-brewhub";

    public static final String VAGRANT = "vagrant";
    public static final String BEAKER = "beaker";

    public static final String RHEL_7_X64 = "el7.x86_64";
    public static final String F_29_X64 = "f29.x86_64";

    public static final String AGENT = "AGENT";
    public static final String LINUX = "linux";
    public static final String IS_RHEL_Z_STREAM = "IS_RHEL_Z_STREAM";
    public static final String TRUE = "true";
    public static final String BUILD = "build";
    public static final String TCK = "tck";
    public static final String JTREG = "jtreg";
    public static final String TCK_AGENT = "tck~agent";

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

    public static final String X_SERVER = "x11";
    public static final String WAYLAND = "wayland";

    public static final String SCP_POLL_SCHEDULE = "H/24 * * * *";

    public static final String BUILD_PROVIDER_1_TOP_URL = "http://hydra.brq.redhat.com:XPORT/RPC2/";
    public static final String BUILD_PROVIDER_1_DOWNLOAD_URL = "http://hydra.brq.redhat.com:DPORT/";
    public static final String BUILD_PROVIDER_2_TOP_URL = "brewtopUrl";
    public static final String BUILD_PROVIDER_2_DOWNLOAD_URL = "brewdownloadUrl";

    private static FolderHolder folderHolder;

    public static JDKVersion getJDKVersion8() {
        return new JDKVersion(
                JDK_8,
                "JDK 8",
                "8",
                Arrays.asList(
                        JDK_8_PACKAGE_NAME,
                        RANDOM_JDK_8_PACKAGE_NAME
                )
        );
    }

    public static JDKVersion getJDKVersion11() {
        return new JDKVersion(
                JDK_11,
                "JDK 11",
                "11",
                Arrays.asList(
                        JDK_11_PACKAGE_NAME,
                        RANDOM_JDK_11_PACKAGE_NAME
                )
        );
    }

    public static Product getJDK8Product() {
        return new Product(JDK_8, JDK_8_PACKAGE_NAME);
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
                "Release",
                Arrays.asList(
                        ".*fastdebug.*", ".*slowdebug.*"
                ),
                Arrays.asList("*release*")
        );
    }

    public static TaskVariantValue getFastdebugVariant() {
        return new TaskVariantValue(
                FASTDEBUG,
                "Fastdebug",
                Arrays.asList(
                        ".*release.*", ".*slowdebug.*"
                ),
                Arrays.asList(".*fastdebug*")
        );
    }

    public static TaskVariantValue getSlowdebugVariant() {
        return new TaskVariantValue(
                SLOWDEBUG,
                "Slowdebug",
                Arrays.asList(
                        ".*fastdebug*", ".*release*"
                ),
                Arrays.asList(".*slowdebug*")
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
                HOTSPOT,
                1,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue hotspot = getHotspotVariant();
                            put(hotspot.getId(), hotspot);
                            final TaskVariantValue zero = getZeroVariant();
                            put(zero.getId(), zero);
                        }}
                ),
                false
        );
    }

    public static TaskVariant getDebugModeVariant() {
        return new TaskVariant(
                DEBUG_MODE,
                "Debug mode",
                Task.Type.BUILD,
                RELEASE,
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
                ),
                true
        );
    }

    public static TaskVariant getGarbageCollectorCategory() {
        return new TaskVariant(
                GARBAGE_COLLECTOR,
                "Garbage Collector",
                Task.Type.TEST,
                SHENANDOAH,
                0,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue shenandoah = getShenandoahVariant();
                            put(shenandoah.getId(), shenandoah);
                            final TaskVariantValue zgc = getZGCVariant();
                            put(zgc.getId(), zgc);
                        }}
                ),
                false
        );
    }

    public static TaskVariant getDisplayProtocolCategory() {
        return new TaskVariant(
                DISPLAY_PROTOCOL,
                "Display protocol",
                Task.Type.TEST,
                X_SERVER,
                1,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            final TaskVariantValue wayland = getWaylandVariant();
                            put(wayland.getId(), wayland);
                            final TaskVariantValue xServer = getXServerVariant();
                            put(xServer.getId(), xServer);
                        }}
                ),
                false
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

    public static Platform.Provider getVMOnlyProvider() {
        return new Platform.Provider(
                "vagrant",
                Collections.emptyList(),
                Arrays.asList("Hydra", "Norn")
        );
    }

    public static Platform.Provider getHWOnlyProvider() {
        return new Platform.Provider(
                "vagrant",
                Arrays.asList("Hydra", "Norn"),
                Collections.emptyList()
                );
    }

    public static Platform.Provider getProvider() {
        return new Platform.Provider(
                "vagrant",
                Arrays.asList("Hydra", "Norn"),
                Arrays.asList("Odin", "Tyr")
        );
    }

    public static Platform getRHEL7x64() {
        return Platform.create(new Platform(
                null,
                "el",
                "7",
                "7",
                "x86_64",
                null,
                Arrays.asList(getProvider()),
                "rhel-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el7-*"),
                Collections.emptyList()
        ));
    }

    public static Platform getRHEL7Zx64() {
        return Platform.create(new Platform(
                null,
                "el",
                "7z",
                "7",
                "x86_64",
                null,
                Arrays.asList(getProvider()),
                "rhel-x64",
                Platform.TestStableYZupdates.True,
                Platform.TestStableYZupdates.False,
                Collections.singletonList("el7-*"),
                Arrays.asList(
                        new OToolVariable(IS_RHEL_Z_STREAM, TRUE)
                )
        ));
    }

    public static Platform getF29x64() {
        return Platform.create(new Platform(
                null,
                "f",
                "29",
                "29",
                "x86_64",
                null,
                Arrays.asList(getVMOnlyProvider()),
                "f29-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("f29-*"),
                Collections.emptyList()
        ));
    }

    public static Platform getRHEL8Aarch64() {
        return Platform.create(new Platform(
                null,
                "el",
                "8",
                "8",
                "aarch64",
                null,
                Arrays.asList(getHWOnlyProvider()),
                "rhel8-aarch64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el8-*"),
                Collections.emptyList()
        ));
    }

    public static Platform getWin2019x64() {
        return Platform.create(new Platform(
                null,
                "win",
                "2019",
                "2019",
                "x86_64",
                "win",
                Arrays.asList(getProvider()),
                "win-2019",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("win2019-*"),
                Collections.emptyList()
        ));
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
                        false,
                        Task.BinaryRequirements.NONE
                ),
                BUILD_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList()
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
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Arrays.asList(
                                "subpackageA",
                                "subpackageB"
                        ),
                        Arrays.asList(
                                "subpackageC",
                                "subpackageD"
                        )
                ),
                Collections.emptyList()
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
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList()
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
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList()
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
                        false,
                        Task.BinaryRequirements.BINARIES
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList()
        );
    }

    public static Task getTCK() {
        return getTestTask();
    }

    public static Task getJTREG() {
        return getTestTaskRequiringSourcesAndBinary();
    }

    public static Task getTCKWithAgent() {
        return new Task(
                TCK_AGENT,
                "/path/test.sh",
                Task.Type.TEST,
                SCP_POLL_SCHEDULE,
                Task.MachinePreference.VM,
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.Limitation<>(Collections.emptyList(), null),
                new Task.FileRequirements(
                        false,
                        false,
                        Task.BinaryRequirements.BINARY
                ),
                TEST_POST_BUILD_TASK,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Arrays.asList(
                        new OToolVariable(AGENT, LINUX)
                )
        );
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

    public static Set<JDKTestProject> getJDKTestProjects() {
        return new HashSet<>(Arrays.asList(
                getJDKTestProject()
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
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                repoState,
                urlValid ? PROJECT_URL : INVALID_PROJECT_URL,
                DataGenerator.getBuildProvidersIds(),

                new JobConfiguration(
                        Collections.unmodifiableList(Arrays.asList(new PlatformConfig(
                                RHEL_7_X64,
                                Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                    put(BUILD, new TaskConfig(
                                            Collections.singletonList(
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, RELEASE),
                                                            Arrays.asList(
                                                                    new PlatformConfig(
                                                                            RHEL_7_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            )
                                                    )
                                            )
                                    ));
                                }}), VAGRANT

                        )))
                ),
                Collections.emptyList()
        );
    }

    public static JDKProject getJDKProjectU() {
        return new JDKProject(
                PROJECT_NAME_U,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                JDKProject.RepoState.CLONED,
                PROJECT_URL_U,
                DataGenerator.getBuildProvidersIds(),
                new JobConfiguration(Collections.unmodifiableList(Arrays.asList(
                        new PlatformConfig(
                                RHEL_7_X64,
                                Collections.unmodifiableMap(
                                        new HashMap<String, TaskConfig>() {{
                                            put(BUILD, new TaskConfig(Arrays.asList(
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, RELEASE),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            RHEL_7_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ),
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, FASTDEBUG),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            RHEL_7_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ),
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, SLOWDEBUG),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            RHEL_7_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ))
                                            ));
                                        }}), VAGRANT

                        ),
                        new PlatformConfig(
                                F_29_X64,
                                Collections.unmodifiableMap(
                                        new HashMap<String, TaskConfig>() {{
                                            put(BUILD, new TaskConfig(Arrays.asList(
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, RELEASE),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            F_29_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ),
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, FASTDEBUG),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            F_29_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ),
                                                    getBuildVariantConfig(
                                                            getBuildVariantsMap(HOTSPOT, SLOWDEBUG),
                                                            Collections.unmodifiableList(Arrays.asList(
                                                                    new PlatformConfig(
                                                                            F_29_X64,
                                                                            Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                                put(TCK, new TaskConfig(
                                                                                        Collections.singletonList(
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                                        )
                                                                                ));
                                                                            }}), VAGRANT
                                                                    )
                                                            ))
                                                    ))
                                            ));
                                        }}), VAGRANT
                        )))
                ),
                Collections.emptyList()
        );
    }

    public static List<String> getSubpackageBlacklist() {
        return Arrays.asList(
                "subpackage1",
                "subpackage2"
        );
    }

    public static List<String> getSubpackageWhitelist() {
        return Arrays.asList(
                "subpackage3",
                "subpackage4"
        );
    }

    public static JDKTestProject getJDKTestProject() {
        return new JDKTestProject(
                TEST_PROJECT_NAME,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                getBuildProvidersIds(),
                getSubpackageBlacklist(),
                getSubpackageWhitelist(),
                new TestJobConfiguration(Collections.unmodifiableList(Arrays.asList(
                        new BuildPlatformConfig(
                                RHEL_7_X64,
                                Collections.unmodifiableList(Arrays.asList(
                                        new VariantsConfig(
                                                Collections.unmodifiableMap(new HashMap<String, String>() {{
                                                    put(DEBUG_MODE, SLOWDEBUG);
                                                }}),
                                                Collections.unmodifiableList(Arrays.asList(
                                                        new PlatformConfig(
                                                                RHEL_7_X64,
                                                                Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                    put(TCK, new TaskConfig(
                                                                            Arrays.asList(
                                                                                    new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER)),
                                                                                    new VariantsConfig(getTestVariantsMap(SHENANDOAH, WAYLAND))
                                                                            )
                                                                    ));
                                                                    put(JTREG, new TaskConfig(
                                                                            Collections.singletonList(
                                                                                    new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER))
                                                                            )
                                                                    ));
                                                                }}), VAGRANT
                                                        ),
                                                        new PlatformConfig(
                                                                F_29_X64,
                                                                Collections.unmodifiableMap(new HashMap<String, TaskConfig>() {{
                                                                    put(TCK, new TaskConfig(
                                                                            Arrays.asList(
                                                                                    new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER)),
                                                                                    new VariantsConfig(getTestVariantsMap(SHENANDOAH, WAYLAND))
                                                                            )
                                                                    ));
                                                                }}), VAGRANT
                                                        )
                                                ))
                                        )
                                ))
                        )
                ))),
                Collections.emptyList()
        );
    }

    private static VariantsConfig getBuildVariantConfig(Map<String, String> map, List<PlatformConfig> platforms) {
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

    public static Set<JDKVersion> getJDKVersions() {
        return new HashSet<>(Arrays.asList(
                getJDKVersion8(),
                getJDKVersion11()
        ));
    }

    public static Set<Platform> getPlatforms() {
        return new HashSet<>(Arrays.asList(
                getRHEL7x64(),
                getRHEL7Zx64(),
                getF29x64(),
                getRHEL8Aarch64(),
                getWin2019x64()
        ));
    }

    public static Set<Task> getTasks() {
        return new HashSet<>(Arrays.asList(
                getBuildTask(),
                getTCK(),
                getTCKWithAgent(),
                getJTREG()
        ));
    }

    public static Job get_pull_jdk8_project() {
        return new PullJob(
                PROJECT_NAME,
                getJDK8Product(),
                getJDKVersion8(),
                folderHolder.reposRoot,
                folderHolder.scriptsRoot,
                Arrays.asList(new OToolVariable("o1", "v1"))
        );
    }

    public static Job get_build_jdk8_project_el7_x86_64_vagrant_hotspot_release() {
        return new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                getJDK8Product(),
                getJDKVersion8(),
                getBuildProviders(),
                getBuildTask(),
                getRHEL7x64(),
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot,
                Arrays.asList(new OToolVariable("o2", "v2"))
        );
    }

    public static Job get_build_jdk8_project_el7_x86_64_vagrant_hotspot_fastdebug() {
        return new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                getJDK8Product(),
                getJDKVersion8(),
                getBuildProviders(),
                getBuildTask(),
                getRHEL7x64(),
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getFastdebugVariant());
                }},
                folderHolder.scriptsRoot,
                Arrays.asList(new OToolVariable("o3", "v3"))
        );
    }

    public static Job get_build_jdk8_project_f29_x86_64_vagrant_hotspot_release() {
        return new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                getJDK8Product(),
                getJDKVersion8(),
                getBuildProviders(),
                getBuildTask(),
                getF29x64(),
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot,
                null
        );
    }

    public static Job get_tck_jdk8_project_el7_x86_64_hotspot_release_el7_x86_64_vagrant_shenandoah_xServer() {
        final Platform platform = getRHEL7x64();
        return new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                getJDK8Product(),
                getJDKVersion8(),
                getBuildProviders(),
                getTCK(),
                platform,
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getGarbageCollectorCategory(), getShenandoahVariant());
                    put(getDisplayProtocolCategory(), getXServerVariant());
                }},
                platform,
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot,
                Arrays.asList(new OToolVariable("o4", "v4"))
        );
    }

    public static Job get_tck_jdk8_project_el7_x86_64_hotspot_release_el7_x86_64_vagrant_zgc_xServer() {
        final Platform platform = getRHEL7x64();
        return new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                getJDK8Product(),
                getJDKVersion8(),
                getBuildProviders(),
                getTCK(),
                platform,
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getGarbageCollectorCategory(), getZGCVariant());
                    put(getDisplayProtocolCategory(), getXServerVariant());
                }},
                platform,
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                }},
                folderHolder.scriptsRoot,
                null
        );
    }

    public static final String SUFFIX = ".tarxz";

    public static final String VERSION_1 = "version1";
    public static final String VERSION_2 = "version2";
    public static final String RELEASE_1 = "release1";
    public static final String RELEASE_2 = "re.l.ease.2";

    public static final List<String> notBuilt = Arrays.asList(
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.fastdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.fastdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.slowdebug.hotspot.f29.x86_64.tarxz"
    );

    public static final String[] versions = new String[]{VERSION_1, VERSION_2};

    public static final String[] releases = new String[]{RELEASE_1, RELEASE_2};

    public static void initBuildsRoot(final File buildsRoot) throws IOException {
        long timeStamp = new Date().getTime();
        final Set<Platform> platforms = DataGenerator.getPlatforms();
        final Set<JDKVersion> jdkVersions = DataGenerator.getJDKVersions();
        for (final JDKProject jdkProject : DataGenerator.getJDKProjects()) {
            final JDKVersion jdkVersion = jdkVersions.stream()
                    .filter(v -> v.getId().equals(jdkProject.getProduct().getJdk()))
                    .findFirst().get();
            final File productDir = new File(buildsRoot, jdkVersion.getPackageNames().get(0));
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
                    final String baseName = jdkVersion.getPackageNames().get(0) + '-' + version + '-' + releaseName + '.';
                    final File srcDir = new File(releaseDir, JenkinsJobTemplateBuilder.SOURCES);
                    srcDir.mkdirs();
                    final String srcName = baseName + JenkinsJobTemplateBuilder.SOURCES + SUFFIX;
                    final File srcFile = new File(srcDir, srcName);
                    Files.write(srcFile.toPath(), srcName.getBytes());
                    if (!srcFile.setLastModified(timeStamp += 60000)) {
                        throw new RuntimeException("Failed to set lastModified of file " + srcFile.getAbsolutePath());
                    }
                    for (final PlatformConfig platformConfig : jdkProject.getJobConfiguration().getPlatforms()) {
                        for (final Map.Entry<String, TaskConfig> buildTaskConfig : platformConfig.getTasks().entrySet()) {
                            for (final VariantsConfig variantsConfig : buildTaskConfig.getValue().getVariants()) {

                                final Platform platform = platforms
                                        .stream()
                                        .filter(p -> p.getId().equals(platformConfig.getId())).findFirst().get();
                                final String archName = variantsConfig.getMap()
                                        .entrySet()
                                        .stream()
                                        .sorted(Comparator.comparing(Map.Entry::getKey))
                                        .map(Map.Entry::getValue)
                                        .collect(Collectors.joining("."))
                                        + '.' + platform.getId();
                                final File platformDir = new File(releaseDir, archName);
                                final String archiveFileName = baseName + archName + SUFFIX;
                                if (!notBuilt.contains(archiveFileName)) {
                                    final File archiveFile = new File(platformDir, archiveFileName);
                                    platformDir.mkdirs();
                                    Files.write(archiveFile.toPath(), archiveFileName.getBytes());
                                    if (!archiveFile.setLastModified(timeStamp += 60000)) {
                                        throw new RuntimeException("Failed to set lastModified of file " + archiveFile.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void initConfigsRoot(final File configsRoot) throws IOException, StorageException {
        final String root = configsRoot.getAbsolutePath();

        final File products = Paths.get(root, ConfigManager.JDK_VERSIONS).toFile();
        final File platforms = Paths.get(root, ConfigManager.PLATFORMS).toFile();
        final File tasks = Paths.get(root, ConfigManager.TASKS).toFile();
        final File buildProviders = Paths.get(root, ConfigManager.BUILD_PROVIDERS).toFile();
        final File taskVariantCategories = Paths.get(root, ConfigManager.TASK_VARIANTS).toFile();
        final File jdkProjects = Paths.get(root, ConfigManager.JDK_PROJECTS).toFile();
        final File jdkTestProjects = Paths.get(root, ConfigManager.JDK_TEST_PROJECTS).toFile();

        final List<File> configFiles = Arrays.asList(
                products,
                platforms,
                tasks,
                buildProviders,
                taskVariantCategories,
                jdkProjects,
                jdkTestProjects
        );

        for (final File configFile : configFiles) {
            if (!configFile.mkdir()) {
                throw new RuntimeException("Couldn't create " + configFile.getAbsolutePath());
            }
        }

        final ConfigManager configManager = ConfigManager.create(configsRoot.getAbsolutePath());

        for (final JDKVersion jdkVersion : getJDKVersions()) {
            configManager.getJdkVersionStorage().store(jdkVersion.getId(), jdkVersion);
        }

        for (final Platform platform : getPlatforms()) {
            configManager.getPlatformStorage().store(platform.getId(), platform);
        }

        for (final TaskVariant taskVariant : getTaskVariants()) {
            configManager.getTaskVariantStorage().store(taskVariant.getId(), taskVariant);
        }

        for (final Task task : getTasks()) {
            configManager.getTaskStorage().store(task.getId(), task);
        }

        for (final JDKProject jdkProject : getJDKProjects()) {
            configManager.getJdkProjectStorage().store(jdkProject.getId(), jdkProject);
        }

        for (final JDKTestProject jdkTestProject : getJDKTestProjects()) {
            configManager.getJdkTestProjectStorage().store(jdkTestProject.getId(), jdkTestProject);
        }

        final Set<BuildProvider> buildProviderSet = getBuildProviders();
        for (final BuildProvider buildProvider : buildProviderSet) {
            configManager.getBuildProviderStorage().store(buildProvider.getId(), buildProvider);
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

    public static FolderHolder initFolders(TemporaryFolder temporaryFolder) throws IOException, StorageException {
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

    public static FolderHolder initFolders(File root) throws IOException, StorageException {
        final String rootPath = root.getAbsolutePath();
        final File buildsRoot = Paths.get(rootPath, "builds").toFile();
        final File configsRoot = Paths.get(rootPath, "configs").toFile();
        final File scriptsRoot = Paths.get(rootPath, "scripts").toFile();
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
