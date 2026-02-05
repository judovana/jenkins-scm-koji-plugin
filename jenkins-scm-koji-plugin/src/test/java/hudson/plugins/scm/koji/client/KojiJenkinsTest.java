/*
 * The MIT License
 *
 * Copyright 2018 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.scm.koji.client;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.tasks.Shell;

import org.fakekoji.core.FakeKojiTestUtil;
import org.fakekoji.server.JavaServer;
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
import java.util.List;

import static hudson.plugins.scm.koji.client.KojiListBuildsTest.createLocalhostKojiBuildProvider;

@WithJenkins
public class KojiJenkinsTest {
    @TempDir
    static Path temporaryFolder;
    public static JavaServer javaServer = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        /* create fake koji server (with data) */
        temporaryFolder.toFile().mkdirs();
        javaServer = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(temporaryFolder.toFile());
        /* start fake-koji server */
        javaServer.start();
    }

    @AfterAll
    public static void afterClass() {
        /* stop fake-koji */
        if (javaServer != null) {
            javaServer.stop();
        }
    }

    private List<KojiBuildProvider> createKojiBuildProviders() {
        return Collections.singletonList(createLocalhostKojiBuildProvider());
    }

    public void runTest(JenkinsRule j, RealKojiXmlRpcApi kojiXmlRpcApi, String shellScript, boolean successExpected) throws Exception {
        /* create new jenkins free style project */
        FreeStyleProject project = j.createFreeStyleProject();


        /* create KojiSCM plugin instance */
        KojiSCM scm = new KojiSCM(
                createKojiBuildProviders(),
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

    @Test
    public void testExistingBuild(JenkinsRule j) throws Exception {
        /* Test koji scm plugin on existing fake-koji build(s) 
           -> should end with success */
        String shellString = "find . | grep \"java-1.8.0-openjdk.*x86_64.tarxz\"\n"
                + "find . | grep \"java-1.8.0-openjdk.*src.tarxz\"";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "java-1.8.0-openjdk",
                        "x86_64 src",
                        "fastdebug-f24.*",
                        "",
                        null
                ),
                shellString,
                false
        );
    }

    @Test
    public void testNonExistingBuild(JenkinsRule j) throws Exception {
        /* Test koji scm plugin on non-existing fake-koji build(s)
           -> should not end with success */
        String shellString = "! find . | grep \".*tarxz\"";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "non-existing-build",
                        "x86_64,src",
                        "fastdebug-f24.*",
                        "",
                        null
                ),
                shellString,
                false
        );
    }

    @Test
    public void testNoWhitelistWorks(JenkinsRule j) throws Exception {
        String shellString = " a=`find . | wc -l ` ; test $a -eq 2 ";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "java-1.8.0-openjdk",
                        "x86_64",
                        ".*",
                        null,
                        ".*slow.*"
                ),
                shellString,
                true
        );
    }

    public void testWhitelistWorks(JenkinsRule j) throws Exception {
        String shellString = " a=`find . | wc -l ` ; test $a -eq 1 ";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "java-1.8.0-openjdk",
                        "x86_64",
                        ".*",
                        null,
                        ".*nothing.*"
                ),
                shellString,
                true
        );
    }

    @Test
    public void testBlackListExcludesNothing(JenkinsRule j) throws Exception {
        String shellString = "a=`find . | wc -l ` ; test $a -eq 2 ";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "java-1.8.0-openjdk",
                        "x86_64",
                        ".*",
                        ".*nothing.*",
                        ".*"
                ),
                shellString,
                true
        );
    }

    @Test
    public void testBlackListExcludes(JenkinsRule j) throws Exception {
        String shellString = "a=`find . | wc -l ` ; test $a -eq 1 ";
        runTest(j,
                new RealKojiXmlRpcApi(
                        "java-1.8.0-openjdk",
                        "x86_64",
                        ".*",
                        ".*java.*",
                        ".*"
                ),
                shellString,
                true
        );
    }

    //TODO add test which will test that blacklist and whitelist are acting in proper order
    //the order is crucial, the  oposite direction do not have meaning
    //it seesm that we do nothave enough data for this in generator
}
