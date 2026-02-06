package hudson.plugins.scm.koji.client;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.tasks.Shell;
import org.fakekoji.core.FakeKojiTestUtil;
import org.fakekoji.server.JavaServer;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collections;

import static org.fakekoji.DataGenerator.*;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.SOURCES;

@WithJenkins
public class OToolJenkinsTest {

    @TempDir
    static Path temporaryFolder;


    private static JavaServer javaServer = null;

    @BeforeAll
    public static void setup() throws Exception {
        javaServer = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(temporaryFolder.toFile());
        javaServer.start();
    }

    @AfterAll
    public static void tearDown() {
        if (javaServer != null) {
            javaServer.stop();
        }
    }

    @Test
    public void testBuildJobFailure(JenkinsRule j) throws Exception {
        /* Test koji scm plugin on existing fake-koji build(s)
           -> should end with success */
        final String expectedFile = JDK_8_PACKAGE_NAME + '-' + VERSION_1 + '-' + RELEASE_1 + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=release jvm=hotspot jreSdk=sdk buildPlatform=f29.x86_64",
                        "src",
                        false
                ),
                shellString,
                false
        );
    }

    @Test
    public void testBuildJobSuccess(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_2 + "-" + RELEASE_1 + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk buildPlatform=f29.x86_64",
                        "src",
                        false
                ),
                shellString,
                true
        );
    }

    @Test
    public void testBuildJobNotSuccessForBadRelease(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_2 + "-" + RELEASE_1_BAD + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk buildPlatform=f29.x86_64",
                        "src",
                        false
                ),
                shellString,
                false
        );
    }

    @Test
    public void testTestJobFailure(JenkinsRule j) throws Exception {
        final String expectedFile = "java-1.8.0-openjdk-version1-release2.uName.slowdebug.hotspot.f29.x86_64.tarxz";
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=slowdebug jvm=hotspot jreSdk=sdk",
                        "f29.x86_64",
                        true
                ),
                shellString,
                false
        );
    }

    @Test
    public void testTestJobSuccess(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1 + '.' + PROJECT_NAME_U + ".fastdebug.hotspot.sdk.f29.x86_64"+SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk",
                        "f29.x86_64",
                        true
                ),
                shellString,
                true
        );
    }

    @Test
    public void testTestJobSuccessNotForBadRelease(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1_BAD + '.' + PROJECT_NAME_U + ".fastdebug.hotspot.sdk.f29.x86_64"+SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk",
                        "f29.x86_64",
                        true
                ),
                shellString,
                false
        );
    }

    @Test
    public void testTestJobWithSourcesSuccess(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1 + '.' + PROJECT_NAME_U + ".fastdebug.hotspot.sdk.f29.x86_64"+SUFFIX;
        final String expectedSourceFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1 + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\nfind . | grep \"" + expectedSourceFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk",
                        "f29.x86_64 src",
                        true
                ),
                shellString,
                true
        );
    }

    @Test
    public void testTestJobWithSourcesSuccessNotForBAdRelease(JenkinsRule j) throws Exception {
        final String expectedFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1_BAD + '.' + PROJECT_NAME_U + ".fastdebug.hotspot.sdk.f29.x86_64"+SUFFIX;
        final String expectedSourceFile = JDK_8_PACKAGE_NAME + "-" + VERSION_1 + "-" + RELEASE_1_BAD + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"\nfind . | grep \"" + expectedSourceFile + "\"\n";
        runTest(j,
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot jreSdk=sdk",
                        "f29.x86_64 src",
                        true
                ),
                shellString,
                false
        );
    }

    private void runTest(JenkinsRule j, FakeKojiXmlRpcApi kojiXmlRpcApi, String shellScript, boolean successExpected) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        KojiSCM scm = new KojiSCM(
                Collections.singletonList(createLocalhostBuildProvider()),
                kojiXmlRpcApi,
                null,
                false,
                false,
                10
        );
        /* set new KojiSCM plugin instance as scm for project */
        project.setScm(scm);
        /* set shell string executed by the project */
        project.getBuildersList().add(new Shell(shellScript));
        /* schedule build and get it (wait for it to finish) */
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        /* Print log (of the build) to stdout */
        try (FileInputStream fis = new FileInputStream(build.getLogFile())) {
            for (;;) {
                int readByte = fis.read();
                if (readByte < 0) {
                    break;
                }
                System.out.write(readByte);
            }
        }
        /* get result of the build and check it it meets expectations */
        Result result = build.getResult();
        Assertions.assertEquals(successExpected, result == Result.SUCCESS);
    }

    private KojiBuildProvider createLocalhostBuildProvider() {
        return new KojiBuildProvider(
                "http://localhost:" + JavaServerConstants.DFAULT_RP2C_PORT + "/RPC2",
                "http://localhost:" + JavaServerConstants.DFAULT_DWNLD_PORT
        );
    }
}
