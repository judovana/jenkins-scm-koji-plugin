package org.fakekoji.api.xmlrpc;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.Nvr;
import org.fakekoji.api.ssh.TestSshApi;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.FakeBuild;
import org.fakekoji.core.FakeKojiDB;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.FakeBuildList;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class NewApiTests {

    private static AccessibleSettings as;
    private static FakeKojiDB kojiDB;
    private static File dbRoot;

    @BeforeClass
    public static void prepareDb() throws IOException {
        dbRoot = File.createTempFile("fakeKoji", "dbRoot");
        dbRoot.delete();
        dbRoot.mkdir();
        File builds = new File(dbRoot, "builds");
        builds.mkdir();
        File repos = new File(dbRoot, "repos");
        repos.mkdir();
        String[] archEnum = new String[]{"src", "el7_x86_64", "el6_i686", "win10", "win2016s", "win2012_x86_64", "el8_ppc64le", "el7_aarch64"};
        String[] projects = new String[]{"java-1.8.0-openjdk-shenandoah", "java-1.8.0-openjdk", "java-X-openjdk-shenandoah", "java-12-openjdk-updates", "java-1.7.0-openjdk-forest", "java-1.8.0-openjdk-aarch64-shenandoah", "java-11-openjdk-updates", "java-12-openjdk", "java-1.7.0-openjdk-forest-26", "java-1.8.0-openjdk-dev", "java-1.7.0-openjdk", "java-11-openjdk-shenandoah", "java-9-openjdk", "java-1.8.0-openjdk-aarch64", "java-10-openjdk", "java-9-openjdk-shenandoah", "java-X-openjdk", "java-11-openjdk", "thermostat-ng", "java-9-openjdk-updates", "java-9-openjdk-dev", "java-10-openjdk-updates"};
        for (String project : projects) {
            File p = new File(repos, project);
            p.mkdir();
            Files.write(new File(p, "something").toPath(), Arrays.asList(new String[]{"nothing", project, p.getAbsolutePath()}), StandardCharsets.UTF_8);
            //currenlty mandatory to enumerate arches
            //TODO, fix, once configs will be in place
            //the arches_expected in repos should go away
            Files.write(new File(p, FakeBuild.archesConfigFileName).toPath(), Arrays.asList(new String[]{String.join(" ", archEnum)}), StandardCharsets.UTF_8);

        }
        String[] products = new String[]{"java-openjdk", "java-1.8.0-openjdk", "java-12-openjdk", "java-1.7.0-openjdk", "java-9-openjdk", "java-10-openjdk", "openjdk8-win", "java-X-openjdk", "java-11-openjdk"};
        for (String product : products) {
            File p = new File(builds, product);
            p.mkdir();
            for (String v : new String[]{"v1", "v2"}) {
                File d = new File(p, v);
                d.mkdir();
                List<String> releases = new ArrayList<>();
                for (String project : projects) {
                    if (project.contains(product)) {
                        String sb = project.replaceAll(product + "-*", "").replaceAll("-", ".");
                        if (!sb.isEmpty()) {
                            sb = "." + sb;
                        }
                        releases.add("r1" + sb);
                        releases.add("r2" + sb);
                        releases.add("r1" + sb + ".hotspot");
                        releases.add("r2" + sb + ".hotspot");
                        releases.add("r1" + sb + ".zero");
                        releases.add("r2" + sb + ".zero");
                        releases.add("r1" + sb + ".openj9");
                        releases.add("r2" + sb + ".openj9");
                        releases.add("r1" + sb + ".release");
                        releases.add("r2" + sb + ".release");
                        releases.add("r1" + sb + ".slowdebug");
                        releases.add("r2" + sb + ".slowdebug");
                        releases.add("r1" + sb + ".fastdebug");
                        releases.add("r2" + sb + ".fastdebug");
                        releases.add("r1" + sb + ".release.hotspot");
                        releases.add("r2" + sb + ".release.hotspot");
                        releases.add("r1" + sb + ".slowdebug.hotspot");
                        releases.add("r2" + sb + ".slowdebug.hotspot");
                        releases.add("r1" + sb + ".fastdebug.hotspot");
                        releases.add("r2" + sb + ".fastdebug.hotspot");
                        releases.add("r1" + sb + ".release.zero");
                        releases.add("r2" + sb + ".release.zero");
                        releases.add("r1" + sb + ".slowdebug.zero");
                        releases.add("r2" + sb + ".slowdebug.zero");
                        releases.add("r1" + sb + ".fastdebug.zero");
                        releases.add("r2" + sb + ".fastdebug.zero");
                        releases.add("r1" + sb + ".release.openj9");
                        releases.add("r2" + sb + ".release.openj9");
                    }
                }
                for (String r : releases) {
                    File dd = new File(d, r);
                    dd.mkdir();
                    File dataDir = new File(dd, "data");
                    dataDir.mkdir();
                    Files.write(new File(dataDir, "info.log").toPath(), Arrays.asList(new String[]{product, v, r, dd.getAbsolutePath()}), StandardCharsets.UTF_8);
                    for (String arch : archEnum) {
                        File archDir = new File(dd, arch);
                        archDir.mkdir();
                        Files.write(new File(archDir, product + "-" + v + "-" + r + "." + arch + ".tarxz").toPath(), Arrays.asList(new String[]{product, v, r, arch, archDir.getAbsolutePath()}), StandardCharsets.UTF_8);
                    }
                }
            }
        }
        //make java-1.8.0-openjdk-v2-r2.aarch64.shenandoah.release as rpms
        File d3 = new File(builds.getAbsolutePath() + "/java-1.8.0-openjdk/v2/r2.aarch64.shenandoah.release");
        for (String arch : archEnum) {
            File d3ArchDir = new File(d3, arch);
            File d3ArchFileSrcc = new File(d3ArchDir, "java-1.8.0-openjdk-v2-r2.aarch64.shenandoah.release." + arch + ".tarxz");
            File d3ArchFileDest = new File(d3ArchDir, "java-1.8.0-openjdk-v2-r2.aarch64.shenandoah.release." + arch + ".rpm");
            if (!d3ArchFileSrcc.renameTo(d3ArchFileDest)) {
                throw new RuntimeException("cant rename test item 3");
            }
        }
        //make java-1.8.0-openjdk-v1-r1.aarch64.shenandoah.release.zero as notyet built
        File d2 = new File(builds.getAbsolutePath() + "/java-1.8.0-openjdk/v1/r1.aarch64.shenandoah.release.zero");
        File d2ArchDir = new File(d2, "el7_x86_64");
        File d2ArchFile = new File(d2ArchDir, "java-1.8.0-openjdk-v1-r1.aarch64.shenandoah.release.zero.el7_x86_64.tarxz");
        if (!d2ArchFile.delete()) {
            throw new RuntimeException("cant remove test item 2");
        }
        //make java-11-openjdk-v2-r2.shenandoah.slowdebug failed
        File d1 = new File(builds.getAbsolutePath() + "/java-11-openjdk/v2/r2.shenandoah.slowdebug/");
        File d1ArchDir = new File(d1, "el7_x86_64");
        File d1ArchFile = new File(d1ArchDir, "java-11-openjdk-v2-r2.shenandoah.slowdebug.el7_x86_64.tarxz");
        if (!d1ArchFile.delete()) {
            throw new RuntimeException("cant remove test item1");
        }
        File d1failFile = new File(d1ArchDir, "FAILED");
        d1failFile.createNewFile();
        as = new AccessibleSettings(builds, repos, 9848, 9849, 9822, 8000, 8080);
        kojiDB = new FakeKojiDB(as);
    }

    @AfterClass
    public static void rmDb() {
        dbRoot.deleteOnExit();
    }

    @Test
    public void slowdebugHotspot() throws IOException, InterruptedException {
        GetBuildList gbl = new GetBuildList("java-11-openjdk-shenandoah", "hotspot", "slowdebug", true);
        final GetBuildList getBuildListParams = GetBuildList.create(gbl);//??
        //kojiDB.getArchives( )
        List<Nvr> r = kojiDB.getBuildList(getBuildListParams);
        //there is 8 mathcing records, however we removed one to be failed
        //shouldbe missing java-11-openjdk-v2-r2.shenandoah.slowdebug
        Assert.assertEquals(7, r.size());
    }

    @Test
    public void releaseZero() throws IOException, InterruptedException {
        GetBuildList gbl = new GetBuildList("java-1.8.0-openjdk-aarch64-shenandoah", "zero", "release", true);
        final GetBuildList getBuildListParams = GetBuildList.create(gbl);//??
        //kojiDB.getArchives( )
        List<Nvr> r = kojiDB.getBuildList(getBuildListParams);
        //java-1.8.0-openjdk-v1-r1.aarch64.shenandoah.release.zero
        //is forced to be not yet build in setup
        Assert.assertEquals(7, r.size());
    }

    @Test
    public void releaseZeroToBeBUild() throws IOException, InterruptedException {
        GetBuildList gbl = new GetBuildList("java-1.8.0-openjdk-aarch64-shenandoah", "zero", "release", false);
        final GetBuildList getBuildListParams = GetBuildList.create(gbl);//??
        //kojiDB.getArchives( )
        List<Nvr> r = kojiDB.getBuildList(getBuildListParams);
        //java-1.8.0-openjdk-v1-r1.aarch64.shenandoah.release.zero
        //is forced to be not yet build in setup
        Assert.assertEquals(1, r.size());
    }


    @Test
    public void oneGoneToOldApi() throws IOException, InterruptedException {
        GetBuildList gbl = new GetBuildList("java-1.8.0-openjdk-aarch64-shenandoah", "hotspot", "release", true);
        final GetBuildList getBuildListParams = GetBuildList.create(gbl);//??
        //kojiDB.getArchives( )
        List<Nvr> r = kojiDB.getBuildList(getBuildListParams);
        //there is 16 release of aarch64-shenandoah forjjdk8 hotspot (release and nothing are doublig it)
        //hoewer java-1.8.0-openjdk-v2-r2.aarch64.shenandoah.release are rpms, thuis old api only
        Assert.assertEquals(15, r.size());
    }

    @Test
    public void oneForOldapi() throws IOException, InterruptedException {
        GetBuildList gbl = new GetBuildList("java-1.8.0-openjdk-aarch64-shenandoah", "hotspot", "release", true);
        List<Build> r = kojiDB.getProjectBuilds(kojiDB.getPkgId("java-1.8.0-openjdk"), new HashSet<>());
        //hoewer java-1.8.0-openjdk-v2-r2.aarch64.shenandoah.release are rpms, thuis old api only
        Assert.assertEquals(1, r.size());
    }
}
