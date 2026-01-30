/*
 * The MIT License
 *
 * Copyright 2017 jvanek.
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
package org.fakekoji.jobmanager;

import org.fakekoji.DataGenerator;
import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobBump;
import org.fakekoji.jobmanager.model.JobCollisionAction;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.storage.StorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.fakekoji.jobmanager.JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE;

/**
 * Warning - reaming check have missing check on content!
 *
 * @author jvanek
 */
public class JenkinsJobUpdaterTest {

    boolean wasFinallyRun = false;

    @TempDir
    static Path temporaryFolder;

    private AccessibleSettings settings;
    private DataGenerator.FolderHolder folderHolder;
    private JobUpdater jobUpdater;

    final Job from = new DummyJob("from");
    final Job to = new DummyJob("to");
    JenkinsJobUpdater updater;
    File jobs;
    File archive;
    File fromFile;
    File fromConfig;
    File toFile;
    File toConfig;

    @BeforeEach
    public void setup() throws IOException {
        folderHolder = DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile());
        settings = DataGenerator.getSettings(folderHolder);
        jobUpdater = settings.getJobUpdater();
        updater = (JenkinsJobUpdater) jobUpdater;
        jobs = settings.getJenkinsJobsRoot();
        archive = settings.getJenkinsJobArchiveRoot();
        fromFile = new File(jobs, from.getName());
        toFile = new File(jobs, to.getName());
        fromConfig = new File(fromFile, JENKINS_JOB_CONFIG_FILE);
        toConfig = new File(toFile, JENKINS_JOB_CONFIG_FILE);
    }

    @Test()
    public void firtsWin() throws Throwable {
        Assertions.assertThrows(IOException.class, () -> {

            wasFinallyRun = false;
            try {
                String r = new JenkinsJobUpdater.PrimaryExceptionThrower<String>(new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {
                        throw new IOException();
                    }
                }, new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {
                        wasFinallyRun = true;
                        throw new InterruptedException();
                    }
                }, "returned").call();
                Assertions.assertNull(r);
            } finally {
                Assertions.assertTrue(wasFinallyRun);
            }
        });
    }

    @Test()
    public void returnOnPass() throws Throwable {
        wasFinallyRun = false;
        try {
            String r = new JenkinsJobUpdater.PrimaryExceptionThrower<String>(new JenkinsJobUpdater.Rummable() {
                @Override
                public void rum() throws Exception {

                }
            }, new JenkinsJobUpdater.Rummable() {
                @Override
                public void rum() throws Exception {
                    wasFinallyRun = true;

                }
            }, "returned").call();
            Assertions.assertEquals("returned", r);
        } finally {
            Assertions.assertTrue(wasFinallyRun);
        }
    }

    @Test()
    public void firstExceptionKills() throws Throwable {
        Assertions.assertThrows(IOException.class, () -> {
            wasFinallyRun = false;
            try {
                String r = new JenkinsJobUpdater.PrimaryExceptionThrower<String>(new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {
                        throw new IOException();
                    }
                }, new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {
                        wasFinallyRun = true;
                    }
                }, "returned").call();
                Assertions.assertNull(r);
            } finally {
                Assertions.assertTrue(wasFinallyRun);
            }
        });
    }

    public void secondExceptionAlsoKills() throws Throwable {
        Assertions.assertThrows(InterruptedException.class, () -> {
            wasFinallyRun = false;
            try {
                String r = new JenkinsJobUpdater.PrimaryExceptionThrower<String>(new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {

                    }
                }, new JenkinsJobUpdater.Rummable() {
                    @Override
                    public void rum() throws Exception {
                        wasFinallyRun = true;
                        throw new InterruptedException();
                    }
                }, "returned").call();
                Assertions.assertNull(r);
            } finally {
                Assertions.assertTrue(wasFinallyRun);
            }
        });
    }

    @Test
    public void regenerateAllJDKProject() throws ManagementException, StorageException {
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkProjectManager,null);//create all
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkProjectManager, null);//re create all
            Assertions.assertEquals(0, r1.jobsRewritten.size());
            Assertions.assertNotEquals(0, r1.jobsCreated.size());
            Assertions.assertNotEquals(0, r2.jobsRewritten.size());
            Assertions.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void regenerateAllJDKProjectWithWhitelist() throws ManagementException, StorageException {
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkProjectManager, "somethingNotExisting");//create nothing
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkProjectManager, null);//re create all
            Assertions.assertEquals(0, r1.jobsRewritten.size());
            Assertions.assertEquals(0, r1.jobsCreated.size());
            Assertions.assertEquals(0, r2.jobsRewritten.size());
            Assertions.assertNotEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void regenerateAllJDKTestProject() throws ManagementException, StorageException {
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkTestProjectManager, null);//create all
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkTestProjectManager, null);//re create all
            Assertions.assertEquals(0, r1.jobsRewritten.size());
            Assertions.assertEquals(5, r1.jobsCreated.size());
            Assertions.assertEquals(5, r2.jobsRewritten.size());
            Assertions.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void regenerateAllJDKTestProjectWithWhitelist() throws ManagementException, StorageException {
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkTestProjectManager, "tck-.*");//create tck
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, settings.getConfigManager().jdkTestProjectManager, null);//re create rest
            Assertions.assertEquals(0, r1.jobsRewritten.size());
            Assertions.assertEquals(4, r1.jobsCreated.size());
            Assertions.assertEquals(4, r2.jobsRewritten.size());
            Assertions.assertEquals(1,  r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void testBumpFunctionWithoutCollision() throws Exception {
        JenkinsCliWrapper.killCli();
        mkdir(fromFile);
        Utils.writeToFile(fromConfig, "fromOriginal");
        final JobBump jobBump = new JobBump(from, to, false);
        final JobUpdateResult result = updater.getBumpFunction(JobCollisionAction.STOP).apply(jobBump);
        Assertions.assertTrue(result.success);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(toFile.exists());
        Assertions.assertEquals("bumped from " + from.getName() + " to " + to.getName(), result.message);
        Assertions.assertEquals(to.getName() + '\n', Utils.readFile(toConfig));
    }

    @Test
    public void testBumpFunctionWithCollisionAndStop() throws Exception {
        JenkinsCliWrapper.killCli();
        mkdir(fromFile);
        mkdir(toFile);
        Utils.writeToFile(fromConfig, "fromOriginal");
        Utils.writeToFile(toConfig, "toOriginal");
        final JobBump jobBump = new JobBump(from, to, true);
        final JobUpdateResult result = updater.getBumpFunction(JobCollisionAction.STOP).apply(jobBump);
        Assertions.assertFalse(result.success);
        Assertions.assertEquals("Collision: no changes done", result.message);
        Assertions.assertTrue(fromFile.exists());
        Assertions.assertTrue(toFile.exists());
        Assertions.assertEquals("fromOriginal\n", Utils.readFile(fromConfig));
        Assertions.assertEquals("toOriginal\n", Utils.readFile(toConfig));
    }

    @Test
    public void testBumpFunctionWithCollisionAndKeepExisting() throws Exception {
        JenkinsCliWrapper.killCli();
        mkdir(fromFile);
        mkdir(toFile);
        Utils.writeToFile(fromConfig, "fromOriginal");
        Utils.writeToFile(toConfig, "toOriginal");
        final File archivedFromFile = new File(archive, from.getName());
        final JobBump jobBump = new JobBump(from, to, true);
        final JobUpdateResult result = updater.getBumpFunction(JobCollisionAction.KEEP_EXISTING).apply(jobBump);
        Assertions.assertTrue(result.success);
        Assertions.assertEquals("Collision: the existing config was kept", result.message);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(archivedFromFile.exists());
        Assertions.assertTrue(toFile.exists());
        Assertions.assertEquals("toOriginal\n", Utils.readFile(toConfig));
    }

    @Test
    public void testBumpFunctionWithCollisionAndKeepBumped() throws Exception {
        JenkinsCliWrapper.killCli();
        mkdir(fromFile);
        mkdir(toFile);
        Utils.writeToFile(fromConfig, "fromOriginal");
        Utils.writeToFile(toConfig, "toOriginal");
        final JobBump jobBump = new JobBump(from, to, true);
        final JobUpdateResult result = updater.getBumpFunction(JobCollisionAction.KEEP_BUMPED).apply(jobBump);
        final File archivedToFile = new File(archive, to.getName());
        final File archivedConfig = new File(archivedToFile, JENKINS_JOB_CONFIG_FILE);
        Assertions.assertTrue(result.success);
        Assertions.assertEquals("bumped from " + from.getName() + " to " + to.getName(), result.message);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(toFile.exists());
        Assertions.assertTrue(archivedToFile.exists());
        Assertions.assertEquals("toOriginal\n", Utils.readFile(archivedConfig));
        Assertions.assertEquals("to\n", Utils.readFile(toConfig));
    }

    @Test
    public void archiveWithoutDuplicates() throws IOException {
        mkdir(fromFile);
        updater.archive(fromFile);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(new File(archive, fromFile.getName()).exists());
    }

    @Test
    public void archiveWithDuplicate() throws IOException {
        final File archived = new File(archive, fromFile.getName());
        mkdir(fromFile);
        mkdir(archived);
        updater.archive(fromFile);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(new File(archive, fromFile.getName() + "(1)").exists());
    }

    @Test
    public void archiveWithNDuplicates() throws IOException {
        final int n = 4;
        final File archived = new File(archive, fromFile.getName());
        mkdir(fromFile);
        mkdir(archived);
        for (int i = 1; i < n; i++) {
            final File file = new File(archive, fromFile.getName() + '(' + i + ')');
            mkdir(file);
        }
        updater.archive(fromFile);
        Assertions.assertFalse(fromFile.exists());
        Assertions.assertTrue(new File(archive, fromFile.getName() + '(' + n + ')').exists());
    }

    void mkdir(final File dir) {
        if (!dir.mkdirs()) {
            throw new RuntimeException("mkdirs() failed on " + dir.getAbsolutePath());
        }
    }
    
    static class DummyJob extends Job {
        final String name;
        protected DummyJob(final String name) {
            super(null, null, null, null);
            this.name = name;
        }

        @Override
        public String generateTemplate() {
            return getName();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getShortName() {
            return getName();
        }
    }
}
