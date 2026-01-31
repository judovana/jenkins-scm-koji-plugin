package org.fakekoji;

import org.apache.commons.io.FileUtils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.SummaryReportRunner;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ConfigManager;
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
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.DESTROY_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JENKINS;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JOB_NAME_SHORTENED;


public class DataGenerator {

    @TempDir
    static Path temporaryFolder;

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    //the CVB projects were added very late in development cycle, and many asserts works with total numbers, so enabling them only on demand
    public static boolean withCvbJobs = false;
    // there are few  (CVB) configs, which have conflicts. Those conflicts may be causing issues randomly, so they are enabled only on sueprdemand
    //note, that only one of those can be enabled, as they all provide same PROJECT_VBC_JP_SUPER, just with different setup of conflicts
    public static boolean withConflictingCvbJobsConflict_Total = false;

    public static final String TEST_PROJECT_NAME = "testProject";
    public static final String NEW_PROJECT_NAME = "new_project_name";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_NAME_U = "uName";
    public static final String PROJECT_VBC_JP = "vagrantBeakerConflictsProjectJdkProject";
    public static final String PROJECT_VBC_JTP = "vagrantBeakerConflictsProjectJdkTestProject";
    public static final String PROJECT_VBC_JP_SUPER = "vagrantBeakerSuperConflictsProjectJdkProject";
    public static final String PROJECT_URL = "https://gitlab.com/fake_jdk/fake_jdk_repo";
    public static final String PROJECT_URL_U = "https://gitlab.com/fake_jdk_u/fake_jdk_repo_u";
    public static final String INVALID_PROJECT_URL = "shttp://gitlab.com/fake_jdk/fake_jdk_repo";

    public static final String JDK_8 = "jdk8";
    public static final String JDK_8_PACKAGE_NAME = "java-1.8.0-openjdk";
    public static final String RANDOM_JDK_8_PACKAGE_NAME = "javarandom-1.8.0-openjdk";
    public static final String JDK_11 = "jdk11";
    public static final String JDK_11_PACKAGE_NAME = "java-11-openjdk";
    public static final String RANDOM_JDK_11_PACKAGE_NAME = "javarandom-11-openjdk";
    public static final String JDK_13 = "jdk13";
    public static final String JDK_13_PACKAGE_NAME = "jdk13";
    public static final String JDK_14 = "jdk14";
    public static final String JDK_14_PACKAGE_NAME = "jdk14";

    public static final String BUILD_PROVIDER_1 = "fakekoji-hydra";
    public static final String BUILD_PROVIDER_2 = "brew-brewhub";

    public static final String VAGRANT = "vagrant";
    public static final String BEAKER = "beaker";

    public static final String RHEL_7_X64 = "el7.x86_64";
    public static final String RHEL_8_X64 = "el8.x86_64";
    public static final String F_29_X64 = "f29.x86_64";
    public static final String F_30_X64 = "f30.x86_64";
    public static final String F_31_X64 = "f31.x86_64";

    public static final String AGENT = "AGENT";
    public static final String LINUX = "linux";
    public static final String IS_RHEL_Z_STREAM = "IS_RHEL_Z_STREAM";
    public static final String TRUE = "true";
    public static final String BUILD = "build";
    public static final String TCK = "tck";
    public static final String DACAPO = "dacapo";
    public static final String LUCENE = "lucene";
    public static final String WILDFLY = "wildfly";
    public static final String CHURN = "churn";
    public static final String JTREG = "jtreg";
    public static final String TCK_AGENT = "tck~agent";

    public static final String JVM = "jvm";

    public static final String HOTSPOT = "hotspot";
    public static final String ZERO = "zero";
    public static final String OPENJ9 = "openj9";

    public static final String DEBUG_MODE = "debugMode";

    public static final String RELEASE = "release";
    public static final String FASTDEBUG = "fastdebug";
    public static final String SLOWDEBUG = "slowdebug";

    public static final String JRE_SDK = "jreSdk";
    public static final String JRE = "jre";
    public static final String SDK = "sdk";
    public static final String JRE_HEADLESS = "jreheadless";

    public static final String CRYPTO = "crypto";
    public static final String LEGACY = "legacy";
    public static final String FUTURE = "future";
    public static final String FIPS = "fips";

    public static final String JFR = "jfr";
    public static final String JFR_ON = "jfron";
    public static final String JFR_OFF = "jfroff";

    public static final String GARBAGE_COLLECTOR = "garbageCollector";

    public static final String DEFAULT_GC = "defaultgc";
    public static final String SHENANDOAH = "shenandoah";
    public static final String ZGC = "zgc";

    public static final String DISPLAY_PROTOCOL = "displayProtocol";

    public static final String X_SERVER = "x11";
    public static final String WAYLAND = "wayland";
    public static final String HEADLESS = "headless";

    public static final String OS_AGENT = "agent";
    public static final String LINUX_AGENT = "lnxagent";
    public static final String WIN_AGENT = "winagent";

    public static final String SCP_POLL_SCHEDULE = "H/24 * * * *";

    public static final String PRODUCTION_URL = "http://hydra.brq.redhat.com";
    public static final String TESTRUN_URL = "http://localhost";
    public static final String USED_URL = setMainUrl();

    private static String setMainUrl() {
        if (System.getProperty("otool.test.url") != null && System.getProperty("otool.test.url").length() > 0) {
            if (System.getProperty("otool.test.url").equals("production")) {
                return PRODUCTION_URL;
            } else {
                System.getProperty("otool.test.url");
            }
        }
        return TESTRUN_URL;
    }

    public static final String BUILD_PROVIDER_1_TOP_URL = USED_URL + ":XPORT/RPC2/";
    public static final String BUILD_PROVIDER_1_DOWNLOAD_URL = USED_URL + ":DPORT/";
    public static final String BUILD_PROVIDER_1_PACKAGE_INFO_URL = USED_URL + ":9849";
    public static final String BUILD_PROVIDER_2_TOP_URL = "brewtopUrl";
    public static final String BUILD_PROVIDER_2_DOWNLOAD_URL = "brewdownloadUrl";
    public static final String BUILD_PROVIDER_2_PACKAGE_INFO_URL = "brewPackageInfoUrl";

    public static final String JENKINS_URL = USED_URL + ":8080/";
    public static final String COMPARE_URL = USED_URL + ":9090/comp.html";

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

    public static JDKVersion getJDKVersion13() {
        return new JDKVersion(
                JDK_13,
                "JDK 13",
                "13",
                Arrays.asList(
                        JDK_13_PACKAGE_NAME
                )
        );
    }

    public static JDKVersion getJDKVersion14() {
        return new JDKVersion(
                JDK_14,
                "JDK 14",
                "14",
                Arrays.asList(
                        JDK_14_PACKAGE_NAME
                )
        );
    }

    public static Product getJDK8Product() {
        return new Product(JDK_8, JDK_8_PACKAGE_NAME);
    }

    public static Product getJDK11Product() {
        return new Product(JDK_11, JDK_11_PACKAGE_NAME);
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
                        BUILD_PROVIDER_1_DOWNLOAD_URL,
                        BUILD_PROVIDER_1_PACKAGE_INFO_URL
                ),
                new BuildProvider(
                        BUILD_PROVIDER_2,
                        "Brew @ Brewhub",
                        BUILD_PROVIDER_2_TOP_URL,
                        BUILD_PROVIDER_2_DOWNLOAD_URL,
                        BUILD_PROVIDER_2_PACKAGE_INFO_URL
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
                            put(OPENJ9, new TaskVariantValue(OPENJ9, OPENJ9));
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
                            put(DEFAULT_GC, new TaskVariantValue(DEFAULT_GC, DEFAULT_GC));
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
                            put(HEADLESS, new TaskVariantValue(HEADLESS, HEADLESS));
                        }}
                ),
                false
        );
    }

    public static TaskVariant getJreSdk() {
        return new TaskVariant(
                JRE_SDK,
                JRE_SDK,
                Task.Type.BUILD,
                SDK,
                2,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            put(SDK, new TaskVariantValue(SDK, SDK));
                            put(JRE, new TaskVariantValue(JRE, JRE));
                            put(JRE_HEADLESS, new TaskVariantValue(JRE_HEADLESS, JRE_HEADLESS));

                        }}
                ),
                true
        );
    }

    public static TaskVariant getCrypto() {
        return new TaskVariant(
                CRYPTO,
                CRYPTO,
                Task.Type.TEST,
                FIPS,
                2,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            put(FIPS, new TaskVariantValue(FIPS, FIPS));
                            put(LEGACY, new TaskVariantValue(LEGACY, LEGACY));
                            put(FUTURE, new TaskVariantValue(FUTURE, FUTURE));

                        }}
                ),
                false
        );
    }

    public static TaskVariant getJfr() {
        return new TaskVariant(
                JFR,
                JFR,
                Task.Type.TEST,
                JFR_ON,
                4,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            put(JFR_ON, new TaskVariantValue(JFR_ON, JFR_ON));
                            put(JFR_OFF, new TaskVariantValue(JFR_OFF, JFR_OFF));
                        }}
                ),
                false
        );
    }

    public static TaskVariant getAgent() {
        return new TaskVariant(
                OS_AGENT,
                OS_AGENT,
                Task.Type.TEST,
                LINUX_AGENT,
                3,
                Collections.unmodifiableMap(
                        new HashMap<String, TaskVariantValue>() {{
                            put(LINUX_AGENT, new TaskVariantValue(LINUX_AGENT, LINUX_AGENT));
                            put(WIN_AGENT, new TaskVariantValue(WIN_AGENT, WIN_AGENT));
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
                getGarbageCollectorCategory(),
                getCrypto(),
                getJreSdk(),
                getAgent(),
                getJfr()
        ));
    }

    private static Platform.Provider getVMOnlyProvider() {
        return new Platform.Provider(
                VAGRANT,
                Collections.emptyList(),
                Arrays.asList("Hydra", "Norn")
        );
    }

    private static Platform.Provider getHWOnlyProvider() {
        return new Platform.Provider(
                VAGRANT,
                Arrays.asList("Hydra", "Norn"),
                Collections.emptyList()
        );
    }

    private static Platform.Provider getVagrantProvider() {
        return new Platform.Provider(
                VAGRANT,
                Arrays.asList("Hydra", "Norn"),
                Arrays.asList("Odin", "Tyr", "os-%{OTOOL_OS_NAME}")
        );
    }

    private static Platform.Provider getBeakerProvider() {
        return new Platform.Provider(
                BEAKER,
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d")
        );
    }

    private static Platform getRHEL6i686() {
        return Platform.create(new Platform(
                null,
                "el",
                "6",
                "6",
                "i686",
                null,
                Arrays.asList(getVagrantProvider(), getBeakerProvider()),
                "rhel-i686",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el6-*"),
                Collections.emptyList()
        ));
    }

    private static Platform getRHEL6x64() {
        return Platform.create(new Platform(
                null,
                "el",
                "6",
                "6",
                "x86_64",
                null,
                Arrays.asList(getVagrantProvider(), getBeakerProvider()),
                "rhel-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el6-*"),
                Collections.emptyList()
        ));
    }

    public static Platform getRHEL7x64() {
        return Platform.create(new Platform(
                null,
                "el",
                "7",
                "7",
                "x86_64",
                null,
                Arrays.asList(getVagrantProvider(), getBeakerProvider()),
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
                Arrays.asList(getVagrantProvider()),
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

    private static Platform getF30x64() {
        return Platform.create(new Platform(
                null,
                "f",
                "30",
                "30",
                "x86_64",
                null,
                Arrays.asList(getVMOnlyProvider()),
                "30-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("f30-*"),
                Collections.emptyList()
        ));
    }

    private static Platform getF31x64() {
        return Platform.create(new Platform(
                null,
                "f",
                "31",
                "31",
                "x86_64",
                null,
                Arrays.asList(getVMOnlyProvider()),
                "f31-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("f31-*"),
                Collections.emptyList()
        ));
    }

    private static Platform getRHEL8X64() {
        return Platform.create(new Platform(
                null,
                "el",
                "8",
                "8",
                "x86_64",
                null,
                Arrays.asList(getHWOnlyProvider()),
                "rhel8-x64",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el8-*"),
                Collections.emptyList()
        ));
    }

    private static Platform getRHEL8i686() {
        return Platform.create(new Platform(
                null,
                "el",
                "8",
                "8",
                "i686",
                null,
                Arrays.asList(getHWOnlyProvider()),
                "rhel8-i686",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("el8-*"),
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
                Arrays.asList(getVagrantProvider()),
                "win-2019",
                Platform.TestStableYZupdates.NaN,
                Platform.TestStableYZupdates.NaN,
                Collections.singletonList("win2019-*"),
                Collections.emptyList()
        ));
    }

    public static Map<TaskVariant, TaskVariantValue> getBuildVariants() {
        final TaskVariant jreSdk = getJreSdk();
        return Collections.unmodifiableMap(
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getJvmVariant(), getHotspotVariant());
                    put(getDebugModeVariant(), getReleaseVariant());
                    put(jreSdk, jreSdk.getVariants().get(jreSdk.getDefaultValue()));
                }}
        );
    }

    public static Map<TaskVariant, TaskVariantValue> getTestVariants() {
        final TaskVariant agent = getAgent();
        final TaskVariant crypto = getCrypto();
        final TaskVariant jfr = getJfr();
        return Collections.unmodifiableMap(
                new HashMap<TaskVariant, TaskVariantValue>() {{
                    put(getGarbageCollectorCategory(), getShenandoahVariant());
                    put(getDisplayProtocolCategory(), getWaylandVariant());
                    put(agent, agent.getVariants().get(agent.getDefaultValue()));
                    put(crypto, crypto.getVariants().get(crypto.getDefaultValue()));
                    put(jfr, jfr.getVariants().get(jfr.getDefaultValue()));
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
                null,
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
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
                "",
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
                Collections.emptyList(),
                0
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
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
                "some weird column",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                10
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
        );
    }

    public static Task getTCK() {
        return getTestTask();
    }

    public static Task getJTREG() {
        return getTestTaskRequiringSourcesAndBinary();
    }

    public static Task getLucene() {
        return new Task(
                LUCENE,
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
        );
    }

    public static Task getWildfly() {
        return new Task(
                WILDFLY,
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
        );
    }

    public static Task getChurn() {
        return new Task(
                CHURN,
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
        );
    }

    public static Task getDacapo() {
        return new Task(
                DACAPO,
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
                "",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Collections.emptyList(),
                0
        );
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
                "another weird column",
                new Task.RpmLimitation(
                        Collections.emptyList(),
                        Collections.emptyList()
                ),
                Arrays.asList(
                        new OToolVariable(AGENT, LINUX)
                ),
                0
        );
    }


    public static String getPostTasks(String scriptsRoot, boolean vm, boolean analyser, String provider, String shortenedName, String vmPlatformName) {
        if (!vm && !analyser) {
            return "";
        }
        String s =
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n";
        if (vm) {
            s = s + getVmShutdownPostTask(scriptsRoot, provider, shortenedName, vmPlatformName);
        }
        if (analyser) {
            s = s + getAnnalyzePostTask(scriptsRoot);
        }
        s = s +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n";
        return s;
    }
    public static String getVmShutdownPostTask(String scriptsRoot, String provider, String shortenedName, String vmPlatformName) {
         return
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;"+getShortenedVar(shortenedName) + " bash " + Paths.get(scriptsRoot, JENKINS, provider, DESTROY_SCRIPT_NAME) + " " + vmPlatformName + "&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n";
    }

    private static String getShortenedVar(String shortenedName) {
        if (shortenedName==null){
            return "";
        } else {
            return "OTOOL_" + JOB_NAME_SHORTENED + "=\"" + shortenedName+"\"";
        }
    }

    public static String getAnnalyzePostTask(String scriptsRoot) {
        return
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>false</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;bash " + scriptsRoot + "/otool/wrappers/analyzeAndReportJenkinsJob.sh&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n";
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
        HashSet<JDKProject> r = new HashSet<>(Arrays.asList(
                getJDKProject(),
                getJDKProjectU()
        ));
        if (withCvbJobs){
            r.add(getJDKProjectCVB());
            if (withConflictingCvbJobsConflict_Total){
                r.add(getJDKProjectCVB_total());
            }
        }
            return r;

    }

    public static Set<JDKTestProject> getJDKTestProjects() {
        HashSet<JDKTestProject> r = new HashSet<>(Arrays.asList(
                getJDKTestProject()
        ));
        if (withCvbJobs){
            r.add(getJDKTestProjectCVB());
        }
        return r;
    }

    public static Set<Project> getProjects() {
        return Stream.of(getJDKTestProjects(), getJDKProjects())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
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
        final Set<TaskConfig> tasks = new HashSet<>(Arrays.asList(
                new TaskConfig(
                        TCK,
                        new HashSet<>(Arrays.asList(
                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, FIPS, JFR_ON))
                        ))
                )
        ));
        return new JDKProject(
                projectName,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                repoState,
                urlValid ? PROJECT_URL : INVALID_PROJECT_URL,
                DataGenerator.getBuildProvidersIds(),

                new JobConfiguration(
                        new HashSet<>(Arrays.asList(new PlatformConfig(
                                RHEL_7_X64,
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(
                                                BUILD,
                                                new HashSet<>(Arrays.asList(
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                                new HashSet<>(Arrays.asList(
                                                                        new PlatformConfig(
                                                                                RHEL_7_X64, tasks, VAGRANT
                                                                        )
                                                                ))
                                                        ),
                                                        getBuildVariantConfig(
                                                                getBuildVariantsMap(ZERO, RELEASE, SDK),
                                                                new HashSet<>(Arrays.asList(
                                                                        new PlatformConfig(
                                                                                RHEL_7_X64, tasks, BEAKER
                                                                        )
                                                                ))
                                                        )
                                                ))
                                        )
                                )), BEAKER

                        )))
                ),
                Collections.emptyList()
        );
    }

    public static JDKProject getJDKProjectU() {
        final  Set<VariantsConfig> testVariants = new HashSet<>(Arrays.asList(
                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, LEGACY, JFR_ON)),
                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, FUTURE, JFR_ON)),
                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, WAYLAND, LINUX_AGENT, LEGACY, JFR_ON)),
                new VariantsConfig(getTestVariantsMap(SHENANDOAH, WAYLAND, LINUX_AGENT, FUTURE, JFR_ON))
        ));
        return new JDKProject(
                PROJECT_NAME_U,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                JDKProject.RepoState.CLONED,
                PROJECT_URL_U,
                DataGenerator.getBuildProvidersIds(),
                new JobConfiguration(new HashSet<>(Arrays.asList(
                        new PlatformConfig(
                                RHEL_7_X64,
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, FASTDEBUG, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, SLOWDEBUG, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        )))
                                        )))
                                )), VAGRANT

                        ),
                        new PlatformConfig(
                                F_29_X64,
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        F_29_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        LUCENE,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                ),
                                                                new PlatformConfig(
                                                                        F_30_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                ),
                                                                new PlatformConfig(
                                                                        F_31_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, FASTDEBUG, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        F_29_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, SLOWDEBUG, SDK),
                                                        new HashSet<>((Arrays.asList(
                                                                new PlatformConfig(
                                                                        F_29_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        testVariants
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        )))
                                                )
                                        )))
                                )), VAGRANT
                        )
                ))),
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
                new TestJobConfiguration(new HashSet<>(Arrays.asList(
                        new BuildPlatformConfig(
                                RHEL_7_X64,
                                new HashSet<>(Arrays.asList(
                                        new VariantsConfig(
                                                Collections.unmodifiableMap(new HashMap<String, String>() {{
                                                    put(DEBUG_MODE, SLOWDEBUG);
                                                    put(JRE_SDK, SDK);
                                                }}),
                                                new HashSet<>(Arrays.asList(
                                                        new PlatformConfig(
                                                                RHEL_7_X64,
                                                                new HashSet<>(Arrays.asList(
                                                                        new TaskConfig(
                                                                                TCK,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, WAYLAND, LINUX_AGENT, FIPS, JFR_ON)),
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, FIPS, JFR_ON))

                                                                                ))
                                                                        ),
                                                                        new TaskConfig(
                                                                                JTREG,
                                                                                new HashSet<>(Collections.singletonList(
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, FIPS, JFR_ON))
                                                                                ))
                                                                        )
                                                                )), VAGRANT
                                                        ),
                                                        new PlatformConfig(
                                                                F_29_X64,
                                                                new HashSet<>(Arrays.asList(
                                                                        new TaskConfig(
                                                                                TCK,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, WAYLAND, LINUX_AGENT, FIPS, JFR_ON)),
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, FIPS, JFR_ON))
                                                                                ))
                                                                        )
                                                                )), VAGRANT
                                                        )
                                                ))
                                        )))
                        )
                ))),
                Collections.emptyList()
        );
    }

    private static VariantsConfig getBuildVariantConfig(Map<String, String> map, Set<PlatformConfig> platforms) {
        return new VariantsConfig(map, platforms);
    }

    private static Map<String, String> getBuildVariantsMap(String jvm, String debugMode, String jreSdk) {
        return Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(JVM, jvm);
            put(DEBUG_MODE, debugMode);
            put(JRE_SDK, jreSdk);
        }});
    }

    private static Map<String, String> getTestVariantsMap(
            String garbageCollector,
            String displayProtocol,
            String agent,
            String crypto,
            String jfr
    ) {
        return Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(GARBAGE_COLLECTOR, garbageCollector);
            put(DISPLAY_PROTOCOL, displayProtocol);
            put(OS_AGENT, agent);
            put(CRYPTO, crypto);
            put(JFR, jfr);
        }});
    }

    public static Set<JDKVersion> getJDKVersions() {
        return new HashSet<>(Arrays.asList(
                getJDKVersion8(),
                getJDKVersion11(),
                getJDKVersion13(),
                getJDKVersion14()
        ));
    }

    public static Set<Platform> getPlatforms() {
        return new HashSet<>(Arrays.asList(
                getRHEL6i686(),
                getRHEL6x64(),
                getRHEL7x64(),
                getRHEL7Zx64(),
                getRHEL8i686(),
                getRHEL8X64(),
                getF29x64(),
                getF30x64(),
                getF31x64(),
                getRHEL8Aarch64(),
                getWin2019x64()
        ));
    }

    public static Set<Task> getTasks() {
        return new HashSet<>(Arrays.asList(
                getBuildTask(),
                getTCK(),
                getTCKWithAgent(),
                getJTREG(),
                getDacapo(),
                getChurn(),
                getLucene(),
                getWildfly()
        ));
    }

    public static Set<Job> getJDKProjectJobs() {
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final JDKVersion jdkVersion = DataGenerator.getJDKVersion8();
        final Platform rhel7x64 = DataGenerator.getRHEL7x64();
        final Task tckTask = DataGenerator.getTCK();
        final Task buildTask = DataGenerator.getBuildTask();
        final File scriptsRoot = folderHolder.scriptsRoot;
        final File repositoriesRoot = folderHolder.reposRoot;
        final Product jdk8 = DataGenerator.getJDK8Product();

        final TaskVariant jreSdk = getJreSdk();
        final TaskVariantValue jreSdkDefault = jreSdk.getVariants().get(jreSdk.getDefaultValue());

        final TaskVariant agent = getAgent();
        final TaskVariantValue agentDefault = agent.getVariants().get(agent.getDefaultValue());

        final TaskVariant crypto = getCrypto();
        final TaskVariantValue cryptoDefault = crypto.getVariants().get(crypto.getDefaultValue());

        final TaskVariant jfr = getJfr();
        final TaskVariantValue jfrDefault = jfr.getVariants().get(jfr.getDefaultValue());

        final PullJob pullJob = new PullJob(
                PROJECT_NAME,
                PROJECT_URL,
                jdk8,
                jdkVersion,
                repositoriesRoot,
                scriptsRoot,
                Collections.emptyList()
        );
        final BuildJob buildJobHotspotRelease = new BuildJob(
                BEAKER,
                PROJECT_NAME,
                jdk8,
                jdkVersion,
                buildProviders,
                buildTask,
                rhel7x64,
                Collections.unmodifiableMap(
                        new HashMap<TaskVariant, TaskVariantValue>() {{
                            put(DataGenerator.getJvmVariant(), DataGenerator.getHotspotVariant());
                            put(DataGenerator.getDebugModeVariant(), DataGenerator.getReleaseVariant());
                            put(jreSdk, jreSdkDefault);
                        }}
                ),
                scriptsRoot,
                Collections.emptyList()
        );
        final BuildJob buildJobZeroRelease = new BuildJob(
                BEAKER,
                PROJECT_NAME,
                jdk8,
                jdkVersion,
                buildProviders,
                buildTask,
                rhel7x64,
                Collections.unmodifiableMap(
                        new HashMap<TaskVariant, TaskVariantValue>() {{
                            put(DataGenerator.getJvmVariant(), DataGenerator.getZeroVariant());
                            put(DataGenerator.getDebugModeVariant(), DataGenerator.getReleaseVariant());
                            put(jreSdk, jreSdkDefault);
                        }}
                ),
                scriptsRoot,
                Collections.emptyList()
        );
        final TestJob testJobHotspotRelease = new TestJob(
                VAGRANT,
                buildJobHotspotRelease,
                tckTask,
                rhel7x64,
                Collections.unmodifiableMap(
                        new HashMap<TaskVariant, TaskVariantValue>() {{
                            put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                            put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getXServerVariant());
                            put(agent, agentDefault);
                            put(crypto, cryptoDefault);
                            put(jfr, jfrDefault);
                        }}
                )
        );
        final TestJob testJobZeroHotspot = new TestJob(
                BEAKER,
                buildJobZeroRelease,
                tckTask,
                rhel7x64,
                Collections.unmodifiableMap(
                        new HashMap<TaskVariant, TaskVariantValue>() {{
                            put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                            put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getXServerVariant());
                            put(agent, agentDefault);
                            put(crypto, cryptoDefault);
                            put(jfr, jfrDefault);
                        }}
                )
        );

        return new HashSet<>(Arrays.asList(
                pullJob,
                buildJobHotspotRelease,
                buildJobZeroRelease,
                testJobHotspotRelease,
                testJobZeroHotspot
        ));
    }

    public static Set<Job> getJDKTestProjectJobs() {
        final JDKTestProject jdkTestProject = DataGenerator.getJDKTestProject();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final JDKVersion jdkVersion = DataGenerator.getJDKVersion8();
        final List<String> blacklist = jdkTestProject.getSubpackageBlacklist();
        final List<String> whitelist = jdkTestProject.getSubpackageWhitelist();
        final Platform rhel7x64 = DataGenerator.getRHEL7x64();
        final Platform f29x64 = DataGenerator.getF29x64();
        final Task tckTask = DataGenerator.getTCK();
        final Task jtregTask = DataGenerator.getJTREG();
        final File scriptsRoot = folderHolder.scriptsRoot;
        final Product jdk8 = DataGenerator.getJDK8Product();

        final TaskVariant jreSdk = getJreSdk();
        final TaskVariantValue jreSdkDefault = jreSdk.getVariants().get(jreSdk.getDefaultValue());

        final TaskVariant agent = getAgent();
        final TaskVariantValue agentDefault = agent.getVariants().get(agent.getDefaultValue());

        final TaskVariant crypto = getCrypto();
        final TaskVariantValue cryptoDefault = crypto.getVariants().get(crypto.getDefaultValue());

        final TaskVariant jfr = getJfr();
        final TaskVariantValue jfrDefault = jfr.getVariants().get(jfr.getDefaultValue());

        return new HashSet<>(Arrays.asList(
                new TestJob(
                        VAGRANT,
                        TEST_PROJECT_NAME,
                        Project.ProjectType.JDK_TEST_PROJECT,
                        jdk8,
                        jdkVersion,
                        buildProviders,
                        tckTask,
                        rhel7x64,
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                                    put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getXServerVariant());
                                    put(agent, agentDefault);
                                    put(crypto, cryptoDefault);
                                    put(jfr, jfrDefault);
                                }}
                        ),
                        rhel7x64,
                        null,
                        new Task(),
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getDebugModeVariant(), DataGenerator.getSlowdebugVariant());
                                    put(jreSdk, jreSdkDefault);
                                }}
                        ),
                        blacklist,
                        whitelist,
                        scriptsRoot,
                        Collections.emptyList()
                ),
                new TestJob(
                        VAGRANT,
                        TEST_PROJECT_NAME,
                        Project.ProjectType.JDK_TEST_PROJECT,
                        jdk8,
                        jdkVersion,
                        buildProviders,
                        tckTask,
                        rhel7x64,
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                                    put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getWaylandVariant());
                                    put(agent, agentDefault);
                                    put(crypto, cryptoDefault);
                                    put(jfr, jfrDefault);
                                }}
                        ),
                        rhel7x64,
                        null,
                        new Task(),
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getDebugModeVariant(), DataGenerator.getSlowdebugVariant());
                                    put(jreSdk, jreSdkDefault);
                                }}
                        ),
                        blacklist,
                        whitelist,
                        scriptsRoot,
                        Collections.emptyList()
                ),
                new TestJob(
                        VAGRANT,
                        TEST_PROJECT_NAME,
                        Project.ProjectType.JDK_TEST_PROJECT,
                        jdk8,
                        jdkVersion,
                        buildProviders,
                        jtregTask,
                        rhel7x64,
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                                    put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getXServerVariant());
                                    put(agent, agentDefault);
                                    put(crypto, cryptoDefault);
                                    put(jfr, jfrDefault);
                                }}
                        ),
                        rhel7x64,
                        null,
                        new Task(),
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getDebugModeVariant(), DataGenerator.getSlowdebugVariant());
                                    put(jreSdk, jreSdkDefault);
                                }}
                        ),
                        blacklist,
                        whitelist,
                        scriptsRoot,
                        Collections.emptyList()
                ),
                new TestJob(
                        VAGRANT,
                        TEST_PROJECT_NAME,
                        Project.ProjectType.JDK_TEST_PROJECT,
                        jdk8,
                        jdkVersion,
                        buildProviders,
                        tckTask,
                        f29x64,
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                                    put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getWaylandVariant());
                                    put(agent, agentDefault);
                                    put(crypto, cryptoDefault);
                                    put(jfr, jfrDefault);
                                }}
                        ),
                        rhel7x64,
                        null,
                        new Task(),
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getDebugModeVariant(), DataGenerator.getSlowdebugVariant());
                                    put(jreSdk, jreSdkDefault);
                                }}
                        ),
                        blacklist,
                        whitelist,
                        scriptsRoot,
                        Collections.emptyList()
                ),
                new TestJob(
                        VAGRANT,
                        TEST_PROJECT_NAME,
                        Project.ProjectType.JDK_TEST_PROJECT,
                        jdk8,
                        jdkVersion,
                        buildProviders,
                        tckTask,
                        f29x64,
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getGarbageCollectorCategory(), DataGenerator.getShenandoahVariant());
                                    put(DataGenerator.getDisplayProtocolCategory(), DataGenerator.getXServerVariant());
                                    put(agent, agentDefault);
                                    put(crypto, cryptoDefault);
                                    put(jfr, jfrDefault);
                                }}
                        ),
                        rhel7x64,
                        null,
                        new Task(),
                        Collections.unmodifiableMap(
                                new HashMap<TaskVariant, TaskVariantValue>() {{
                                    put(DataGenerator.getDebugModeVariant(), DataGenerator.getSlowdebugVariant());
                                    put(jreSdk, jreSdkDefault);
                                }}
                        ),
                        blacklist,
                        whitelist,
                        scriptsRoot,
                        Collections.emptyList()
                )
        ));
    }

    public static final String SUFFIX = ".tarxz";

    public static final String VERSION_1 = "version1";
    public static final String VERSION_2 = "version2";
    public static final String RELEASE_1 = "152";
    public static final String RELEASE_1_BAD = "release2";
    public static final String RELEASE_2 = "2.re.l.ease";
    public static final String RELEASE_2_BAD = "BAD.re.l.ease";

    public static final List<String> notBuilt = Arrays.asList(
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.fastdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.fastdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.slowdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.slowdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.slowdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.slowdebug.hotspot.sdk.f29.x86_64.tarxz",
            "java-1.8.0-openjdk-version2-" + RELEASE_2_BAD + ".uName.slowdebug.hotspot.sdk.f29.x86_64.tarxz"
    );

    public static final String[] versions = new String[]{VERSION_1, VERSION_2};

    public static final String[] releases = new String[]{RELEASE_1, RELEASE_2};

    public static void initBuildsRoot(final File buildsRoot) throws IOException {
        final Map<String, TaskVariant> buildVariants = getBuildVariants()
                .keySet()
                .stream()
                .collect(Collectors.toMap(TaskVariant::getId, key -> key));
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
                    final File dataDir = new File(releaseDir, "data");
                    dataDir.mkdirs();
                    final File logsDir = new File(dataDir, "logs");
                    logsDir.mkdirs();
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
                        for (final TaskConfig buildTaskConfig : platformConfig.getTasks()) {
                            for (final VariantsConfig variantsConfig : buildTaskConfig.getVariants()) {

                                final Platform platform = platforms
                                        .stream()
                                        .filter(p -> p.getId().equals(platformConfig.getId())).findFirst().get();
                                final String archName = variantsConfig.getMap()
                                        .entrySet()
                                        .stream()
                                        .map(entry -> new Tuple<>(buildVariants.get(entry.getKey()), entry.getValue()))
                                        .sorted(Comparator.comparing(tuple -> tuple.x))
                                        .map(tuple -> tuple.y)
                                        .collect(Collectors.joining("."))
                                        + '.' + platform.getId();
                                final File platformDir = new File(releaseDir, archName);
                                final String archiveFileName = baseName + archName + SUFFIX;
                                if (!notBuilt.contains(archiveFileName)) {
                                    final File archiveFile = new File(platformDir, archiveFileName);
                                    platformDir.mkdirs();
                                    final File archiveLogsDir = new File(logsDir, archName);
                                    archiveLogsDir.mkdirs();
                                    final File log = new File(archiveLogsDir, "log");
                                    log.createNewFile();
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

    public static void initConfigsRoot(final AccessibleSettings settings) {
        final String root = settings.getConfigRoot().getAbsolutePath();

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

        try {
            final ConfigManager configManager = settings.getConfigManager();

            for (final JDKVersion jdkVersion : getJDKVersions()) {
                configManager.jdkVersionManager.create(jdkVersion);
            }

            for (final Platform platform : getPlatforms()) {
                configManager.platformManager.create(platform);
            }

            for (final TaskVariant taskVariant : getTaskVariants()) {
                configManager.taskVariantManager.create(taskVariant);
            }

            for (final Task task : getTasks()) {
                configManager.taskManager.create(task);
            }

            for (final JDKProject jdkProject : getJDKProjects()) {
                configManager.jdkProjectManager.create(jdkProject);
            }

            for (final JDKTestProject jdkTestProject : getJDKTestProjects()) {
                configManager.jdkTestProjectManager.create(jdkTestProject);
            }

            final Set<BuildProvider> buildProviderSet = getBuildProviders();
            for (final BuildProvider buildProvider : buildProviderSet) {
                configManager.buildProviderManager.create(buildProvider);
            }
        } catch (StorageException | ManagementException e) {
            throw new RuntimeException(e);
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

    public static FolderHolder initFoldersFromTmpFolder(File temporaryFolder) throws IOException {
        File[] filesToClean = new File[] {
                new File(temporaryFolder, "builds"),
                new File(temporaryFolder, "scripts"),
                new File(temporaryFolder, "configs"),
                //?
                new File(temporaryFolder, "repos"),
                new File(temporaryFolder, "jenkinsJobs"),
                new File(temporaryFolder, "jenkinsJobArchive")
        };
        for (File file: filesToClean) {
            FileUtils.deleteDirectory(file);
        }
        for (File file: filesToClean) {
            file.mkdir();
        }
        initScriptsRoot(filesToClean[1]);
        folderHolder = new FolderHolder(
                filesToClean[0],
                filesToClean[1],
                filesToClean[3],
                filesToClean[4],
                filesToClean[5],
                filesToClean[2]
        );
        initConfigsRoot(getSettings(folderHolder));
        return folderHolder;
    }

    public static FolderHolder initFoldersOnFileRoot(Path temporaryFolder) throws IOException {
        File root = temporaryFolder.toFile();
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
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!file.mkdir()) {
                throw new RuntimeException("Couldn't create dir " + file.getAbsolutePath());
            }
        });
        initScriptsRoot(scriptsRoot);
        initBuildsRoot(buildsRoot);
        File usedJenkins=jenkinsJobsRoot;
        if (System.getProperty("otool.test.jenkins.dir") != null) {
            File futureJenkins=new File(System.getProperty("otool.test.jenkins.dir"));
            if (!futureJenkins.exists() || !futureJenkins.isDirectory()) {
                throw new IOException("Set otool.test.jenkins.dir do not exists or is file: " + futureJenkins.getAbsolutePath());
            }
            LOGGER.severe("Test run points to real jenkins home: " + futureJenkins.getAbsolutePath() + " - that is VERY DANGEROUS");
            usedJenkins=futureJenkins;
        }
        folderHolder = new FolderHolder(
                buildsRoot,
                scriptsRoot,
                reposRoot,
                usedJenkins,
                jenkinsJobArchiveRoot,
                configsRoot
        );
        initConfigsRoot(getSettings(folderHolder));
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

    public static void createProjectJobs(final AccessibleSettings settings) {
        getProjects().forEach(jdkProject -> {
            try {
                settings.getJobUpdater().update(null, jdkProject);
            } catch (StorageException | ManagementException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static SummaryReportRunner getSummaryReportRunner(final AccessibleSettings settings) {
        return new SummaryReportRunner(
                settings,
                "NVR",
                "TIME",
                "CHART_DIR",
                Optional.empty(),
                new String[0]
        ) {
            @Override
            public Result<SummaryReportRunner.SummaryreportResults, String> getSummaryReport() {
                return Result.ok(new SummaryreportResults("report", new HashMap<>(), new File("fake_report_logs_file")));
            }

            @Override
            public int getJobReportSummary(final String jobName) {
                return 3;
            }
        };
    }
    
    public static AccessibleSettings getSettings(Path tmpf) throws IOException {
        return getSettings(initFoldersFromTmpFolder(tmpf.toFile()));
    }

    private static Integer getNullableInt(String prop) {
        String s = System.getProperty(prop);
        if (s == null || s.equals("null")) {
            return null;
        }
        return Integer.valueOf(s);
    }

    private static String getNullableString(String prop) {
        String s = System.getProperty(prop);
        if (s == null || s.equals("null")) {
            return null;
        }
        return s;
    }

    public static AccessibleSettings getSettings(FolderHolder folderHolder) throws MalformedURLException {
        return new AccessibleSettings(
                folderHolder.buildsRoot,
                folderHolder.reposRoot,
                folderHolder.configsRoot,
                folderHolder.jenkinsJobsRoot,
                folderHolder.jenkinsJobArchiveRoot,
                folderHolder.scriptsRoot,
                new URL(JENKINS_URL),
                getNullableString("jenkins.test.ssh.host"),
                getNullableInt("jenkins.test.ssh.port"),
                getNullableString("jenkins.test.ssh.user"),
                getNullableString("jenkins.test.ssh.keypath"),
                new URL(COMPARE_URL),
                JavaServerConstants.DFAULT_RP2C_PORT,
                JavaServerConstants.DFAULT_DWNLD_PORT,
                JavaServerConstants.DFAULT_SCP_PORT,
                JavaServerConstants.DFAULT_WEBAP_PORT,
                Collections.unmodifiableList(new ArrayList<>()),
                Collections.unmodifiableList(new ArrayList<>())
        );
    }

    /**
     * To test beaker bump, we have to:
     *  build platform with no conflicts bumps, following tests are unchanged
     *  test platform with no conflicts bumps
     *
     *  build platform with conflicts do not bumps
     *  test platform with conflicts do not bumps
     *
     *  just for fun: build platform with no conflicts bumps, causing test conflicts do what?
     *
     * notes:
     *   two build jobs , each with different build platform provider are valid
     *   each can have its tree of tests
     *   two build jobs , each with  same build platform provider are valid
     *   each can have its tree of tests, however the belonging is not deterministic
     *   three of above four cases may lead to job naming conflict, as tests do not contain build platform provider, and this is not in otool's hands. no teven in verification steps
     *
     * impotant cornercase is to bump project, with platfrom, without given provider. This should lead to validation error (both build and run)
     * @return
     */

    public static JDKProject getJDKProjectCVB_total() {
        return new JDKProject(
                PROJECT_VBC_JP_SUPER,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                JDKProject.RepoState.CLONED,
                PROJECT_URL_U,
                DataGenerator.getBuildProvidersIds(),
                new JobConfiguration(new HashSet<>(Arrays.asList(
                        new PlatformConfig(
                                RHEL_7_X64, //beaker, vagrant, conflict on build level, thus conflicting on all 3 jobs during generation
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)), //conflicting already on generation level, will super conflict  after generation
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, FASTDEBUG, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)),
                                                                                                new VariantsConfig(getTestVariantsMap(ZGC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), BEAKER
                                                                )
                                                        ))
                                                )
                                        )))
                                )), VAGRANT

                        ),
                        new PlatformConfig(
                                RHEL_7_X64, //beaker, vagrant, conflicting on build level
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)),
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, FASTDEBUG, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        RHEL_7_X64,
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)),
                                                                                                new VariantsConfig(getTestVariantsMap(ZGC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), BEAKER
                                                                )
                                                        ))
                                                )
                                        )))
                                )), BEAKER
                        )))),
                Collections.emptyList());
    }

    public static JDKProject getJDKProjectCVB() {
        return new JDKProject(
                PROJECT_VBC_JP,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                JDKProject.RepoState.CLONED,
                PROJECT_URL_U,
                DataGenerator.getBuildProvidersIds(),
                new JobConfiguration(new HashSet<>(Arrays.asList(
                        new PlatformConfig(
                                RHEL_7_X64, //beaker, vagrant, conflict on build level
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>()
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, FASTDEBUG, SDK),
                                                        new HashSet<>()
                                                )
                                        )))
                                )), VAGRANT

                        ),
                        new PlatformConfig(
                                RHEL_7_X64,
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>()
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, SLOWDEBUG, SDK),
                                                        new HashSet<>()
                                                )
                                        )))
                                )), BEAKER
                        ),
                        new PlatformConfig(
                                getRHEL6x64().getId(), // //for testing conflict on test level (not triggering build conflict ) [but that is possibel too)
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        getRHEL6x64().getId(),
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)), //will conflict on job level
                                                                                                new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                ),
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        getRHEL6x64().getId(),
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)),//conflciting on job level
                                                                                                new VariantsConfig(getTestVariantsMap(ZGC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), BEAKER
                                                                )
                                                        ))
                                                )
                                        )))
                                )), VAGRANT
                        ),
                        new PlatformConfig(
                                getWin2019x64().getId(), //no beaker
                                new HashSet<>(Arrays.asList(
                                        new TaskConfig(BUILD, new HashSet<>(Arrays.asList(
                                                getBuildVariantConfig(
                                                        getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                        new HashSet<>(Arrays.asList(
                                                                new PlatformConfig(
                                                                        getWin2019x64().getId(), //o beaker
                                                                        new HashSet<>(Arrays.asList(
                                                                                new TaskConfig(
                                                                                        DACAPO,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                ),
                                                                                new TaskConfig(
                                                                                        TCK,
                                                                                        new HashSet<>(Arrays.asList(
                                                                                                new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                        ))
                                                                                )
                                                                        )), VAGRANT
                                                                )
                                                        ))
                                                )
                                        )))
                                )), VAGRANT
                        )
                ))),
                Collections.emptyList()
        );
    }

    /**
     * With testproject, the issue described at getJDKProjectCVB should not happen
     */
    public static JDKTestProject getJDKTestProjectCVB() {
        return new JDKTestProject(
                PROJECT_VBC_JTP,
                new Product(JDK_8, JDK_8_PACKAGE_NAME),
                getBuildProvidersIds(),
                getSubpackageBlacklist(),
                getSubpackageWhitelist(),
                new TestJobConfiguration(new HashSet<>(Arrays.asList(
                          new BuildPlatformConfig(
                                getRHEL6x64().getId(), //beaker+vagrant, no conflict on build level
                                new HashSet<>(Arrays.asList(
                                        getBuildVariantConfig(
                                                getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                new HashSet<>(Arrays.asList(
                                                        new PlatformConfig(
                                                                getRHEL6x64().getId(),
                                                                new HashSet<>(Arrays.asList(
                                                                        new TaskConfig(
                                                                                TCK,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)), //will conflict on job level
                                                                                        new VariantsConfig(getTestVariantsMap(SHENANDOAH, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                ))
                                                                        )
                                                                )), VAGRANT
                                                        )
                                                ))
                                        ),
                                        getBuildVariantConfig(
                                                getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                new HashSet<>(Arrays.asList(
                                                        new PlatformConfig(
                                                                getRHEL6x64().getId(),
                                                                new HashSet<>(Arrays.asList(
                                                                        new TaskConfig(
                                                                                TCK,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF)),//conflciting on job level
                                                                                        new VariantsConfig(getTestVariantsMap(ZGC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                ))
                                                                        )
                                                                )), BEAKER
                                                        )
                                                ))
                                        )
                                ))
                        ),
                        new BuildPlatformConfig(
                                getWin2019x64().getId(), //no beaker
                                new HashSet<>(Arrays.asList(
                                        getBuildVariantConfig(
                                                getBuildVariantsMap(HOTSPOT, RELEASE, SDK),
                                                new HashSet<>(Arrays.asList(
                                                        new PlatformConfig(
                                                                getWin2019x64().getId(), //o beaker
                                                                new HashSet<>(Arrays.asList(
                                                                        new TaskConfig(
                                                                                TCK,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                ))
                                                                        ),
                                                                        new TaskConfig(
                                                                                DACAPO,
                                                                                new HashSet<>(Arrays.asList(
                                                                                        new VariantsConfig(getTestVariantsMap(DEFAULT_GC, X_SERVER, LINUX_AGENT, LEGACY, JFR_OFF))
                                                                                ))
                                                                        )
                                                                )), VAGRANT
                                                        )
                                                ))
                                        )
                                ))
                        )
                ))),
                Collections.emptyList());

    }
}
