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

import java.io.IOException;
import java.util.Arrays;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Warning - reaming check have missing check on content!
 *
 * @author jvanek
 */
public class JenkinsJobUpdaterTest {

    boolean wasFinallyRun = false;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AccessibleSettings settings;
    private JobUpdater jobUpdater;
    private ConfigManager cm;

    @Before
    public void setup() throws IOException, StorageException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        settings = DataGenerator.getSettings(folderHolder);
        jobUpdater = new JenkinsJobUpdater(settings);
    }

    @Test(expected = IOException.class)
    public void firtsWin() throws Throwable {
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
            Assert.assertNull(r);
        } finally {
            Assert.assertTrue(wasFinallyRun);
        }
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
            Assert.assertEquals("returned", r);
        } finally {
            Assert.assertTrue(wasFinallyRun);
        }
    }

    @Test(expected = IOException.class)
    public void firstExceptionKills() throws Throwable {
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
            Assert.assertNull(r);
        } finally {
            Assert.assertTrue(wasFinallyRun);
        }
    }

    @Test(expected = InterruptedException.class)
    public void secondExceptionAlsoKills() throws Throwable {
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
            Assert.assertNull(r);
        } finally {
            Assert.assertTrue(wasFinallyRun);
        }
    }

    @Test
    public void regenerateAllJDKTestProjectJobs() throws ManagementException, StorageException {
        final JobUpdater jobUpdater = new JenkinsJobUpdater(settings);
        final JDKTestProjectManager manager = new JDKTestProjectManager(cm.getJdkTestProjectStorage());
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, manager); // create all
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, manager); // recreate all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            //Assert.assertNotEquals(0, r1.jobsCreated.size()); todo fix when generator generates test only jobs
            //Assert.assertNotEquals(0, r2.jobsRewritten.size()); todo fix when generator generates test only jobs
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void regenerateAllJDKProjectJobs() throws ManagementException, StorageException {
        final JDKProjectManager manager = new JDKProjectManager(
                cm,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = jobUpdater.regenerateAll(null, manager); // create all
            JobUpdateResults r2 = jobUpdater.regenerateAll(null, manager); // recreate all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            Assert.assertNotEquals(0, r1.jobsCreated.size());
            Assert.assertNotEquals(0, r2.jobsRewritten.size());
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }
}
