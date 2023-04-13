package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.NotProcessedNvrPredicate;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import org.fakekoji.xmlrpc.server.expensiveobjectscache.RemoteRequestCacheConfigKeys;
import org.junit.BeforeClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

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
                "http://hydra.brq.redhat.com:" + JavaServerConstants.DFAULT_RP2C_PORT + "/RPC2",
                "http://hydra.brq.redhat.com:" + JavaServerConstants.DFAULT_DWNLD_PORT
        );
    }

    static KojiBuildProvider createLocalhostKojiBuildProvider() {
        return new KojiBuildProvider(
                "http://localhost:" + JavaServerConstants.DFAULT_RP2C_PORT+ "/RPC2",
                "http://localhost:" + JavaServerConstants.DFAULT_DWNLD_PORT
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

    //for some reason, this maps to new api
     RealKojiXmlRpcApi createConfigCustomFedora28() {
        return new RealKojiXmlRpcApi(
                "java-11-openjdk",
                "x86_64,src",
                "f28.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigHydrasIbm() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-ibm",
                "x86_64,src",
                ".*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomFedoraSlowdebug() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64",
                "slowdebug-f24-.*",
                "",
                null
        );
    }
    RealKojiXmlRpcApi createConfigCustomFedoraRelease() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64",
                "f24-.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomFedoraSrcOnly() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "src",
                "f24.*",
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
                "f24.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigMultipleValidUrls() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64,src",
                "f24.*",
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
                "rhel-6.*-supp.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigR5_ibm6() {
        return new RealKojiXmlRpcApi(
                "java-1.6.0-ibm",
                "i386",
                "dist-5.*-extras.*",
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
                "oracle-java-rhel-5.*",
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
                "openjdk-win.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigWindowsNewName() {
        return new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "win",
                "openjdk-win.*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createConfigCustomWindows() {
        return new RealKojiXmlRpcApi(
                "openjdk8-win",
                "win",
                "openjdk-win.*",
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
                ".*",
                "",
                null
        );
    }

    RealKojiXmlRpcApi createMultiProductConfig() {
        return new RealKojiXmlRpcApi(
                "java-1.7.0-openjdk java-1.8.0-openjdk java-9-openjdk",
                null,
                ".*",
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
        //this class shold be the only test really pooling the xmlrpc
        //thus we are creating dummy cache file, to enable at least default caching
        //it is speeding up the run by 10 minutes on rhnet
        File cacheConfig = new File(RemoteRequestCacheConfigKeys.DEFAULT_CONFIG_LOCATION.getAbsolutePath());
        if (!cacheConfig.exists()) {
            cacheConfig.createNewFile();
        }
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
            Build build = worker.invoke(temporaryFolder.newFolder(), null);
            if (invertAssert){
                Assert.assertFalse(build != null);
            } else {
                Assert.assertTrue(build != null);
            }
        } finally {
            javaServer.stop();
        }
    }

    @Test
    public void testListMatchingBuildsCustomFedoraSlowDebug() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomFedoraSlowdebug(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker, true);
    }

    @Test
    public void testListMatchingBuildsCustomFedoraRelease() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createLocalhostOnlyList(),
                createConfigCustomFedoraRelease(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        testListMatchingBuildsCustom(worker, false);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }

    @Test
    /**
     * Probably the only test really testing old api on fake koji
     * Afaik the old api is now severe broken, as upstream keyword is in all projects
     * If there will ever need to try openjdk rpms via old api, the tagging will need to be revisited,
     * and the logic of usptream/static first fc/el last inverted (see this commit in FakeBuild.guessTags
     *
     * Removing it completely, will need adapt the tests. To invert old api hydra tests to match null build is bad idea
     */
    public void testListMatchingBuildsCustomIbm8() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createHydraOnlyList(),
                createConfigHydrasIbm(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
        File ff  =temporaryFolder.newFolder();
        KojiBuildDownloader dwldr = new KojiBuildDownloader(
                createKojiBrewList(),
                createConfigMultipleValidUrls(),  new NotProcessedNvrPredicate(new ArrayList<String>()),
                build,ff.getAbsolutePath(),
                2,
                false,
                false);
        dwldr.invoke(ff, null);
        final boolean[] found  = new boolean[]{false};
        final List<Path> rpms  = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(ff.toPath())) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        if (path.endsWith("metadata.file.1.json")){
                            found[0] = true;
                        }
                        if (path.toString().endsWith(".rpm")){
                            rpms.add(path);
                        }
                    });
        }
        Assert.assertFalse(found[0]);
        Assert.assertTrue(rpms.size()>10);
    }

    @Test
    public void testSanitizeBadDate() {
        Assert.assertEquals("2017-03-01 01:10:54.934384" , BuildMatcher.sanitizeBadKojiDate("2017-03-01 01:10:54.934384+00:00"));
        Assert.assertEquals("2017-03-01 01:10:54.934384" , BuildMatcher.sanitizeBadKojiDate("2017-03-01 01:10:54.934384"));
        Assert.assertEquals("blah+123456abc" , BuildMatcher.sanitizeBadKojiDate("blah+123456abc"));
        Assert.assertEquals("blah+abc" , BuildMatcher.sanitizeBadKojiDate("blah+abc"));
        Assert.assertEquals("blah+123456" , BuildMatcher.sanitizeBadKojiDate("blah+123456"));
        Assert.assertEquals("blah+123:456" , BuildMatcher.sanitizeBadKojiDate("blah+123:456"));
        Assert.assertEquals("blah" , BuildMatcher.sanitizeBadKojiDate("blah+12:45"));
        Assert.assertEquals("blah" , BuildMatcher.sanitizeBadKojiDate("blah+1:4"));
    }


    @Test
    public void testListMatchingBuildsF() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(
                createKojiOnlyList(),
                createConfigF(),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(f1, null),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }


    @Test
    public void testJmc() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                new RealKojiXmlRpcApi(
                        "jmc",
                        "x86_64 noarch",
                        "(supp-|)rhel-9\\.[0-9]\\.[0-9]-(alpha-candidate|nocompose-candidate|candidate|gate|beta-gate) rhaos-.*-rhel-9-container-candidate epel9.* openj9-1-rhel-9-candidate",
                        ".*src.zip .*test.zip .*openjdksrc.zip$ .*-jre-.*windows.* .*jre.win.* .*-portable-[b\\d\\.\\-ea]{3,}el.openjdkportable.*",
                        null
                ),
                NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e71, null),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }

    @Test
    public void testListMatchingBuildsR6WithGlobal() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker1 = new KojiListBuilds(createBrewOnlyList(), createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, null), 10);
        Build build1 = worker1.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build1 != null);
        File gf = File.createTempFile("globalTest", "koji.scm");
        KojiListBuilds worker3 = new KojiListBuilds(createBrewOnlyList(),createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, gf), 10);
        Build build3 = worker3.invoke(temporaryFolder.newFolder(), null);
        int a = build1.compareTo(build3);
        Assert.assertTrue(0 == a);
        Files.write(gf.toPath(), build1.getNvr().getBytes("utf-8"));
        KojiListBuilds worker2 = new KojiListBuilds(createBrewOnlyList(),createConfigR6(), NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(e61, gf), 10);
        Build build2 = worker2.invoke(temporaryFolder.newFolder(), null);
        int b = build1.compareTo(build2);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }
    
    @Test
    public void testListMatchingBuildsAnything64_OpenJ9() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigWithOpenJ9(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }

    @Test
    public void testListMatchingBuildsWindowsNewJdk8Name() throws Exception {
        assumeTrue(onRhNet);
        KojiListBuilds worker = new KojiListBuilds(
                createBrewOnlyList(),
                createConfigWindowsNewName(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                1
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
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
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
    }

    @Test
    public void testNonExistingBuilds() throws IOException, InterruptedException {
        assumeTrue(onRhNet);
        RealKojiXmlRpcApi config = new RealKojiXmlRpcApi(
                "some_random_package_name_that_does_not_exist some_other_package_that_hopefully_also_does_not_exist",
                null,
                ".*",
                null,
                null
        );
        KojiListBuilds worker = new KojiListBuilds(
                createKojiOnlyList(),
                config,
                new NotProcessedNvrPredicate(new ArrayList<>()),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertFalse(build != null);
    }

    private  final String[] containersUbi8Now = new String[] {
            //openjdk-11-ubi8-container-1.10-10 intentionally not here
    "openjdk-11-ubi8-container-1.10-1",
    "openjdk-11-ubi8-container-1.10-1.1634738701",
    "openjdk-11-ubi8-container-1.10-2",
    "openjdk-11-ubi8-container-1.10-3",
    "openjdk-11-ubi8-container-1.10-4",
    "openjdk-11-ubi8-container-1.10-5",
    "openjdk-11-ubi8-container-1.10-6",
    "openjdk-11-ubi8-container-1.10-7",
    "openjdk-11-ubi8-container-1.10-8",
    "openjdk-11-ubi8-container-1.10-9",
    "openjdk-11-ubi8-container-1.3-1",
    "openjdk-11-ubi8-container-1.3-10",
    "openjdk-11-ubi8-container-1.3-10.1618412959",
    "openjdk-11-ubi8-container-1.3-11",
    "openjdk-11-ubi8-container-1.3-12",
    "openjdk-11-ubi8-container-1.3-13",
    "openjdk-11-ubi8-container-1.3-14",
    "openjdk-11-ubi8-container-1.3-15",
    "openjdk-11-ubi8-container-1.3-15.1622639262",
    "openjdk-11-ubi8-container-1.3-15.1622643823",
    "openjdk-11-ubi8-container-1.3-16",
    "openjdk-11-ubi8-container-1.3-16.1626836231",
    "openjdk-11-ubi8-container-1.3-16.1626857775",
    "openjdk-11-ubi8-container-1.3-16.1627000335",
    "openjdk-11-ubi8-container-1.3-16.1627034239",
    "openjdk-11-ubi8-container-1.3-16.1627035227",
    "openjdk-11-ubi8-container-1.3-16.1627041171",
    "openjdk-11-ubi8-container-1.3-17",
    "openjdk-11-ubi8-container-1.3-18",
    "openjdk-11-ubi8-container-1.3-18.1634738691",
    "openjdk-11-ubi8-container-1.3-2",
    "openjdk-11-ubi8-container-1.3-3",
    "openjdk-11-ubi8-container-1.3-3.1591609340",
    "openjdk-11-ubi8-container-1.3-3.1592811766",
    "openjdk-11-ubi8-container-1.3-3.1593114401",
    "openjdk-11-ubi8-container-1.3-3.1594890755",
    "openjdk-11-ubi8-container-1.3-3.1595332543",
    "openjdk-11-ubi8-container-1.3-3.1599573774",
    "openjdk-11-ubi8-container-1.3-4",
    "openjdk-11-ubi8-container-1.3-4.1594890812",
    "openjdk-11-ubi8-container-1.3-4.1595335747",
    "openjdk-11-ubi8-container-1.3-4.1599573721",
    "openjdk-11-ubi8-container-1.3-4.1604569229",
    "openjdk-11-ubi8-container-1.3-5",
    "openjdk-11-ubi8-container-1.3-6",
    "openjdk-11-ubi8-container-1.3-6.1604582405",
    "openjdk-11-ubi8-container-1.3-7",
    "openjdk-11-ubi8-container-1.3-8",
    "openjdk-11-ubi8-container-1.3-8.1608081508",
    "openjdk-11-ubi8-container-1.3-9",
    "openjdk-11-ubi8-container-1.3-9.1614713191"};

    @Test
    public void ubi8jdk11containerRuntime() throws Exception {
        RealKojiXmlRpcApi description = new RealKojiXmlRpcApi(
                "openjdk-11-runtime-ubi8-container",
                "ppc64le",
                "(supp-|)rhel-8\\.5\\.[0-9]-z-(nocompose-candidate|candidate|gate) openj9-1-rhel-8-candidate rhaos-.*-rhel-8-container-candidate epel8.*",
                ".*-debuginfo-.* .*-debugsource-.* .*src.zip  .*-jmods-.* .*static-libs.* .*-devel-.* .*-static-libs-.* .*-openjdk[b\\d\\.\\-]{3,}(ea.windows.redhat|ea.redhat.windows).* .*-fastdebug-.* .*-slowdebug-.* .*-debug-.*",
                ""
        );
        KojiListBuilds worker = new KojiListBuilds(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description,
                new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
        File target = File.createTempFile("fakeKoji", "testDir");
        target.delete();
        target.mkdir();
        KojiBuildDownloader dwldr = new KojiBuildDownloader(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description, new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                build,target.getAbsolutePath(),
                10,
                false,
                false);
        List l = dwldr.downloadRPMs(target.getAbsoluteFile(), build, description);
        Assert.assertTrue(l.size() == 1);
    }

    @Test
    public void ubi8jdk11container() throws Exception {
        assumeTrue(onRhNet);
        RealKojiXmlRpcApi description = new RealKojiXmlRpcApi(
                "openjdk-11-ubi8-container",
                "ppc64le",
                "(supp-|)rhel-8\\.5\\.[0-9]-z-(nocompose-candidate|candidate|gate) openj9-1-rhel-8-candidate rhaos-.*-rhel-8-container-candidate epel8.*",
                ".*-debuginfo-.* .*-debugsource-.* .*src.zip  .*-jmods-.* .*static-libs.* .*-jre-.*windows.* .*jre.win.* .*-portable-[b\\d\\.\\-ea]{3,}el.openjdkportable.*",
                ""
        );
        KojiListBuilds worker = new KojiListBuilds(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description,
                new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                10
        );
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        Assert.assertTrue(build != null);
        File target = File.createTempFile("fakeKoji", "testDir");
        target.delete();
        target.mkdir();
        KojiBuildDownloader dwldr = new KojiBuildDownloader(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description, new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                build,target.getAbsolutePath(),
                10,
                false,
                false);
        List l = dwldr.downloadRPMs(target.getAbsoluteFile(), build, description);
        Assert.assertTrue(l.size() == 1);
    }

    @Test
    public void ubi8jdk11containerMetadata() throws Exception {
        assumeTrue(onRhNet);
        RealKojiXmlRpcApi description = new RealKojiXmlRpcApi(
                "openjdk-11-ubi8-container",
                "ppc64le",
                "(supp-|)rhel-8\\.5\\.[0-9]-z-(nocompose-candidate|candidate|gate) openj9-1-rhel-8-candidate rhaos-.*-rhel-8-container-candidate epel8.*",
                ".*-debuginfo-.* .*-debugsource-.* .*src.zip  .*-jmods-.* .*static-libs.* .*-jre-.*windows.* .*jre.win.* .*-portable-[b\\d\\.\\-ea]{3,}el.openjdkportable.*",
                ""
        );
        KojiListBuilds worker = new KojiListBuilds(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description,
                new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                2
        );
        File ff  =temporaryFolder.newFolder();
        Build build = worker.invoke(ff, null);
        Assert.assertTrue(build != null);
        File target = File.createTempFile("fakeKoji", "testDir");
        target.delete();
        target.mkdir();
        KojiBuildDownloader dwldr = new KojiBuildDownloader(
                Collections.singletonList(createBrewHubKojiBuildProvider()),
                description, new NotProcessedNvrPredicate(Arrays.asList(containersUbi8Now)),
                build,target.getAbsolutePath(),
                2,
                false,
                false);
        dwldr.invoke(ff, null);
        final boolean[] found  = new boolean[]{false};
        try (Stream<Path> stream = Files.walk(ff.toPath())) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        if (path.endsWith("metadata.file.1.json")){
                            found[0] = true;
                        }
                    });
        }
        Assert.assertTrue(found[0]);
    }
}

