package org.fakekoji.jobmanager;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_TOP_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_TOP_URL;
import static org.fakekoji.DataGenerator.HOTSPOT;
import static org.fakekoji.DataGenerator.PROJECT_NAME;
import static org.fakekoji.DataGenerator.PROJECT_URL;
import static org.fakekoji.DataGenerator.RELEASE;
import static org.fakekoji.DataGenerator.SCP_POLL_SCHEDULE;
import static org.fakekoji.DataGenerator.SDK;
import static org.fakekoji.DataGenerator.TEST_PROJECT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.DESTROY_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JENKINS;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.FAKEKOJI_XML_RPC_API_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.KOJI_XML_RPC_API_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.POST_BUILD_TASK_PLUGIN;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.POST_BUILD_TASK_PLUGIN_ANALYSE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.POST_BUILD_TASK_PLUGIN_DESTROYVM;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.LOCAL;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.NO_CHANGE_RETURN_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.OJDK_VAR;
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

    private static final TaskVariant jvm = DataGenerator.getJvmVariant();
    private static final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
    private static final TaskVariant jreSdk = DataGenerator.getJreSdk();
    private static final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
    private static final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();
    private static final TaskVariant agent = DataGenerator.getAgent();
    private static final TaskVariant crypto = DataGenerator.getCrypto();
    private static final TaskVariant jfr = DataGenerator.getJfr();

    private static final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
    private static final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

    private static final Product jdk8Product = DataGenerator.getJDK8Product();
    private static final JDKVersion jdk8 = DataGenerator.getJDKVersion8();

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
                        new Task.FileRequirements(false,false,  Task.BinaryRequirements.BINARY),
                        Arrays.asList("a", "b"),
                        Arrays.asList("c", "d")
                ).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildKojiXmlRpcApiTemplateWithBinaries() throws IOException {
        final Platform vmPlatform = DataGenerator.getF29x64();

        final String expectedTemplate = "<kojiXmlRpcApi class=\"hudson.plugins.scm.koji.RealKojiXmlRpcApi\">\n" +
                "    <xmlRpcApiType>REAL_KOJI</xmlRpcApiType>\n" +
                "    <packageName>" + jdk8.getPackageNames().get(0) + "</packageName>\n" +
                "    <arch/>\n" +
                "    <tag>" + String.join(" ", vmPlatform.getTags()) + "</tag>\n" +
                "    <subpackageBlacklist>a b</subpackageBlacklist>\n" +
                "    <subpackageWhitelist>c d</subpackageWhitelist>\n" +
                "</kojiXmlRpcApi>\n";

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(KOJI_XML_RPC_API_TEMPLATE), dummyNamesProvider)
                .buildKojiXmlRpcApiTemplate(
                        jdk8.getPackageNames().get(0),
                        vmPlatform,
                        new Task.FileRequirements(false, false, Task.BinaryRequirements.BINARIES),
                        Arrays.asList("a", "b"),
                        Arrays.asList("c", "d")
                ).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildKojiXmlRpcApiTemplateWithWindows() throws IOException {

        final Platform win = DataGenerator.getWin2019x64();

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
                        new Task.FileRequirements(false, false,  Task.BinaryRequirements.BINARY),
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
                "    <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
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

        final String expectedTemplate = DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, null, vmPlatform.getVmName()).replaceAll("(?m)^        ", "");

        final String actualTemplate = new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(POST_BUILD_TASK_PLUGIN), dummyNamesProvider)
                .buildPostBuildTaskTemplate(VAGRANT, vmPlatform.getVmName(), scriptsRoot, new ArrayList<>(), true, true).prettyPrint();

        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildBuildJobTemplateWithVmPlatform() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task buildTask = DataGenerator.getBuildTask();
        final Platform vmPlatform = DataGenerator.getF29x64();

        final BuildJob buildJob = new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                jdk8Product,
                jdk8,
                buildProviders,
                buildTask,
                vmPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "buildPlatform=" + vmPlatform.getId() + " debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId()
                + "</buildVariants>\n" +
                "            <buildPlatform>src</buildPlatform>\n" +
                "            <isBuilt>" + false + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", vmPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
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
                "export OTOOL_ARCH=\"" + vmPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + buildJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + buildJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + vmPlatform.getOs() + '.' + vmPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + vmPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + vmPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + vmPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + vmPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"build\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + vmPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + buildTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.BUILD_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, buildJob.getShortName(), vmPlatform.getVmName()) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = buildJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildBuildJobTemplateWithHwPlatform() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task buildTask = DataGenerator.getBuildTask();
        final Platform hwPlatform = DataGenerator.getRHEL8Aarch64();

        final BuildJob buildJob = new BuildJob(
                VAGRANT,
                PROJECT_NAME,
                jdk8Product,
                jdk8,
                buildProviders,
                buildTask,
                hwPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>buildPlatform=" + hwPlatform.getId() + " debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId()
                + "</buildVariants>\n" +
                "            <buildPlatform>src</buildPlatform>\n" +
                "            <isBuilt>" + false + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", hwPlatform.getProviders().get(0).getHwNodes()) + "</assignedNode>\n" +
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
                "export OTOOL_ARCH=\"" + hwPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + buildJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + buildJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + hwPlatform.getOs() + '.' + hwPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + hwPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + hwPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + hwPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + hwPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"build\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + LOCAL+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + buildTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.BUILD_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), false, true, null, null, null) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = buildJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateWithHwPlatform() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getRHEL8Aarch64();
        final Platform testPlatform = DataGenerator.getRHEL8Aarch64();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>" + buildPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getHwNodes()) + "</assignedNode>\n" +
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
                "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + buildPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"tck\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + LOCAL+ "\"" + XML_NEW_LINE +
                "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), false, true, null, null, null) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateWithVmPlatform() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getRHEL7x64();
        final Platform testPlatform = DataGenerator.getRHEL7x64();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>" + testPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getVmNodes().stream().map(s -> s.replace("%{OTOOL_OS_NAME}","el")).collect(Collectors.toList())) + "</assignedNode>\n" +
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
                "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + testPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"tck\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + testPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, testJob.getShortName(), testPlatform.getVmName()) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateWithCustomVariables() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTCKWithAgent();
        final Platform buildPlatform = DataGenerator.getRHEL7Zx64();
        final Platform testPlatform = DataGenerator.getRHEL7Zx64();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>" + testPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getVmNodes().stream().map(s -> s.replace("%{OTOOL_OS_NAME}","el")).collect(Collectors.toList())) + "</assignedNode>\n" +
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
                "export OTOOL_AGENT=\"linux"+ "\"" + XML_NEW_LINE +
                "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_IS_RHEL_Z_STREAM=\"true"+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + testPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"tck~agent\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + testPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_ystream=\"true"+ "\"" + XML_NEW_LINE +
                "export OTOOL_zstream=\"false"+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, testJob.getShortName(), testPlatform.getVmName()) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateRequestingSourcesAndBinary() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTaskRequiringSourcesAndBinary();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();
        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform>src " + testPlatform.getId() + "</buildPlatform>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
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
                "        <hudson.plugins.build__timeout.BuildStepWithTimeout plugin=\"build-timeout@1.19\">\n" +
                "            <strategy class=\"hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy\">\n" +
                "                <timeoutMinutes>600</timeoutMinutes>\n" +
                "            </strategy>\n" +
                "            <buildStep class=\"hudson.tasks.Shell\">\n" +
                "                <command>\n" +
                "#!/bin/bash&#13;\n" +
                "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + testPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"jtreg\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + testPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "    </command>\n" +
                "            </buildStep>\n" +
                "            <operationList>\n" +
                "                <hudson.plugins.build__timeout.operations.AbortOperation/>\n" +
                "            </operationList>\n" +
                "        </hudson.plugins.build__timeout.BuildStepWithTimeout>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, testJob.getShortName(), testPlatform.getVmName()) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateRequestingSourcesAndBinaries() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTaskRequiringSourcesAndBinaries();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TestJob testJob = new TestJob(
                VAGRANT,
                PROJECT_NAME,
                Project.ProjectType.JDK_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot,
                null
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
                "            <buildVariants>" + "debugMode=" + buildVariants.get(debugMode).getId() + " jreSdk=" + buildVariants.get(jreSdk).getId() + " jvm=" + buildVariants.get(jvm).getId() + "</buildVariants>\n" +
                "            <buildPlatform/>\n" +
                "            <isBuilt>" + true + "</isBuilt>\n" +
                "        </kojiXmlRpcApi>\n" +
                "        <downloadDir>rpms</downloadDir>\n" +
                "        <cleanDownloadDir>true</cleanDownloadDir>\n" +
                "        <dirPerNvr>false</dirPerNvr>\n" +
                "        <maxPreviousBuilds>10</maxPreviousBuilds>\n" +
                "    </scm>\n" +
                "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
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
                "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_PROJECT_NAME=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export OTOOL_RELEASE_SUFFIX=\"" + RELEASE + '.' + HOTSPOT + '.' + SDK + '.' + testPlatform.getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_TASK=\"tck\"" + XML_NEW_LINE +
                "export OTOOL_VM_NAME_OR_LOCAL=\"" + testPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_debugMode=\"" + buildVariants.get(debugMode).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jreSdk=\"" + buildVariants.get(jreSdk).getId()+ "\"" + XML_NEW_LINE +
                "export OTOOL_jvm=\"" + buildVariants.get(jvm).getId()+ "\"" + XML_NEW_LINE +
                "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                "</command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                DataGenerator.TEST_POST_BUILD_TASK +
                DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(), true, true, VAGRANT, testJob.getShortName(), testPlatform.getVmName()) +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildPullJobTemplate() throws IOException {
        final File reposRoot = temporaryFolder.newFolder("repos");

        final String expectedTemplate = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n" +
                "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.scm.NullSCM\"/>\n" +
                "    <assignedNode>" + AccessibleSettings.master.label + "</assignedNode>\n" +
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
                "#export " + NO_CHANGE_RETURN_VAR + "=\"-1\" # any negative is enforcing pull even without changes detected" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + JDK_VERSION_VAR + "=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + OJDK_VAR + "=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PACKAGE_NAME_VAR + "=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PROJECT_NAME_VAR + "=\"" + PROJECT_NAME+ "\"" + XML_NEW_LINE +
                "export " + OTOOL_BASH_VAR_PREFIX + PROJECT_PATH_VAR + "=\"" + Paths.get(reposRoot.getAbsolutePath(), PROJECT_NAME)+ "\"" + XML_NEW_LINE +
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
                "                <buildId/>\n" +
                "                <succeedIfFound>false</succeedIfFound>\n" +
                "                <unstableIfFound>true</unstableIfFound>\n" +
                "                <notBuiltIfFound>false</notBuiltIfFound>\n" +
                "                <alsoCheckConsoleOutput>true</alsoCheckConsoleOutput>\n" +
                "            </primaryTextFinder>\n" +
                "            <additionalTextFinders>\n" +
                "                <hudson.plugins.textfinder.TextFinderModel>\n" +
                "                    <regexp>nothing_ever</regexp>\n" +
                "                    <buildId>^future VR: </buildId>\n" +
                "                    <succeedIfFound>false</succeedIfFound>\n" +
                "                    <unstableIfFound>false</unstableIfFound>\n" +
                "                    <notBuiltIfFound>false</notBuiltIfFound>\n" +
                "                    <alsoCheckConsoleOutput>true</alsoCheckConsoleOutput>\n" +
                "                </hudson.plugins.textfinder.TextFinderModel>\n" +
                "            </additionalTextFinders>\n" +
                "        </hudson.plugins.textfinder.TextFinderPublisher>\n" +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final PullJob pullJob = new PullJob(
                PROJECT_NAME,
                PROJECT_URL,
                jdk8Product,
                jdk8,
                reposRoot,
                scriptsRoot,
                null
        );

        final String actualTemplate = pullJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void buildTestJobTemplateOfJDKTestProject() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TaskVariantValue release = DataGenerator.getReleaseVariant();

        final Map<TaskVariant, TaskVariantValue> buildVariants = new HashMap<TaskVariant, TaskVariantValue>() {{
            put(debugMode, release);
        }};
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
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                null,
                null,
                buildVariants,
                blacklist,
                whitelist,
                scriptsRoot,
                null
        );

        final List<String> expectedBlacklist = new ArrayList<>();
        expectedBlacklist.addAll(blacklist);
        expectedBlacklist.addAll(testTask.getRpmLimitation().getBlacklist());
        expectedBlacklist.addAll(release.getSubpackageBlacklist().get());
        final List<String> expectedWhitelist = new ArrayList<>();
        expectedWhitelist.addAll(whitelist);
        expectedWhitelist.addAll(testTask.getRpmLimitation().getWhitelist());
        expectedWhitelist.addAll(release.getSubpackageWhitelist().get());

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
                        "    <assignedNode>" + String.join("||", testPlatform.getProviders().get(0).getVmNodes()) + "</assignedNode>\n" +
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
                        "export OTOOL_ARCH=\"" + testPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_BUILD_ARCH=\"" + buildPlatform.getArchitecture()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_BUILD_OS=\"" + buildPlatform.toOsVar()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_BUILD_OS_NAME=\"" + buildPlatform.getOs()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_BUILD_OS_VERSION=\"" + buildPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_JDK_VERSION=\"" + jdk8.getVersion()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_JOB_NAME=\"" + testJob.getName()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_JOB_NAME_SHORTENED=\"" + testJob.getShortName()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_OJDK=\"" + jdk8.getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_OS=\"" + testPlatform.getOs() + '.' + testPlatform.getVersion()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_OS_NAME=\"" + testPlatform.getOs()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_OS_VERSION=\"" + testPlatform.getVersionNumber()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_PACKAGE_NAME=\"" + jdk8.getPackageNames().get(0)+ "\"" + XML_NEW_LINE +
                        "export OTOOL_PLATFORM_PROVIDER=\"" + testPlatform.getProviders().get(0).getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_PROJECT_NAME=\"" + TEST_PROJECT_NAME+ "\"" + XML_NEW_LINE +
                        "export OTOOL_TASK=\"tck\"" + XML_NEW_LINE +
                        "export OTOOL_VM_NAME_OR_LOCAL=\"" + testPlatform.getVmName()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_agent=\"" + testVariants.get(agent).getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_crypto=\"" + testVariants.get(crypto).getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_debugMode=\"" + release.getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_displayProtocol=\"" + testVariants.get(displayProtocol).getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_garbageCollector=\"" + testVariants.get(garbageCollector).getId()+ "\"" + XML_NEW_LINE +
                        "export OTOOL_jfr=\"" + testVariants.get(jfr).getId()+ "\"" + XML_NEW_LINE +
                        "\nbash " + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME) + " '" + testTask.getScript() + "'&#13;\n" +
                        "</command>\n" +
                        "        </hudson.tasks.Shell>\n" +
                        "    </builders>\n" +
                        "    <publishers>\n" +
                        DataGenerator.TEST_POST_BUILD_TASK +
                        DataGenerator.getPostTasks(scriptsRoot.getAbsolutePath(),true, true, VAGRANT, testJob.getShortName(),  testPlatform.getVmName())+
                        "    </publishers>\n" +
                        "    <buildWrappers/>\n" +
                        "</project>\n";

        final String actualTemplate = testJob.generateTemplate();
        Assert.assertEquals(expectedTemplate, actualTemplate);
    }


    @Test
    public void testVariable() throws IOException {
        OToolVariable v1 = new OToolVariable("myVar", "myVal");
        Assert.assertEquals(
                "export OTOOL_myVar=\"myVal\"" + XML_NEW_LINE,
                v1.getVariableString(XML_NEW_LINE)
        );
        v1 = new OToolVariable("myVar", "myVal", "comment", false, true, true);
        Assert.assertEquals(
                "#export myVar=\"myVal\" # comment" + XML_NEW_LINE,
                v1.getVariableString(XML_NEW_LINE)
        );
        v1 = new OToolVariable("myVar", "myVal", "comment", false, true, false);
        Assert.assertEquals(
                "#myVar=\"myVal\" # comment" + XML_NEW_LINE,
                v1.getVariableString(XML_NEW_LINE)
        );
    }
}
