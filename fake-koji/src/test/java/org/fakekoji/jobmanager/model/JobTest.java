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
package org.fakekoji.jobmanager.model;

import org.fakekoji.DataGenerator;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;


public class JobTest {

    //should be not used
    private final File scriptsRoot = null;

    @Test
    public void testSumTrims() throws Exception {
        String res = Job.truncatedSha("aaaa", 1);
        Assert.assertEquals("4", res);
    }

    @Test
    public void testSumTrimsFromEnd() throws Exception {
        String res = Job.truncatedSha("aaaa", 10);
        Assert.assertEquals("f27b9af0b4", res);
    }

    @Test
    public void testSumTrimDoNotProlong() throws Exception {
        String res = Job.truncatedSha("aaaa", 1000000);
        Assert.assertEquals(64, res.length());
    }

    @Test
    public void testFirstLetterAcceptNothing() throws Exception {
        Assert.assertEquals("", Job.firstLetter(null));
        Assert.assertEquals("", Job.firstLetter(""));
        Assert.assertEquals("", Job.firstLetter("     "));
        ;
    }

    @Test
    public void testFirstLetterWorks() throws Exception {
        Assert.assertEquals("a", Job.firstLetter("a"));
        Assert.assertEquals("a", Job.firstLetter("ab"));
        Assert.assertEquals("c", Job.firstLetter("    cde "));
        ;
    }

    @Test
    public void testSanitize() throws Exception {
        Assert.assertEquals("a-b", Job.sanitizeNames("a-b"));
        Assert.assertEquals("a-b", Job.sanitizeNames("a--b"));
        Assert.assertEquals("    -c-d-e- ", Job.sanitizeNames("    -c-d---e-- "));
        ;
    }

    @Test
    public void testShortenedNameCornerCases() throws Exception {
        //this is testing corenrcases, so we relay on 59 chars
        Assert.assertEquals(59,  Job.MAX_JOBNAME_LENGTH);
        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTaskRequiringSourcesAndBinaries();
        final Platform buildPlatform = new Platform("pb1.x", "pb", "1", "x", null, Arrays.asList(new Platform.Provider("vg", null, null)), "vm", null, null);
        final Platform testPlatform = new Platform("pr.1", "pr", "1", "x", null, Arrays.asList(new Platform.Provider("vg", null, null)), "vm", null, null);


        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final TestJob buildJob1 = new TestJob(
                "",
                "name", //final name have 59 chars. its maximum possible, before shortening
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss1 = buildJob1.getShortName();
        String sl1 = buildJob1.getName();
        System.out.println(sl1);
        System.out.println(sl1.length());
        System.out.println(ss1);
        System.out.println(ss1.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss1.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, sl1.length());
        Assert.assertEquals(ss1, sl1);

        final TestJob buildJob2 = new TestJob(
                "",
                "nameA", //final name have 60 chars. its maximum is just now exceeded
                //the sha here is very long 41 chars
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        String ss2 = buildJob2.getShortName();
        String sl2 = buildJob2.getName();
        System.out.println(sl2);
        System.out.println(sl2.length());
        System.out.println(ss2);
        System.out.println(ss2.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss2.length());
        Assert.assertEquals("tck-nameA-rh-pr.1-sw-e392978d65fbe52a9ebe8ef8656ab47747c5ba", ss2);


        final TestJob buildJob3 = new TestJob(
                "",
                "nameA0123456789012345678901234567890123456", //projname is now so long
                //that its hasum is exactly 1char
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss3 = buildJob3.getShortName();
        String sl3 = buildJob3.getName();
        System.out.println(sl3);
        System.out.println(sl3.length());
        System.out.println(ss3);
        System.out.println(ss3.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss3.length());
        Assert.assertEquals("tck-nameA0123456789012345678901234567890123456-rh-pr.1-sw-3", ss3);

        final TestJob buildJob4 = new TestJob(
                "",
                "nameA01234567890123456789012345678901234567", //projname is now so long
                //that its only hashsum
                Project.ProjectType.JDK_PROJECT,
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss4 = buildJob4.getShortName();
        String sl4 = buildJob4.getName();
        System.out.println(sl4);
        System.out.println(sl4.length());
        System.out.println(ss4);
        System.out.println(ss4.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss4.length());
        Assert.assertEquals("67da6da50e4bac3f653c947eb95bf311b41840a09dd695441a0679703a9", ss4);
    }


    @Test
    public void buildShortenedNameCornerCases() throws Exception {
        //this is testing corenrcases, so we relay on 59 chars
        Assert.assertEquals(59,  Job.MAX_JOBNAME_LENGTH);
        final JDKVersion jdk8 = DataGenerator.getJDKVersion8();
        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getBuildTask();
        final Platform buildPlatform = new Platform("pb.1", "pb", "1", "x", null, Arrays.asList(new Platform.Provider("vg", null, null)), "vm", null, null);


        final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();

        final BuildJob buildJob1 = new BuildJob(
                "",
                "nameNameNameNameNameNameNam", //final name have 59 chars. its maximum possible, before shortening
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss1 = buildJob1.getShortName();
        String sl1 = buildJob1.getName();
        System.out.println(sl1);
        System.out.println(sl1.length());
        System.out.println(ss1);
        System.out.println(ss1.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss1.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, sl1.length());
        Assert.assertEquals(ss1, sl1);

        final BuildJob buildJob2 = new BuildJob(
                "",
                "nameNameNameNameNameNameNamA", //final name have 60 chars. its maximum is just now exceeded
                //the sha here is very long 41 chars
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );

        String ss2 = buildJob2.getShortName();
        String sl2 = buildJob2.getName();
        System.out.println(sl2);
        System.out.println(sl2.length());
        System.out.println(ss2);
        System.out.println(ss2.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss2.length());
        Assert.assertEquals("build-nameNameNameNameNameNameNamA-pb.1-rh-3471aced9ef5802d", ss2);


        final BuildJob buildJob3 = new BuildJob(
                "",
                "nameNameNameNameNameNameNamA012345678901234", //projname is now so long
                //that its hasum is exactly 1char
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss3 = buildJob3.getShortName();
        String sl3 = buildJob3.getName();
        System.out.println(sl3);
        System.out.println(sl3.length());
        System.out.println(ss3);
        System.out.println(ss3.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss3.length());
        Assert.assertEquals("build-nameNameNameNameNameNameNamA012345678901234-pb.1-rh-5", ss3);

        final BuildJob buildJob4 = new BuildJob(
                "",
                "nameNameNameNameNameNameNamA0123456789012345", //projname is now so long
                //that its only hashsum
                DataGenerator.getJDK8Product(),
                jdk8,
                buildProviders,
                testTask,
                buildPlatform,
                buildVariants,
                scriptsRoot
        );
        String ss4 = buildJob4.getShortName();
        String sl4 = buildJob4.getName();
        System.out.println(sl4);
        System.out.println(sl4.length());
        System.out.println(ss4);
        System.out.println(ss4.length());
        Assert.assertEquals(Job.MAX_JOBNAME_LENGTH, ss4.length());
        Assert.assertEquals("1e2571669f01fa5675c925d027bcca632315256dd55f687fab9483de465", ss4);
    }

}
