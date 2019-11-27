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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

import static org.fakekoji.DataGenerator.JDK_8_PACKAGE_NAME;
import static org.fakekoji.DataGenerator.PROJECT_NAME_U;
import static org.fakekoji.DataGenerator.RELEASE_1;
import static org.fakekoji.DataGenerator.SOURCES;
import static org.fakekoji.DataGenerator.SUFFIX;
import static org.fakekoji.DataGenerator.VERSION_1;
import static org.junit.Assert.assertEquals;

public class OToolJenkinsTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static JavaServer javaServer = null;

    @BeforeClass
    public static void setup() throws Exception {
        File tmpDir = temporaryFolder.newFolder();
        javaServer = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(tmpDir);
        javaServer.start();
    }

    @AfterClass
    public static void tearDown() {
        if (javaServer != null) {
            javaServer.stop();
        }
    }

    @Test
    public void testBuildJobFailure() throws Exception {
        /* Test koji scm plugin on existing fake-koji build(s)
           -> should end with success */
        final String expectedFile = JDK_8_PACKAGE_NAME + '-' + VERSION_1 + '-' + RELEASE_1 + '.' + PROJECT_NAME_U + '.' + SOURCES + SUFFIX;
        String shellString = "find . | grep \"" + expectedFile + "\"";
        runTest(
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=release jvm=hotspot buildPlatform=f29.x86_64",
                        "src",
                        false
                ),
                shellString,
                false
        );
    }

    @Test
    public void testBuildJobSuccess() throws Exception {
        final String expectedFile = "java-1.8.0-openjdk-version2-release1.uName.src.tarxz";
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot buildPlatform=f29.x86_64",
                        "src",
                        false
                ),
                shellString,
                true
        );
    }

    @Test
    public void testTestJobFailure() throws Exception {
        final String expectedFile = "java-1.8.0-openjdk-version1-release2.uName.slowdebug.hotspot.f29.x86_64.tarxz";
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=slowdebug jvm=hotspot",
                        "f29.x86_64",
                        true
                ),
                shellString,
                false
        );
    }

    @Test
    public void testTestJobSuccess() throws Exception {
        final String expectedFile = "java-1.8.0-openjdk-version1-release1.uName.fastdebug.hotspot.f29.x86_64.tarxz";
        String shellString = "find . | grep \"" + expectedFile + "\"\n";
        runTest(
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot",
                        "f29.x86_64",
                        true
                ),
                shellString,
                true
        );
    }

    @Test
    public void testTestJobWithSourcesSuccess() throws Exception {
        final String expectedFile = "java-1.8.0-openjdk-version1-release1.uName.fastdebug.hotspot.f29.x86_64.tarxz";
        final String expectedSourceFile = "java-1.8.0-openjdk-version1-release1.uName.src.tarxz";
        String shellString = "find . | grep \"" + expectedFile + "\"\nfind . | grep \"" + expectedSourceFile + "\"\n";
        runTest(
                new FakeKojiXmlRpcApi(
                        PROJECT_NAME_U,
                        "debugMode=fastdebug jvm=hotspot",
                        "f29.x86_64 src",
                        true
                ),
                shellString,
                true
        );
    }

    private void runTest(FakeKojiXmlRpcApi kojiXmlRpcApi, String shellScript, boolean successExpected) throws Exception {
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
        assertEquals(successExpected, result == Result.SUCCESS);
    }

    private KojiBuildProvider createLocalhostBuildProvider() {
        return new KojiBuildProvider(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://localhost:" + JavaServerConstants.dPortAxiom
        );
    }
}
