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
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.fakekoji.xmlrpc.server.JavaServer;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
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

        /* see: https://stackoverflow.com/questions/4596447/check-if-file-exists-on-remote-server-using-its-url */
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://brewweb.engineering.redhat.com/brew/");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            onRhNet = (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            onRhNet = false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void createDir(File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static void createFile(File file, String content) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            try (PrintStream ps = new PrintStream(new FileOutputStream(file))) {
                ps.println(content);
            }
        }
    }

    /* Because fake-koji tests if file is > 5 bytes */
    public static void createNonEmptyFile(File file) throws IOException {
        createFile(file, "Ententýky dva špalíky, čert vyletěl z elektriky!\n");
    }

    public static void generateBuilds(File root, String n, String v, String r, String a, String... builds) throws Exception {
        createDir(root);
        File nDir = new File(root, n);
        createDir(nDir);
        File vDir = new File(nDir, v);
        createDir(vDir);
        File rDir = new File(vDir, r);
        createDir(rDir);
        File aDir = new File(rDir, a);
        createDir(aDir);
        for (String build : builds) {
            File buildFile = new File(aDir, build);
            createNonEmptyFile(buildFile);
        }
        File dataDir = new File(rDir, "data");
        createDir(dataDir);
        File logsDir = new File(dataDir, "logs");
        createDir(logsDir);
        File logsADir = new File(logsDir, a);
        createDir(logsADir);

        File buildLogFile = new File(logsADir, "build.log");
        if (!buildLogFile.exists()) {
            createNonEmptyFile(buildLogFile);
        }
    }

    public static void generateUpstreamRepo(File root, String repoName) throws Exception {
        if (!root.exists()) {
            root.mkdir();
        }
        File repoDir = new File(root, repoName);
        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
    }

    public static void generateFakeKojiData(File localBuilds, File upstreamRepos) throws Exception {
        String n = "java-1.8.0-openjdk";
        String v = "jdk8u121.b13";
        String rbase = "52.dev";
        String r1 = rbase + ".upstream";
        String r2 = rbase + ".upstream.fastdebug";
        String a1 = "src";
        String a2 = "x86_64";
        String a3 = "win";
        String a4 = "i686";

        String suffix = ".tarxz";

        String buildCommon = n + "-" + v + "-" + rbase;
        String build11 = buildCommon + ".upstream." + a1 + suffix;
        String build12 = buildCommon + ".static." + a2 + suffix;
        String build13 = buildCommon + ".static." + a3 + suffix;
        String build14 = buildCommon + ".static." + a4 + suffix;

        String build21 = buildCommon + ".upstream.fastdebug." + a1 + suffix;
        String build22 = buildCommon + ".static.fastdebug." + a2 + suffix;
        String build23 = buildCommon + ".static.fastdebug." + a3 + suffix;
        String build24 = buildCommon + ".static.fastdebug." + a4 + suffix;

        /* upstream builds */
        generateBuilds(localBuilds, n, v, r1, a1, build11);
        generateBuilds(localBuilds, n, v, r1, a2, build12);
        generateBuilds(localBuilds, n, v, r1, a3, build13);
        generateBuilds(localBuilds, n, v, r1, a4, build14);

        /* fastdebug builds */
        generateBuilds(localBuilds, n, v, r2, a1, build21);
        generateBuilds(localBuilds, n, v, r2, a2, build22);
        generateBuilds(localBuilds, n, v, r2, a3, build23);
        generateBuilds(localBuilds, n, v, r2, a4, build24);

        /* create link for windows builds */
        File nFile = new File(localBuilds, n);
        File winLinkFile = new File(localBuilds, "openjdk8-win");
        Files.createSymbolicLink(winLinkFile.toPath(), localBuilds.toPath().relativize(nFile.toPath()));

        File expectedArches = new File(localBuilds, "java-1.8.0-openjdk-arches-expected");
        createFile(expectedArches, a2 + " " + a3 + " " + a4 + "\n");

        generateUpstreamRepo(upstreamRepos, "java-1.8.0-openjdk-dev");
    }

    public void testListMatchingBuildsCustom(KojiListBuilds worker) throws Exception {
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        File localBuilds = new File(tmpDir, "local-builds");
        File upstreamRepos = new File(tmpDir, "upstream-repos");
        generateFakeKojiData(localBuilds, upstreamRepos);
        JavaServer javaServer = new JavaServer(localBuilds, upstreamRepos,
                JavaServer.DFAULT_RP2C_PORT, JavaServer.DFAULT_DWNLD_PORT,
                JavaServer.DFAULT_SSHUPLOAD_PORT, 8080);
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

//FIXME enable those three tests by launching solid test server with some fake data
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
    //this testis currently  very broken. The project is openjdk8-win instead of expected java-1.8.0-openjdk. Needs serious investigations
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
}
