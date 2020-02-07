package org.fakekoji.jobmanager;

import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_TOP_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_TOP_URL;
import static org.fakekoji.DataGenerator.HOTSPOT;
import static org.fakekoji.DataGenerator.PROJECT_NAME;
import static org.fakekoji.DataGenerator.RELEASE;
import static org.fakekoji.DataGenerator.SCP_POLL_SCHEDULE;
import static org.fakekoji.DataGenerator.TEST_PROJECT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.DESTROY_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JENKINS;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.FAKEKOJI_XML_RPC_API_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.KOJI_XML_RPC_API_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.VM_POST_BUILD_TASK_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.LOCAL;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.NO_CHANGE_RETURN_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.OTOOL_BASH_VAR_PREFIX;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.O_TOOL;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PACKAGE_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_PATH_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PULL_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.RUN_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.VAGRANT;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_NEW_LINE;

public class JenkinsJobTemplateBuilderTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File scriptsRoot;

    @Before
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.newFolder("scripts");
    }

    public static final String SHRT_NM = "ShrtNm";
    public static final String LONG_NAME = "Long-Name";

    private static final NamesProvider dummyNamesProvider = new NamesProvider() {
        @Override
        public String getName() {
            return LONG_NAME;
        }

        @Override
        public String getShortName() {
            return SHRT_NM;
        }
    };

    private static final String BUILD_PROVIDERS_TEMPLATE = "        <kojiBuildProviders class=\"list\">\n" +
            "            <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "                <buildProvider>\n" +
            "                    <topUrl>" + BUILD_PROVIDER_1_TOP_URL + "</topUrl>\n" +
            "                    <downloadUrl>" + BUILD_PROVIDER_1_DOWNLOAD_URL + "</downloadUrl>\n" +
            "                </buildProvider>\n" +
            "            </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "            <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "                <buildProvider>\n" +
            "                    <topUrl>" + BUILD_PROVIDER_2_TOP_URL + "</topUrl>\n" +
            "                    <downloadUrl>" + BUILD_PROVIDER_2_DOWNLOAD_URL + "</downloadUrl>\n" +
            "                </buildProvider>\n" +
            "            </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "        </kojiBuildProviders>\n";

    @Test
    public void buildBuildProvidersTemplate() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Object[] buildProviderArray = buildProviders.toArray();
        final BuildProvider buildProvider1 = (BuildProvider) buildProviderArray[0];
        final BuildProvider buildProvider2 = (BuildProvider) buildProviderArray[1];

        final String expectedTemplate = "<kojiBuildProviders class=\"list\">\n" +
                "    <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
                "        <buildProvider>\n" +
                "            <topUrl>" + buildProvider1.getTopUrl() + "</topUrl>\n" +
                "            <downloadUrl>" + buildProvider1.getDownloadUrl() + "</downloadUrl>\n" +
                "        </buildProvider>\n" +
                "    </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
                "    <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
                "        <buildProvider>\n" +
                "            <topUrl>" + buildProvider2.getTopUrl() + "</topUrl>\n" +
                "            <downloadUrl>" + buildProvider2.getDownloadUrl() + "</downloadUrl>\n" +
                "        </buildProvider>\n" +
                "    </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
                "</kojiBuildProviders>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.BUILD_PROVIDERS, dummyNamesProvider)
                .buildBuildProvidersTemplate(buildProviders).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildKojiXmlRpcApiTemplate() throws IOException {

        final Platform vmPlatform = DataGenerator.getF29x64();
        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();

        final String expectedTemplate = "<kojiXmlRpcApi class=\"hudson.plugins.scm.koji.RealKojiXmlRpcApi\">\n" +
                "    <xmlRpcApiType>REAL_KOJI</xmlRpcApiType>\n" +
                "    <packageName>" + jdk8.getPackageNames().get(0) + "</packageName>\n" +
                "    <arch>" + vmPlatform.getArchitecture() + "</arch>\n" +
                "    <tag>" + String.join(" ", vmPlatform.getTags()) + "</tag>\n" +
                "    <subpackageBlacklist>a b</subpackageBlacklist>\n" +
                "    <subpackageWhitelist>c d</subpackageWhitelist>\n" +
                "</kojiXmlRpcApi>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(KOJI_XML_RPC_API_TEMPLATE), dummyNamesProvider)
                .buildKojiXmlRpcApiTemplate(
                        jdk8.getPackageNames().get(0),
                        vmPlatform,
                        Arrays.asList("a", "b"),
                        Arrays.asList("c", "d")
                ).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildKojiXmlRpcApiTemplateWithWindows() throws IOException {

        final Platform win = DataGenerator.getWin2019x64();
        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();

        final String expectedTemplate = "<kojiXmlRpcApi class=\"hudson.plugins.scm.koji.RealKojiXmlRpcApi\">\n" +
                "    <xmlRpcApiType>REAL_KOJI</xmlRpcApiType>\n" +
                "    <packageName>" + jdk8.getPackageNames().get(0) + "</packageName>\n" +
                "    <arch>" + win.getKojiArch().get() + "</arch>\n" +
                "    <tag>" + String.join(" ", win.getTags()) + "</tag>\n" +
                "    <subpackageBlacklist>a b</subpackageBlacklist>\n" +
                "    <subpackageWhitelist>c d</subpackageWhitelist>\n" +
                "</kojiXmlRpcApi>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(KOJI_XML_RPC_API_TEMPLATE), dummyNamesProvider)
                .buildKojiXmlRpcApiTemplate(
                        jdk8.getPackageNames().get(0),
                        win,
                        Arrays.asList("a", "b"),
                        Arrays.asList("c", "d")
                ).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildFakeKojiXmlRpcApiTemplate() throws IOException {

        final Platform vmPlatform = DataGenerator.getF29x64();
        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();

        final String expectedTemplate = "<kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "    <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "    <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "    <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "    <buildPlatform>" + vmPlatform.getId() + "</buildPlatform>\n" +
                "    <isBuilt>false</isBuilt>\n" +
                "</kojiXmlRpcApi>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(FAKEKOJI_XML_RPC_API_TEMPLATE), dummyNamesProvider)
                .buildFakeKojiXmlRpcApiTemplate(
                        PROJECT_NAME,
                        buildVariants,
                        vmPlatform.getId(),
                        false
                ).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildVmPostBuildTaskTemplate() throws IOException {

        final Platform vmPlatform = DataGenerator.getF29x64();

        final String expectedTemplate = "<hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "    <tasks>\n" +
                "        <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            <logTexts>\n" +
                "                <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    <logText>.*</logText>\n" +
                "                    <operator>OR</operator>\n" +
                "                </hudson.plugins.postbuildtask.LogProperties>\n" +
                "            </logTexts>\n" +
                "            <EscalateStatus>true</EscalateStatus>\n" +
                "            <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "            <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + vmPlatform.getVmName() + "&#13;</script>\n" +
                "        </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "    </tasks>\n" +
                "</hudson.plugins.postbuildtask.PostbuildTask>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(VM_POST_BUILD_TASK_TEMPLATE), dummyNamesProvider)
                .buildVmPostBuildTaskTemplate(VAGRANT, vmPlatform.getVmName(), scriptsRoot).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildBuildJobTemplateWithVmPlatform() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task buildTask = DataGenerator.getBuildTask();
        final Platform vmPlatform = DataGenerator.getF29x64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();

        final BuildJob buildJob = new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                buildTask,
                vmPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>" + "buildPlatform=" + vmPlatform.getId() + " debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId()
                + "</buildVariants>\n" +
                "            <buildPlatform>src</buildPlatform>\n" +
                "            <isBuilt>" + false + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", vmPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + SCP_POLL_SCHEDULE + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + vmPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + buildJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + buildJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + vmPlatform.getOs() + '.' + vmPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + vmPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + vmPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + vmPlatform.getVmName() + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + buildTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n" +
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + vmPlatform.getVmName()
                + "&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                DataGenerator.BUILD_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = buildJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildBuildJobTemplateWithHwPlatform() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task buildTask = DataGenerator.getBuildTask();
        final Platform hwPlatform = DataGenerator.getRHEL8Aarch64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();

        final BuildJob buildJob = new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                buildTask,
                hwPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>buildPlatform=" + hwPlatform.getId() + " debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId()
                + "</buildVariants>\n" +
                "            <buildPlatform>src</buildPlatform>\n" +
                "            <isBuilt>" + false + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", hwPlatform.getProviders().get(0).getHwNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + buildTask.getScmPollSchedule() + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + hwPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + buildJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + buildJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + hwPlatform.getOs() + '.' + hwPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + hwPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + hwPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + LOCAL + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + buildTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.BUILD_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = buildJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateWithHwPlatform() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getRHEL8Aarch64();
        final Platform testPlatform = DataGenerator.getRHEL8Aarch64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
        final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
        final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>" + buildPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", testPlatform.getProviders().get(0).getHwNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + testTask.getScmPollSchedule() + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + testPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + testJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + testJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + testPlatform.getOs() + '.' + testPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + testPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + buildPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + LOCAL + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_displayProtocol=" + testVariants.get(displayProtocol).getId() + XML_NEW_LINE +
                "export OTOOL_garbageCollector=" + testVariants.get(garbageCollector).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateWithVmPlatform() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getRHEL7x64();
        final Platform testPlatform = DataGenerator.getRHEL7x64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
        final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
        final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>" + testPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + testTask.getScmPollSchedule() + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + testPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + testJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + testJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + testPlatform.getOs() + '.' + testPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + testPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + testPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + testPlatform.getVmName() + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_displayProtocol=" + testVariants.get(displayProtocol).getId() + XML_NEW_LINE +
                "export OTOOL_garbageCollector=" + testVariants.get(garbageCollector).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n" +
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + testPlatform.getVmName()
                + "&#13;</script>\n" + "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateRequestingSourcesAndBinary() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTaskRequiringSourcesAndBinary();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
        final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
        final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>src " + testPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + testTask.getScmPollSchedule() + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + testPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + testJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + testJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + testPlatform.getOs() + '.' + testPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + testPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + testPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + testPlatform.getVmName() + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_displayProtocol=" + testVariants.get(displayProtocol).getId() + XML_NEW_LINE +
                "export OTOOL_garbageCollector=" + testVariants.get(garbageCollector).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n" +
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + testPlatform.getVmName()
                + "&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateRequestingSourcesAndBinaries() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTaskRequiringSourcesAndBinaries();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TaskVariant jvm = DataGenerator.getJvmVariant();
        final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
        final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
        final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();

        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                BUILD_PROVIDERS_TEMPLATE +
                "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.FakeKojiXmlRpcApi\">\n" +
                "            <xmlRpcApiType>FAKE_KOJI</xmlRpcApiType>\n" +
                "            <projectName>" + PROJECT_NAME + "</projectName>\n" +
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform/>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join(" ", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>" + testTask.getScmPollSchedule() + "</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=" + testPlatform.getArchitecture() + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=" + testJob.getName() + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=" + testJob.getShortName() + XML_NEW_LINE +
                "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                "export OTOOL_OS=" + testPlatform.getOs() + '.' + testPlatform.getVersion() + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=" + testPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=" + PROJECT_NAME + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=" + RELEASE + '.' + HOTSPOT + '.' + testPlatform.getId() + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=" + testPlatform.getVmName() + XML_NEW_LINE +
                "export OTOOL_debugMode=" + buildVariants.get(debugMode).getId() + XML_NEW_LINE +
                "export OTOOL_displayProtocol=" + testVariants.get(displayProtocol).getId() + XML_NEW_LINE +
                "export OTOOL_garbageCollector=" + testVariants.get(garbageCollector).getId() + XML_NEW_LINE +
                "export OTOOL_jvm=" + buildVariants.get(jvm).getId() + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n" +
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + testPlatform.getVmName()
                + "&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildPullJobTemplate() throws IOException {
        final File reposRoot = temporaryFolder.newFolder("repos");
        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.scm.NullSCM\"/>\n" +
                "    <assignedNode>Hydra</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers>\n" +
                "        <hudson.triggers.SCMTrigger>\n" +
                "            <spec>1 1 1 12 *</spec>\n" +
                "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                "        </hudson.triggers.SCMTrigger>\n" +
                "    </triggers>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>" +
                "#!/bin/sh" + XML_NEW_LINE +
                "#export " + NO_CHANGE_RETURN_VAR + "=-1 # any negative is enforcing pull even without changes detected" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + JDK_VERSION_VAR + "=" + jdk8.getVersion() + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PACKAGE_NAME_VAR + "=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PROJECT_NAME_VAR + "=" + PROJECT_NAME + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PROJECT_PATH_VAR + "=" + Paths.get(reposRoot.getAbsolutePath(), PROJECT_NAME) + XML_NEW_LINE +
                "bash '" + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, PULL_SCRIPT_NAME) + "'" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.tasks.ArtifactArchiver>\n" +
                "            <artifacts>**</artifacts>\n" +
                "            <allowEmptyArchive>false</allowEmptyArchive>\n" +
                "            <onlyIfSuccessful>false</onlyIfSuccessful>\n" +
                "            <fingerprint>false</fingerprint>\n" +
                "            <defaultExcludes>true</defaultExcludes>\n" +
                "            <caseSensitive>true</caseSensitive>\n" +
                "        </hudson.tasks.ArtifactArchiver>\n" +
                "        <hudson.plugins.textfinder.TextFinderPublisher plugin=\"text-finder@1.10.3\">\n" +
                "            <primaryTextFinder>\n" +
                "                <regexp>### CHANGES DETECTED ###</regexp>\n" +
                "                <succeedIfFound>false</succeedIfFound>\n" +
                "                <unstableIfFound>true</unstableIfFound>\n" +
                "                <notBuiltIfFound>false</notBuiltIfFound>\n" +
                "                <alsoCheckConsoleOutput>true</alsoCheckConsoleOutput>\n" +
                "            </primaryTextFinder>\n" +
                "            <additionalTextFinders/>\n" +
                "        </hudson.plugins.textfinder.TextFinderPublisher>\n" +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final PullJob pullJob = new PullJob(
                PROJECT_NAME,
                DataGenerator.getJDK8Product(),
                jdk8,
                reposRoot,
                scriptsRoot
        );

        final String actualTemplate = pullJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateOfJDKTestProject() throws IOException {

        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
        final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();

        final Map<TaskVariant, TaskVariantValue> buildVariants = Collections.emptyMap();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final List<String> blacklist = Arrays.asList(
                "pckgA", "pckgB"
        );
        final List<String> whitelist = Arrays.asList(
                "pckgC", "pckgD"
        );

        final TestJob testJob = new TestJob(
                VAGRANT,
                TEST_PROJECT_NAME,
                Project.ProjectType.JDK_TEST_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                blacklist,
                whitelist,
                scriptsRoot
        );

        final List<String> expectedBlacklist = new ArrayList<>();
        expectedBlacklist.addAll(blacklist);
        expectedBlacklist.addAll(testTask.getRpmLimitation().getBlacklist());
        final List<String> expectedWhitelist = new ArrayList<>();
        expectedWhitelist.addAll(whitelist);
        expectedWhitelist.addAll(testTask.getRpmLimitation().getWhitelist());

        final String expectedTemplate =
                "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                        "<project>\n" +
                        "    <actions/>\n" +
                        "    <description/>\n" +
                        "    <keepDependencies>false</keepDependencies>\n" +
                        "    <properties/>\n" +
                        "    <scm class=\"hudson.plugins.scm.koji.KojiSCM\" plugin=\"jenkins-scm-koji-plugin@0.2-SNAPSHOT\">\n" +
                        BUILD_PROVIDERS_TEMPLATE +
                        "        <kojiXmlRpcApi class=\"hudson.plugins.scm.koji.RealKojiXmlRpcApi\">\n" +
                        "            <xmlRpcApiType>REAL_KOJI</xmlRpcApiType>\n" +
                        "            <packageName>" + jdk8.getPackageNames().get(0) + "</packageName>\n" +
                        "            <arch>" + buildPlatform.getArchitecture() + "</arch>\n" +
                        "            <tag>" + String.join(" ", buildPlatform.getTags()) + "</tag>\n" +
                        "            <subpackageBlacklist>" + String.join(" ", expectedBlacklist) + "</subpackageBlacklist>\n" +
                        "            <subpackageWhitelist>" + String.join(" ", expectedWhitelist) + "</subpackageWhitelist>\n" +
                        "        </kojiXmlRpcApi>\n" +
                        "        <downloadDir>rpms</downloadDir>\n" +
                        "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                        "        <dirPerNvr>false</dirPerNvr>\n" +
                        "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                        "    </scm>\n" +
                        "    <assignedNode>" + String.join(" ", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
                        "    <canRoam>false</canRoam>\n" +
                        "    <disabled>false</disabled>\n" +
                        "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                        "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                        "    <triggers>\n" +
                        "        <hudson.triggers.SCMTrigger>\n" +
                        "            <spec>" + testTask.getScmPollSchedule() + "</spec>\n" +
                        "            <ignorePostCommitHooks>false</ignorePostCommitHooks>\n" +
                        "        </hudson.triggers.SCMTrigger>\n" +
                        "    </triggers>\n" +
                        "    <concurrentBuild>false</concurrentBuild>\n" +
                        "    <builders>\n" +
                        "        <hudson.tasks.Shell>\n" +
                        "            <command>\n" +
                        "#!/bin/bash&#13;\n" +
                        "export OTOOL_ARCH=" + testPlatform.getArchitecture() + XML_NEW_LINE +
                        "export OTOOL_JDK_VERSION=" + jdk8.getVersion() + XML_NEW_LINE +
                        "export OTOOL_JOB_NAME=" + testJob.getName() + XML_NEW_LINE +
                        "export OTOOL_JOB_NAME_SHORTENED=" + testJob.getShortName() + XML_NEW_LINE +
                        "export OTOOL_OJDK=o" + jdk8.getId() + XML_NEW_LINE +
                        "export OTOOL_OS=" + testPlatform.getOs() + '.' + testPlatform.getVersion() + XML_NEW_LINE +
                        "export OTOOL_PACKAGE_NAME=" + jdk8.getPackageNames().get(0) + XML_NEW_LINE +
                        "export OTOOL_PLATFORM_PROVIDER=" + testPlatform.getProviders().get(0).getId() + XML_NEW_LINE +
                        "export OTOOL_PROJECT_NAME=" + TEST_PROJECT_NAME + XML_NEW_LINE +
                        "export OTOOL_VM_NAME_OR_LOCAL=" + testPlatform.getVmName() + XML_NEW_LINE +
                        "export OTOOL_displayProtocol=" + testVariants.get(displayProtocol).getId() + XML_NEW_LINE +
                        "export OTOOL_garbageCollector=" + testVariants.get(garbageCollector).getId() + XML_NEW_LINE +
                        "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                        "</command>\n" +
                        "        </hudson.tasks.Shell>\n" +
                        "    </builders>\n" +
                        "    <publishers>\n" +
                        "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                        "            <tasks>\n" +
                        "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                        "                    <logTexts>\n" +
                        "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                        "                            <logText>.*</logText>\n" +
                        "                            <operator>OR</operator>\n" +
                        "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                        "                    </logTexts>\n" +
                        "                    <EscalateStatus>true</EscalateStatus>\n" +
                        "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                        "                    <script>#!/bin/bash&#13;bash " + Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, VAGRANT, DESTROY_SCRIPT_NAME) + " " + testPlatform.getVmName()
                        + "&#13;</script>\n" +
                        "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                        "            </tasks>\n" +
                        "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                        DataGenerator.TEST_POST_BUILD_TASK +
                        "    </publishers>\n" +
                        "    <buildWrappers/>\n" +
                        "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }


    @Test
    public void testVariable() throws IOException {
        JenkinsJobTemplateBuilder.Variable v1 = new JenkinsJobTemplateBuilder.Variable("myVar", "myVal");
        Assert.assertEquals("export OTOOL_myVar=myVal" + XML_NEW_LINE, v1.toString());
        v1 = new JenkinsJobTemplateBuilder.Variable(false, true, "comment", "myVar", "myVal");
        Assert.assertEquals("#export myVar=myVal # comment" + XML_NEW_LINE, v1.toString());

    }
}
