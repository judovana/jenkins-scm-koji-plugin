package org.fakekoji.jobmanager.bumpers;

import org.fakekoji.DataGenerator;
import org.fakekoji.Utils;
import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.api.http.rest.args.BumpArgs;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.bumpers.impl.ProviderBumper;
import org.fakekoji.jobmanager.model.JobCollisionAction;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.storage.StorageException;
import org.junit.After;
import org.junit.Assert;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProviderBumperTest {

    private static class CurrentSetup {

        private final AccessibleSettings settings;
        private final File oTool;

        private CurrentSetup(AccessibleSettings settings, File oTool) {
            this.settings = settings;
            this.oTool = oTool;
        }
    }


    public CurrentSetup setup(boolean withCvbJobs, boolean withConflictingCvbJobsConflict_Total) throws IOException {
        DataGenerator.withCvbJobs = withCvbJobs;
        DataGenerator.withConflictingCvbJobsConflict_Total = withConflictingCvbJobsConflict_Total;
        JenkinsCliWrapper.killCli();
        File oTool = Files.createTempDirectory("oTool").toFile();
        oTool.deleteOnExit();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(oTool);
        AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        return new CurrentSetup(settings, oTool);
    }

    @After
    public void cleanCvbJobs() throws IOException {
        DataGenerator.withCvbJobs = false;
        DataGenerator.withConflictingCvbJobsConflict_Total = false;
        JenkinsCliWrapper.reinitCli();
    }

    @Test
    public void okBumpJdkJobsNoChildren() throws ManagementException, StorageException, IOException {
        CurrentSetup cs = setup(true, false);
        List<Project> a = getProjects(DataGenerator.PROJECT_VBC_JP);
        JobUpdateResults r1 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkProjectManager, null);//create all
        JobUpdateResults r2 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkTestProjectManager, null);//create all
        String[] nJobsO0 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO0);
        String[] bumpedBuilds = new String[]{
                "build-jdk8-vagrantBeakerConflictsProjectJdkProject-el7.x86_64.vagrant-release.hotspot.sdk",
                "build-jdk8-vagrantBeakerConflictsProjectJdkProject-el7.x86_64.vagrant-slowdebug.hotspot.sdk"
        };
        String[] bumpedTests = new String[]{
                "tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-defaultgc.x11.legacy.lnxagent.jfroff",
                "tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-zgc.x11.legacy.lnxagent.jfroff"
        };
        String po1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        String so1 = Utils.readFile(cfgFile(cs.settings, bumpedBuilds[0], ".vagrant-", ".beaker-"));
        Assert.assertTrue(so1.contains("OTOOL_PLATFORM_PROVIDER=beaker"));
        Assert.assertTrue(so1.contains("<assignedNode>c||d</assignedNode>"));
        String so2 = Utils.readFile(cfgFile(cs.settings, bumpedTests[0], ".vagrant-", ".beaker-"));
        Assert.assertTrue(so2.contains("OTOOL_PLATFORM_PROVIDER=beaker"));
        Assert.assertTrue(so2.contains("<assignedNode>c||d</assignedNode>"));

        ProviderBumper bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*el6.*"));
        Result<JobUpdateResults, OToolError> r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);
        Assert.assertEquals("tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.beaker-defaultgc.x11.legacy.lnxagent.jfroff => tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-defaultgc.x11.legacy.lnxagent.jfroff", r.getValue().jobsCreated.get(0).jobName);
        String[] nJobsO1 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO1);
        Assert.assertArrayEquals(nJobsO0, nJobsO1);

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*build.*el7.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);
        Assert.assertEquals("build-jdk8-vagrantBeakerConflictsProjectJdkProject-el7.x86_64.beaker-release.hotspot.sdk => build-jdk8-vagrantBeakerConflictsProjectJdkProject-el7.x86_64.vagrant-release.hotspot.sdk", r.getValue().jobsCreated.get(0).jobName);
        String[] nJobsO2 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO2);
        Assert.assertArrayEquals(nJobsO0, nJobsO2);

        String po2 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        Assert.assertEquals(po1, po2);

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*el6.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(2, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, true);
        String sb1 = Utils.readFile(cfgFile(cs.settings, bumpedTests[0], "", ""));
        Assert.assertTrue(sb1.contains("OTOOL_PLATFORM_PROVIDER=vagrant"));
        Assert.assertTrue(sb1.contains("<assignedNode>Odin||Tyr||os-el</assignedNode>"));
        Arrays.sort(bumpedTests);
        Assert.assertEquals(r.getValue().jobsCreated.stream().map(b -> b.jobName).sorted().collect(Collectors.toList()), Arrays.asList(bumpedTests));
        String pb1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        Assert.assertNotEquals(po1, pb1);
        String[] nJobsB1 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsB1);
        Assert.assertTrue(nJobsO0.length == nJobsB1.length + 1); //two jobs changed, but one lsot because of conflict
        for (String bt : bumpedTests) {
            Assert.assertTrue(Arrays.binarySearch(nJobsB1, bt) >= 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsB1, bt.replace(".vagrant-", ".beaker-")) < 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsO1, bt.replace(".vagrant-", ".beaker-")) >= 0);
        }

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*build.*el7.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(2, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, true);
        String sb2 = Utils.readFile(cfgFile(cs.settings, bumpedBuilds[0], "", ""));
        Assert.assertTrue(sb2.contains("OTOOL_PLATFORM_PROVIDER=vagrant"));
        Assert.assertTrue(sb2.contains("<assignedNode>Odin||Tyr||os-el</assignedNode>"));
        Arrays.sort(bumpedBuilds);
        Assert.assertEquals(r.getValue().jobsCreated.stream().map(b -> b.jobName).sorted().collect(Collectors.toList()), Arrays.asList(bumpedBuilds));
        String pb2 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        Assert.assertNotEquals(pb1, pb2);
        String[] nJobsB2 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsB2);
        Assert.assertTrue(nJobsB1.length == nJobsB2.length + 1); //two jobs changed, but one lsot because of conflict
        for (String bt : bumpedBuilds) {
            Assert.assertTrue(Arrays.binarySearch(nJobsB2, bt) >= 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsB2, bt.replace(".vagrant-", ".beaker-")) < 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsO1, bt.replace(".vagrant-", ".beaker-")) >= 0);
        }

        //although windows bump is checking bump to net existing provider, there is hidden sub catch
        //dacapo require ohw slave, and  windows are the only one which do not have it
        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*dacapo.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(0, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, true);

        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*dacapo.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(0, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, true);

        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*tck.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);
    }


    @Test
    public void okBumpJdkTestJobs() throws ManagementException, StorageException, IOException {
        CurrentSetup cs = setup(true, false);
        List<Project> a = getProjects(DataGenerator.PROJECT_VBC_JTP);
        JobUpdateResults r1 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkProjectManager, null);//create all
        JobUpdateResults r2 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkTestProjectManager, null);//create all
        String[] nJobsO0 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO0);
        String[] bumpedTests = new String[]{
                "tck-jdk8-vagrantBeakerConflictsProjectJdkTestProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-defaultgc.x11.legacy.lnxagent.jfroff",
                "tck-jdk8-vagrantBeakerConflictsProjectJdkTestProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-zgc.x11.legacy.lnxagent.jfroff"
        };
        String po1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkTestProjects/" + DataGenerator.PROJECT_VBC_JTP + ".json"));

        String so2 = Utils.readFile(cfgFile(cs.settings, bumpedTests[0], ".vagrant-", ".beaker-"));
        Assert.assertTrue(so2.contains("OTOOL_PLATFORM_PROVIDER=beaker"));
        Assert.assertTrue(so2.contains("<assignedNode>c||d</assignedNode>"));

        ProviderBumper bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*el6.*"));
        Result<JobUpdateResults, OToolError> r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);
        Assert.assertEquals("tck-jdk8-vagrantBeakerConflictsProjectJdkTestProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.beaker-defaultgc.x11.legacy.lnxagent.jfroff => tck-jdk8-vagrantBeakerConflictsProjectJdkTestProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-defaultgc.x11.legacy.lnxagent.jfroff", r.getValue().jobsCreated.get(0).jobName);
        String[] nJobsO1 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO1);
        Assert.assertArrayEquals(nJobsO0, nJobsO1);


        String po2 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkTestProjects/" + DataGenerator.PROJECT_VBC_JTP + ".json"));
        Assert.assertEquals(po1, po2);

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*el6.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(2, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, true);
        String sb1 = Utils.readFile(cfgFile(cs.settings, bumpedTests[0], "", ""));
        Assert.assertTrue(sb1.contains("OTOOL_PLATFORM_PROVIDER=vagrant"));
        Assert.assertTrue(sb1.contains("<assignedNode>Odin||Tyr||os-el</assignedNode>"));
        Arrays.sort(bumpedTests);
        Assert.assertEquals(r.getValue().jobsCreated.stream().map(b -> b.jobName).sorted().collect(Collectors.toList()), Arrays.asList(bumpedTests));
        String pb1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkTestProjects/" + DataGenerator.PROJECT_VBC_JTP + ".json"));
        Assert.assertNotEquals(po1, pb1);
        String[] nJobsB1 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsB1);
        Assert.assertTrue(nJobsO0.length == nJobsB1.length + 1); //two jobs changed, but one lsot because of conflict
        for (String bt : bumpedTests) {
            Assert.assertTrue(Arrays.binarySearch(nJobsB1, bt) >= 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsB1, bt.replace(".vagrant-", ".beaker-")) < 0);
            Assert.assertTrue(Arrays.binarySearch(nJobsO1, bt.replace(".vagrant-", ".beaker-")) >= 0);
        }

        //although windows bump is checking bump to net existing provider, there is hidden sub catch
        //dacapo require ohw slave, and  windows are the only one which do not have it
        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*dacapo.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(0, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);

        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*dacapo.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);

        bumper = new ProviderBumper(cs.settings, "beaker", "vagrant", Pattern.compile(".*tck.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(0, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);

        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*tck.*win2019.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        Assert.assertEquals(1, r.getValue().jobsCreated.size());
        Assert.assertEquals(0, r.getValue().jobsArchived.size());
        Assert.assertEquals(0, r.getValue().jobsRevived.size());
        Assert.assertEquals(0, r.getValue().jobsRewritten.size());
        allAre(r.getValue().jobsCreated, false);
    }

    private void allAre(List<JobUpdateResult> jobsCreated, boolean b) {
        int i = -1;
        for (JobUpdateResult job : jobsCreated) {
            i++;
            if (b) {
                Assert.assertTrue("is false, should be true, for " + i + ": " + job.jobName, job.success);
            } else {
                Assert.assertFalse("is true, should be false, for " + i + ": " + job.jobName, job.success);
            }
        }
    }

    private static List<Project> getProjects(String... projectIds) {
        List<Project> a = new ArrayList<>();
        a.addAll(DataGenerator.getJDKProjects());
        a.addAll(DataGenerator.getJDKTestProjects());
        List<Project> r = new ArrayList<>(a.size());
        for (String id : projectIds) {
            for (Project project : a) {
                if (project.getId().equals(id)) {
                    r.add(project);
                    break;
                }
            }
        }
        return r;
    }

    private static File cfgFile(AccessibleSettings settings, String name, String replace, String replacement) {
        File dir = new File(settings.getJenkinsJobsRoot(), name.replace(replace, replacement));
        return new File(dir, "config.xml");
    }


    /**
     * FIXME bump of build paltform with nodes is currently not implemented (you need to tell to the ReverseJDKProjectParser that its build paltfroms are changed)
     * @throws ManagementException
     * @throws StorageException
     * @throws IOException
     */
    @Test
    public void okBumpJdkBuildJobWithKidsWithoutBumpingKids() throws ManagementException, StorageException, IOException {
        CurrentSetup cs = setup(true, false);
        List<Project> a = getProjects(DataGenerator.PROJECT_VBC_JP);
        JobUpdateResults r1 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkProjectManager, null);//create all
        JobUpdateResults r2 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkTestProjectManager, null);//create all
        String[] nJobsO0 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO0);

        String[] bumpedBuilds = new String[]{
                "build-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64.beaker-release.hotspot.sdk",
        };
        String po1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        String so1 = Utils.readFile(cfgFile(cs.settings, bumpedBuilds[0], ".beaker-", ".vagrant-"));
        Assert.assertTrue(so1.contains("OTOOL_PLATFORM_PROVIDER=vagrant"));
        Assert.assertTrue(so1.contains("<assignedNode>Odin||Tyr||os-el</assignedNode>"));

        ProviderBumper bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*build.*el6.*"));
        Result<JobUpdateResults, OToolError> r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isError());
        String[] nJobsO2 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO2);
        Assert.assertArrayEquals(nJobsO0, nJobsO2);

        String po2 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        Assert.assertEquals(po1, po2);


        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*build.*el6.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isError());
    }

    /**
     * FIXME bump of build paltform and its kids is not suported now
     * Todo this, you have to bump all tests to new provider, and once the build is empty, you can bump it
     * @throws ManagementException
     * @throws StorageException
     * @throws IOException
     */

    @Test
    public void okBumpJdkBuildJobWithKidsWithBumpingKids() throws ManagementException, StorageException, IOException {
        CurrentSetup cs = setup(true, false);
        List<Project> a = getProjects(DataGenerator.PROJECT_VBC_JP);
        JobUpdateResults r1 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkProjectManager, null);//create all
        JobUpdateResults r2 = cs.settings.getJobUpdater().regenerateAll(null, cs.settings.getConfigManager().jdkTestProjectManager, null);//create all
        String[] nJobsO0 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO0);

        String[] bumpedBuilds = new String[]{
                "build-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64.beaker-release.hotspot.sdk",
                "tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-defaultgc.x11.legacy.lnxagent.jfroff",
                "tck-jdk8-vagrantBeakerConflictsProjectJdkProject-el6.x86_64-release.hotspot.sdk-el6.x86_64.vagrant-zgc.x11.legacy.lnxagent.jfroff"
        };
        String po1 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        String so1 = Utils.readFile(cfgFile(cs.settings, bumpedBuilds[0], ".beaker-", ".vagrant-"));
        Assert.assertTrue(so1.contains("OTOOL_PLATFORM_PROVIDER=vagrant"));
        Assert.assertTrue(so1.contains("<assignedNode>Odin||Tyr||os-el</assignedNode>"));

        ProviderBumper bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*el6.*"));
        Result<JobUpdateResults, OToolError> r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.STOP, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isOk());
        String[] nJobsO2 = cs.settings.getJenkinsJobsRoot().list();
        Arrays.sort(nJobsO2);
        Assert.assertArrayEquals(nJobsO0, nJobsO2);

        String po2 = Utils.readFile(new File(cs.settings.getConfigRoot(), "jdkProjects/" + DataGenerator.PROJECT_VBC_JP + ".json"));
        Assert.assertEquals(po1, po2);


        bumper = new ProviderBumper(cs.settings, "vagrant", "beaker", Pattern.compile(".*el6.*"));
        r = bumper.modifyJobs(a, new BumpArgs(JobCollisionAction.KEEP_BUMPED, true));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isError());
    }
}
