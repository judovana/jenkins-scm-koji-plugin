package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.NotProcessedNvrPredicate;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import org.junit.BeforeClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.fakekoji.core.FakeKojiTestUtil;
import org.fakekoji.server.JavaServer;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assume.assumeTrue;

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

    static KojiBuildProvider createHydraKojiBuildProvider() {
        return new KojiBuildProvider(
                "http://hydra.brq.redhat.com:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://hydra.brq.redhat.com:" + JavaServerConstants.dPortAxiom
        );
    }

    static KojiBuildProvider createLocalhostKojiBuildProvider() {
        return new KojiBuildProvider(
                "http://localhost:" + JavaServerConstants.xPortAxiom + "/RPC2",
                "http://localhost:" + JavaServerConstants.dPortAxiom
        );
    }

    static KojiBuildProvider createKojiHubKojiBuildProvider() {
        return new KojiBuildProvider(
                "https://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/"
        );
    }

    static KojiBuildProvider createBrewHubKojiBuildProvider() {
        return new KojiBuildProvider(
                "https://brewhub.engineering.redhat.com/brewhub",
                "http://download.eng.bos.redhat.com/brewroot/packages/"
        );
    }

    static List<KojiBuildProvider> createBrewOnlyList() {
        return Collections.singletonList(createBrewHubKojiBuildProvider());
    }

    static List<KojiBuildProvider> createKojiOnlyList() {
        return Collections.singletonList(createKojiHubKojiBuildProvider());
    }

    static List<KojiBuildProvider> createLocalhostOnlyList() {
        return Collections.singletonList(createLocalhostKojiBuildProvider());
    }

    static List<KojiBuildProvider> createHydraOnlyList() {
        return Collections.singletonList(createHydraKojiBuildProvider());
    }

    static List<KojiBuildProvider> createKojiBrewList() {
        return Arrays.asList(
                createKojiHubKojiBuildProvider(),
                createBrewHubKojiBuildProvider()
        );
    }

     RealKojiXmlRpcApi createConfigCustomFedora28() {
        return new RealKojiXmlRpcApi(
                "java-11-openjdk",
                "x86_64,src",
                "f28*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomFedora() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "fastdebug-f24*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomFedoraSrcOnly() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "src",
                "f24*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomRhel7() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "rhel-7.*-candidate",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigF() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "f24*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigMultipleValidUrls() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "f24*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR7() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "rhel-7.*-candidate",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR6_ibm6() {
        return new RealKojiXmlRpcApi(
                "java-1.6.0-ibm",
                "x86_64",
                "rhel-6.*-supp*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR5_ibm6() {
        return new RealKojiXmlRpcApi(
                "java-1.6.0-ibm",
                "i386",
                "dist-5*-extras*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR7_ibm71() {
        return new RealKojiXmlRpcApi(
                "java-1.7.1-ibm",
                "x86_64",
                "supp-rhel-7.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR6() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "RHEL-6.*-candidate",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR6_oracle7() {
        return new RealKojiXmlRpcApi(
                "java-1.7.0-oracle",
                "i686",
                "oracle-java-rhel-6.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR5_sun6() {
        return new RealKojiXmlRpcApi(
                "java-1.6.0-sun",
                "x86_64",
                "oracle-java-rhel-5*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR7_oracle8() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-oracle",
                "x86_64",
                "oracle-java-rhel-7.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigWindows() {
        return new RealKojiXmlRpcApi(
                "openjdk8-win",
                "win",
                "openjdk-win*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomWindows() {
        return new RealKojiXmlRpcApi(
                "openjdk8-win",
                "win",
                "openjdk-win*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigWithEmptyArch() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                null,
                "RHEL-6.*-candidate",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigWithOpenJ9() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openj9",
                "x86_64",
                "*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createMultiProductConfig() {
        return new RealKojiXmlRpcApi(
                "java-1.7.0-openjdk java-1.8.0-openjdk java-9-openjdk",
                null,
                "*",
                "",
                null
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
        testListMatchingBuildsCustom(worker, false);
    }

    public void testListMatchingBuildsCustom(KojiListBuilds worker, boolean invertAssert) throws Exception {
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        JavaServer javaServer
                = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(tmpDir);
        try {
            javaServer.start();
            Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
            if (invertAssert){
                Assert.assertFalse(build.isPresent());
            } else {
                Assert.assertTrue(build.isPresent());
            }
        } finally {
            javaServer.stop();
        }
    }

    @Test
    public void testListMatchingBuildsCustomF() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomFedora(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker, true);
    }

    @Test
    public void testListMatchingBuildsCustomF28() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createHydraOnlyList(),
                createConfigCustomFedora28(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsCustomFsrcOnly() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomFedoraSrcOnly(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsCustomWindows() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomWindows(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsCustomRhel() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomRhel7(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker);
    }

    @Test
    public void testListMatchingBuildsMultipleValidUrls() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createKojiBrewList(),
                createConfigMultipleValidUrls(),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(f1, null),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        //KojiBuildDownloader dwldr = new KojiBuildDownloader(createConfigMultipleValidUrls(), new NotProcessedNvrPredicate(new HashSet<>()));
        //dwldr.downloadRPMs(new File("/tmp"), build);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsF() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createKojiOnlyList(),
                createConfigF(),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(f1, null),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR7() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR7(),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e71, null),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR6(),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, null),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR6WithGlobal() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker1 = new KojiListBuilds(createBrewOnlyList(), createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, null), 10);
        Optional<Build> build1 = worker1.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build1.isPresent());
        File gf = File.createTempFile("globalTest", "koji.scm");
        KojiListBuilds worker3 = new KojiListBuilds(createBrewOnlyList(),createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, gf), 10);
        Optional<Build> build3 = worker3.invoke(temporaryFolder.newFolder(), null);
        int a = build1.get().compareTo(build3.get());
        Assert.assertTrue(0 == a);
        Files.write(gf.toPath(), build1.get().getNvr().getBytes("utf-8"));
        KojiListBuilds worker2 = new KojiListBuilds(createBrewOnlyList(),createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, gf), 10);
        Optional<Build> build2 = worker2.invoke(temporaryFolder.newFolder(), null);
        int b = build1.get().compareTo(build2.get());
        Assert.assertTrue(0 != b);
    }

    @Test
    public void testListMatchingBuildsR6_ibm6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR6_ibm6(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR5_ibm6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR5_ibm6(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR7_ibm() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR7_ibm71(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR5_sun6() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR5_sun6(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR6_oracle7() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR6_oracle7(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsR7_oracle8() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigR7_oracle8(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }
    
    @Test
    public void testListMatchingBuildsAnything64_OpenJ9() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createHydraOnlyList(),
                createConfigWithOpenJ9(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testListMatchingBuildsWindows() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigWindows(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testNoArchPresentBuilds() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigWithEmptyArch(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build.isPresent());
    }

    @Test
    public void testNonExistingBuilds() throws IOException, InterruptedException {
        assumeTrue(onRhNet);
        RealKojiXmlRpcApi config = new RealKojiXmlRpcApi(
                "some_random_package_name_that_does_not_exist some_other_package_that_hopefully_also_does_not_exist",
                null,
                "*",
                null,
                null
        );
        KojiListBuilds worker = new KojiListBuilds(
                createKojiOnlyList(),
                config,
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Optional<Build> build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertFalse(build.isPresent());
    }
}

