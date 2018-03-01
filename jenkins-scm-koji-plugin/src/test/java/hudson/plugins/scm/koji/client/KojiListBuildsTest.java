package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.NotProcessedNvrPredicate;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.fakekoji.JavaServer;
import org.fakekoji.xmlrpc.server.core.FakeKojiTestUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import org.junit.BeforeClass;

public class KojiListBuildsTest {

    private static void strToFile(String s, File f) throws FileNotFoundException, IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write(s);
        }
    }

    private static void strsToFile(String[] s, File f) throws FileNotFoundException, IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            for (String s1 : s) {
                bw.write(s1);
                bw.newLine();
            }
        }
    }

    KojiScmConfig createConfigCustomFedora() {
        return new KojiScmConfig(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://localhost:" + JavaServerConstants.dPortAxiom,
                "java-1.8.0-openjdk",
                "x86_64,src",
                "fastdebug-f24*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigCustomFedoraSrcOnly() {
        return new KojiScmConfig(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://localhost:" + JavaServerConstants.dPortAxiom,
                "java-1.8.0-openjdk",
                "src",
                "f24*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigCustomRhel7() {
        return new KojiScmConfig(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://localhost:" + JavaServerConstants.dPortAxiom,
                "java-1.8.0-openjdk",
                "x86_64,src",
                "rhel-7.*-candidate",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigF() {
        return new KojiScmConfig(
                "https://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/",
                "java-1.8.0-openjdk",
                "x86_64,src",
                "f24*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigMultipleValidUrls() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub https://koji.fedoraproject.org/kojihub",
                "http://download.eng.bos.redhat.com/brewroot/packages/ https://kojipkgs.fedoraproject.org/packages/",
                "java-1.8.0-openjdk",
                "x86_64,src",
                "f24*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR7() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.8.0-openjdk",
                "x86_64,src",
                "rhel-7.*-candidate",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR6_ibm6() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.6.0-ibm",
                "x86_64",
                "rhel-6.*-supp*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR5_ibm6() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.6.0-ibm",
                "i386",
                "dist-5*-extras*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR7_ibm71() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.7.1-ibm",
                "x86_64",
                "supp-rhel-7.*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR6() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.8.0-openjdk",
                "x86_64,src",
                "RHEL-6.*-candidate",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR6_oracle7() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.7.0-oracle",
                "i686",
                "oracle-java-rhel-6.*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR5_sun6() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.6.0-sun",
                "x86_64",
                "oracle-java-rhel-5*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigR7_oracle8() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.8.0-oracle",
                "x86_64",
                "oracle-java-rhel-7.*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigWindows() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "openjdk8-win",
                "win",
                "openjdk-win*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigCustomWindows() {
        return new KojiScmConfig(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "https://localhost:" + JavaServerConstants.dPortAxiom,
                "openjdk8-win",
                "win",
                "openjdk-win*",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createConfigWithEmptyArch() {
        return new KojiScmConfig(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/",
                "java-1.8.0-openjdk",
                null,
                "RHEL-6.*-candidate",
                null,
                null,
                false,
                false,
                10
        );
    }

    KojiScmConfig createMultiProductConfig() {
        return new KojiScmConfig(
                "https://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/",
                "java-1.7.0-openjdk java-1.8.0-openjdk java-9-openjdk",
                null,
                "*",
                null,
                null,
                false,
                false,
                10
        );
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static File f1;
    private static File e71;
    private static File e61;

    private static boolean onRhNet;

    @BeforeClass
    public static void initProcessed() throws IOException {
        f1 = File.createTempFile("scm-koji", "fedora");
        e71 = File.createTempFile("scm-koji", "rhel7");
        e61 = File.createTempFile("scm-koji", "rhel6");
        f1.deleteOnExit();
        e61.deleteOnExit();
        e71.deleteOnExit();
        strsToFile(
                new String[]{"java-1.8.0-openjdk-1.8.0.71-0.b15.el7_2",
                    "java-1.8.0-openjdk-1.8.0.71-1.b15.el7_2",
                    "java-1.8.0-openjdk-1.8.0.71-2.b15.el7_2",
                    "java-1.8.0-openjdk-1.8.0.72-0.b15.el7",
                    "java-1.8.0-openjdk-1.8.0.72-1.b15.el7",
                    "java-1.8.0-openjdk-1.8.0.72-2.b15.el7",
                    "java-1.8.0-openjdk-1.8.0.72-3.b15.el7",
                    "java-1.8.0-openjdk-1.8.0.72-4.b15.el7",
                    "java-1.8.0-openjdk-1.8.0.72-5.b16.el7",
                    "java-1.8.0-openjdk-1.8.0.72-12.b16.el7",
                    "java-1.8.0-openjdk-1.8.0.72-13.b16.el7",
                    "java-1.8.0-openjdk-1.8.0.77-0.b03.el7",
                    "java-1.8.0-openjdk-1.8.0.77-0.b03.el7_2",
                    "java-1.8.0-openjdk-1.8.0.77-1.b03.el7",
                    "java-1.8.0-openjdk-1.8.0.77-2.b03.el7",
                    "java-1.8.0-openjdk-1.8.0.91-0.b14.el7_2",
                    "java-1.8.0-openjdk-1.8.0.91-1.b14.el7",
                    "java-1.8.0-openjdk-1.8.0.91-2.b14.el7",
                    "java-1.8.0-openjdk-1.8.0.91-3.b14.el7",
                    "java-1.8.0-openjdk-1.8.0.92-1.b14.el7",
                    "java-1.8.0-openjdk-1.8.0.92-2.b14.el7"
                }, e71);

        strsToFile(
                new String[]{
                    "java - 1.8.0-openjdk - 1.8.0.65-3.b17.el6_7",
                    "java-1.8.0-openjdk-1.8.0.65-4.b17.el6",
                    "java-1.8.0-openjdk-1.8.0.65-5.b17.el6",
                    "java-1.8.0-openjdk-1.8.0.71-0.b15.el6_7",
                    "java-1.8.0-openjdk-1.8.0.71-1.b15.el6",
                    "java-1.8.0-openjdk-1.8.0.71-1.b15.el6_7",
                    "java-1.8.0-openjdk-1.8.0.71-2.b15.el6",
                    "java-1.8.0-openjdk-1.8.0.71-3.b15.el6",
                    "java-1.8.0-openjdk-1.8.0.71-4.b15.el6",
                    "java-1.8.0-openjdk-1.8.0.71-5.b15.el6",
                    "java-1.8.0-openjdk-1.8.0.77-0.b03.el6_7",
                    "java-1.8.0-openjdk-1.8.0.77-1.b03.el6",
                    "java-1.8.0-openjdk-1.8.0.77-2.b03.el6",
                    "java-1.8.0-openjdk-1.8.0.91-0.b14.el6_7",
                    "java-1.8.0-openjdk-1.8.0.91-1.b14.el6",
                    "java-1.8.0-openjdk-1.8.0.91-1.b14.el6"

                }, e61
        );
        strsToFile(
                new String[]{
                    "java-1.8.0-openjdk-1.8.0.72-13.b16.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-2.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-3.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-4.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-4.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-6.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-7.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                    "java-1.8.0-openjdk-1.8.0.91-8.b14.fc24"

                }, f1
        );

        HttpURLConnection connection = null;
        try {
            int response = FakeKojiTestUtil.doHttpRequest("https://brewweb.engineering.redhat.com/brew/", "HEAD");
            onRhNet = (response == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            onRhNet = false;
        }
    }

    public void testListMatchingBuildsCustom(KojiListBuilds worker) throws Exception {
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        JavaServer javaServer =
                FakeKojiTestUtil.createDefaultFakeKojiServerWithData(tmpDir);
        try {
            javaServer.start();
            Build build = worker.invoke(temporaryFolder.newFolder(), null);
//        KojiBuildDownloader dwldr = new KojiBuildDownloader(createConfigCustomFedora(), new NotProcessedNvrPredicate(new HashSet<>()));
//        dwldr.downloadRPMs(new File("/tmp"), build);
            assertNotNull(build);
        } finally {
            javaServer.stop();
        }
    }

    @Test
    public void testListMatchingBuildsCustomF() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfigCustomFedora(), new NotProcessedNvrPredicate(new ArrayList<>()));
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsCustomFsrcOnly() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfigCustomFedoraSrcOnly(), new NotProcessedNvrPredicate(new ArrayList<>()));
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsCustomWindows() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfigCustomWindows(), new NotProcessedNvrPredicate(new ArrayList<>()));
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsCustomRhel() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfigCustomRhel7(), new NotProcessedNvrPredicate(new ArrayList<>()));
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsMultipleValidUrls() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigMultipleValidUrls(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(f1));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        //KojiBuildDownloader dwldr = new KojiBuildDownloader(createConfigMultipleValidUrls(), new NotProcessedNvrPredicate(new HashSet<>()));
        //dwldr.downloadRPMs(new File("/tmp"), build);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsF() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfigF(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(f1));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR7() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR7(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e71));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR6_ibm6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR6_ibm6(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR5_ibm6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR5_ibm6(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR7_ibm() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR7_ibm71(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR5_sun6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR5_sun6(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR6_oracle7() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR6_oracle7(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsR7_oracle8() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigR7_oracle8(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testListMatchingBuildsWindows() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigWindows(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testNoArchPresentBuilds() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createConfigWithEmptyArch(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testMultiproductBuilds() throws IOException, InterruptedException {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(createMultiProductConfig(), new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testMultiproductBuildsWithNonExistingProduct() throws IOException, InterruptedException {
        assumeTrue(onRhNet);
        KojiScmConfig config = new KojiScmConfig(
                "https://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/",
                "this_package_name_hopefully_does_not_exist java-1.8.0-openjdk",
                null, "*", null, null, false, false, 10);
        KojiListBuilds worker = new KojiListBuilds(config, new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

    @Test
    public void testNonExistingBuilds() throws IOException, InterruptedException {
        assumeTrue(onRhNet);
        KojiScmConfig config = new KojiScmConfig(
                "https://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/",
                "some_random_package_name_that_does_not_exist some_other_package_that_hopefully_also_does_not_exist",
                null, "*", null, null, false, false, 10);
        KojiListBuilds worker = new KojiListBuilds(config, new NotProcessedNvrPredicate(new ArrayList<>()));
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNull(build);
    }
}
