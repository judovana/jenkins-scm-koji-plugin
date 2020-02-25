/*
 * The MIT License
 *
 * Copyright 2015 user.
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
package org.fakekoji.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.fakekoji.core.utils.DirFilter;
import org.fakekoji.core.utils.FileFileFilter;

public class FakeBuild {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final String name;
    private final String version;
    private final String release;
    private final String nvr;
    private final File dir;
    private final ProjectMapping projectMapping;

    private static final String logs = "logs";
    private static final String data = "data";
    public static final String notBuiltTagPart = "_notBuild-";
    public static final String archesConfigFileName = "arches-expected";

    public FakeBuild(String name, String version, String release, File releaseDir, ProjectMapping projectMapping) {
        this.dir = releaseDir;
        this.name = name;
        this.version = version;
        this.release = release;
        this.projectMapping = projectMapping;
        this.nvr = name + "-" + version + "-" + release;
    }

    public Build toBuild() {
        return toBuild(getTags());
    }

    public Build toBuild(Set<String> tags) {
        return new Build(
                getBuildID(),
                name,
                version,
                release,
                getNVR(),
                Constants.DTF.format(getFinishingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
                getRpms(),
                tags,
                null,
                null
        );
    }

    String getNVR() {
        return nvr;
    }

    @Override
    public String toString() {
        return getNVR() + " " + getBuildID();
    }

    private List<File> getLogs() {
        List<File> logs = new ArrayList();
        List<String> arches = getArches();
        for (String arch : arches) {
            logs.addAll(getLogs(arch));

        }
        return logs;
    }

    public List<File> getLogs(String arch) {
        File logDir = getLogsDir();
        List<File> logs = new ArrayList();
        if (logDir == null) {
            return logs;
        }
        if (!getArches().contains(arch)) {
            return logs;
        }
        File logFinalDir = new File(logDir, arch);
        if (!logFinalDir.exists()) {
            return logs;
        }
        File[] logfiles = logFinalDir.listFiles(new FileFileFilter());
        for (File logfile : logfiles) {
            logs.add(logfile.getAbsoluteFile());
        }
        return logs;
    }

    List<File> getNonLogs() {
        List<File> files = new ArrayList();
        List<String> arches = getArches();
        for (String arche : arches) {
            files.addAll(getNonLogs(arche));
        }
        return files;

    }

    public List<File> getNonLogs(String arch) {
        List<File> files = new ArrayList();
        if (!getArches().contains(arch)) {
            return files;
        }
        File finalDir = new File(dir, arch);
        File[] possibleFfiles = finalDir.listFiles(new FileFileFilter());
        for (File file : possibleFfiles) {
            files.add(file.getAbsoluteFile());
        }
        return files;
    }

    File getLogsDir() {
        File data = getDataDir();
        if (data == null) {
            return null;
        }
        return new File(data, logs);
    }

    private File getDataDir() {
        File[] possibleArchesDirs = dir.listFiles(new DirFilter());
        //List<String> arches = new ArrayList<>(possibleArchesDirs.length);
        for (File archDir : possibleArchesDirs) {
            if (archDir.getName().equalsIgnoreCase(data)) {
                return archDir;
            } else {

            }
        }
        return null;
    }

    private File getBuildExpectedArches() {
        return new File(getDataDir(), archesConfigFileName);
    }

    public List<String> getArches() {
        File[] possibleArchesDirs = dir.listFiles(new DirFilter());
        List<String> arches = new ArrayList<>(possibleArchesDirs.length);
        for (File archDir : possibleArchesDirs) {
            if (archDir.getName().equalsIgnoreCase(data)) {

            } else {
                arches.add(archDir.getName());
            }
        }
        return arches;
    }

    /**
     * This method is crucial for fake koji and its cooperation with
     * scm-koji-plugin.
     * <p>
     * This method is trying to guess tags based on content of the build is it
     * win? Windows is it specific rhel/fedora - that one is it static? all!
     * <p>
     * x
     * <p>
     * is it openjdk? is it oracle? is it ibm??
     * <p>
     * x
     * <p>
     * is the build finished?
     *
     * @return
     * @throws java.io.IOException
     */
    public String[] guessTags() throws ProjectMappingExceptions.ProjectMappingException {
        List<File> files = this.getNonLogs();
        for (File file : files) {
            //appearence and using of openjdkX-win is very unlike, but...
            //we have java-X.Y-openjdk or openjdkX-win)
            if (name.endsWith("openjdk") || name.startsWith("openjdk")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.toLowerCase().contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getFedoraTags(), TagsProvider.getRHELtags(), TagsProvider.getRhelTags(), TagsProvider.getWinTags()));
                }
                if (release.contains("el")) {
                    int osVersion = determineRhOs(release);
                    return prefixIfNecessary(new String[]{
                            TagsProvider.getRhel5Rhel6Base(osVersion),
                            TagsProvider.getRhel7Base(osVersion)});
                }
                if (release.contains("fc")) {
                    int osVersion = determineRhOs(release);
                    return prefixIfNecessary(new String[]{
                            TagsProvider.getFedoraBase(osVersion)
                    });
                }
                if (file.getName().toLowerCase().contains("win") && file.getParentFile().getName().toLowerCase().contains("win")) {
                    return prefixIfNecessary(TagsProvider.getWinTags());
                }
            } else
            //oracle and ibm are far from being done. But at least start is sumarised there
            if (name.endsWith("oracle")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getOracleTags()));
                }
            } else 
            if (name.endsWith("ibm")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getSuplementaryRhel5LikeTag(), TagsProvider.getSuplementaryRhel6LikeTag(), TagsProvider.getSuplementaryRhel7LikeTag()));
                }
            } else {
                if (file.getName().toLowerCase().contains("static") || release.contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getFedoraTags(), TagsProvider.getRHELtags(), TagsProvider.getRhelTags(), TagsProvider.getWinTags()));
                }
            }
        }
        return new String[0];
    }

    public Set<String> getTags() {
        try {
            return Stream.of(guessTags()).collect(Collectors.toSet());
        } catch (ProjectMappingExceptions.ProjectMappingException e) {
            LOGGER.severe(e.getMessage());
            return Collections.emptySet();
        }
    }

    private String[] connect(String[]... tags) {
        List<String> all = new ArrayList<>();
        for (String[] tag : tags) {
            all.addAll(Arrays.asList(tag));
        }
        String[] arr = new String[all.size()];
        arr = all.toArray(arr);
        return arr;
    }

    public int getProjectID() {
        return name.hashCode();
    }

    public int getBuildID() {
        //dir's hash is better then NVR's, as it will be really always unique
        return dir.hashCode();
    }

    public List<RPM> getRpms(List<String> archs) {
        if (archs == null || archs.isEmpty()) {
            return getRpms();
        }
        final List<File> files = getNonLogs();
        final List<RPM> rpms = new ArrayList<>(files.size());
        for (File file : files) {
            final String fileName = file.getName();
            String packageName = replaceLast(fileName, "-.*", "");
            packageName = replaceLast(packageName, "-.*", "");
            String packageFile = replaceLast(fileName, "\\..*", ""); //.suffix
            packageFile = replaceLast(packageFile, "\\..*", ""); //.arch
            final String arch = file.getParentFile().getName();
            final boolean isFailed = new IsFailedBuild(file.getParentFile().getParentFile()).reCheck().getLastResult();
            if (isFailed) {
                LOGGER.warning(file + " seems to be from failed build!");
            }
            if (archs.contains(arch)) {
                rpms.add(new RPM(
                        packageName,
                        version,
                        release,
                        packageFile,
                        arch,
                        fileName
                ));
            }
        }
        return rpms;
    }

    public List<RPM> getRpms() {
        return getRpms(getArches());
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    public Date getFinishingDate() {
        File f = getNewestFile();
        if (f == null) {
            //keep running?
            return new Date();
        } else {
            return new Date(f.lastModified());
        }

    }

    private File getNewestFile() {
        List<File> files = getNonLogs();
        if (files == null || files.isEmpty()) {
            files = getLogs();
        }
        if (files == null || files.isEmpty()) {
            return null;
        }
        Collections.sort(files, (File o1, File o2) -> {
            if (o1.lastModified() == o2.lastModified()) {
                return 0;
            }
            if (o1.lastModified() > o2.lastModified()) {
                return -1;
            }
            return 1;
        });
        return files.get(0);
    }

    /**
     * Each src archive must be be attempted on all those archs.
     * <p>
     * If the build fails, or arch is not accessible, the column must have
     * FAILED file instead of build. (+ ideally logs).
     * <p>
     * Little bit more generally - if the arch dir don't exists or is empty, is
     * considered as not build
     */

    private String[] getSupportedArches() throws ProjectMappingExceptions.ProjectMappingException {
        LOGGER.info("For: " + getNVR());
        String[] arches;
        File archesFile = getBuildExpectedArches();
        if (archesFile.exists()) {
            try {
                arches = readArchesFile(archesFile);
                LOGGER.info("Using build specific arches");
                return arches;
            } catch (IOException e) {
                throw new ProjectMappingExceptions.ProjectMappingException(e);
            }
        }
        arches = this.projectMapping.getExpectedArchesOfNVR(getNVR()).toArray(new String[0]);
        LOGGER.info("Using project default expected arches");
        return arches;
    }

    public void printExpectedArchesForThisBuild() {
        try {
            LOGGER.info("Expected to build on: " + Arrays.toString(getSupportedArches()));
        } catch (ProjectMappingExceptions.ProjectMappingException e) {
            LOGGER.warning("No expected arches to build on");
        }
    }

    public boolean isBuilt() throws ProjectMappingExceptions.ProjectMappingException {
        boolean allBuilt = true;
        String[] thisOnesArches = getSupportedArches();
        LOGGER.info("isBuilt on: " + Arrays.toString(thisOnesArches) + "?");
        for (String arch : thisOnesArches) {
            File archDir = new File(dir, arch);
            if (archDir.exists() && archDir.isDirectory() && archDir.list().length > 0) {
                //hmm no op?
            } else {
                allBuilt = false;
                //return
            }
        }
        return allBuilt;
    }


    private String[] prefixIfNecessary(String[] connectedTags) throws ProjectMappingExceptions.ProjectMappingException {
        List<String> arches = getArches();
        Collections.sort(getArches());
        //primary case - the build have src, so we expect to build it in time onall arches
        if (arches.contains(JenkinsJobTemplateBuilder.SOURCES)) {
            boolean allBuilt = true; //warning! duplicated code with isBuilt!
            //there may be IO involved
            String[] thisOnesArches = getSupportedArches();
            LOGGER.info("Expected to build on: " + Arrays.toString(thisOnesArches));
            List<String> tags = new ArrayList<>(connectedTags.length * thisOnesArches.length);
            for (String connectedTag : connectedTags) {
                for (String arch : thisOnesArches) {
                    File archDir = new File(dir, arch);
                    if (archDir.exists() && archDir.isDirectory() && archDir.list().length > 0) {
                        //hmm no op?
                    } else {
                        allBuilt = false;
                        tags.add(arch + notBuiltTagPart + connectedTag);
                    }
                }
            }
            if (allBuilt) {
                List<File> files = getNonLogs();
                for (File file : files) {
                    if (file.getAbsolutePath().contains("fastdebug")) {
                        String[] nwTags = new String[connectedTags.length];
                        for (int i = 0; i < connectedTags.length; i++) {
                            nwTags[i] = "fastdebug-" + connectedTags[i];

                        }
                        return nwTags;
                    }
                }
                for (File file : files) {
                    if (file.getAbsolutePath().contains("slowdebug")) {
                        String[] nwTags = new String[connectedTags.length];
                        for (int i = 0; i < connectedTags.length; i++) {
                            nwTags[i] = "slowdebug-" + connectedTags[i];

                        }
                        return nwTags;
                    }
                }
                return connectedTags;
            } else {
                return tags.toArray(new String[0]);
            }
        }
        //ok, the package was probably built on all/expected by uploader
        return connectedTags;
    }

    private int determineRhOs(String release) {
        String stripped = release.replaceAll(".*\\.fc", "").replaceAll(".*\\.el", "");
        String result = "";
        for (int i = 0; i < stripped.length(); i++) {
            char ch = stripped.charAt(i);
            if (Character.isDigit(ch)) {
                result = result + ch;
            } else {
                return Integer.valueOf(result);
            }
        }
        //probaby error, so throwing no-int exception
        return Integer.valueOf(result);
    }

    private boolean arrayContains(Object[] archs, String arch) {
        if (archs == null || archs.length == 0) {
            return true;
        }
        for (Object arch1 : archs) {
            if (arch1.equals(arch)) {
                return true;
            }
        }
        return false;
    }

    public File getDir() {
        return dir;
    }

    public static void main(String... arg) throws IOException {
        //arg = new String[]{"/mnt/raid1/upstream-repos/java-9-openjdk/" + archesConfigFileName};
        if (arg.length == 0) {
            LOGGER.severe("Expected single argument - path to file to save the file. Suggested name is: " + archesConfigFileName);
            System.exit(1);
        }
        generateDefaultArchesFile(new File(arg[0]));
        LOGGER.info(new File(arg[0]).getAbsolutePath());
        System.err.println(Arrays.toString(readArchesFile(new File(arg[0]))));
    }

    private static void generateDefaultArchesFile(File defaultArchesFile) throws IOException {
        try (
                OutputStream outputStream = new FileOutputStream(defaultArchesFile);
                OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
                BufferedWriter bufferedWriter = new BufferedWriter(streamWriter)) {
            bufferedWriter.write("# ############## #\n");
            bufferedWriter.write("# This config file contains architectures the project should be built on.\n");
            bufferedWriter.write("# It must be placed in project's directory as " + archesConfigFileName + "\n");
            bufferedWriter.write("# or, if build specific config file is needed, it can be placed in build's data folder.");
            bufferedWriter.write("# Lines are trimmed before processing, and empty and # starting lines are skipped.");
            bufferedWriter.write("# First non # non empty line is considered as arches line and splitted on \\\\s+, returned, reading stopped");
            bufferedWriter.write("# ############## #\n");
            bufferedWriter.write("# Please always keep comment here describing above.\n");
            bufferedWriter.write("# Generated from: " + System.getProperty("user.dir") + "\n");
            bufferedWriter.flush();

        }
    }

    public static String[] readArchesFile(File f) throws IOException {
        try (
                InputStream fis = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);) {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    return null;
                }
                if (line.trim().startsWith("#")) {
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] r = line.trim().split("\\s+");
                return r;

            }
        }
    }

    //FIXME - read from DB!
    public static final String HOTSPOT = "hotspot";
    public static final String ZERO = "zero";
    public static final String OPENJ9 = "openj9";

    public static boolean isValidVm(String s) {
        return (s.equals(ZERO) || s.equals(HOTSPOT) || s.equals(OPENJ9));
    }


    public String getJvm() {
        if (nvr.contains(HOTSPOT)) {
            return HOTSPOT;
        } else if (nvr.contains(ZERO)) {
            return ZERO;
        } else if (nvr.contains(OPENJ9)) {
            return OPENJ9;
        } else {
            return HOTSPOT;
        }
    }


    //FIXME - read from DB!
    public static final String RELEASE = "release";
    public static final String FASTDEBUG = "fastdebug";
    public static final String SLOWDEBUG = "slowdebug";

    public static boolean isValidBuildVariant(String s) {
        return (s.equals(RELEASE) || s.equals(FASTDEBUG) || s.equals(SLOWDEBUG));
    }

    public String getDebugMode() {
        if (nvr.contains(RELEASE)) {
            return RELEASE;
        } else if (nvr.contains(FASTDEBUG)) {
            return FASTDEBUG;
        } else if (nvr.contains(SLOWDEBUG)) {
            return SLOWDEBUG;
        } else {
            return RELEASE;
        }
    }

    public String getRepoOfOriginProject() throws ProjectMappingExceptions.ProjectMappingException {
        return projectMapping.getProjectOfNvra(getNVR()/*+".arch.sufix"?*/);
    }

    public boolean haveSrcs() {
        File srcDir = new File(dir, JenkinsJobTemplateBuilder.SOURCES);
        if (srcDir.exists() && srcDir.isDirectory()) {
            File[] content = srcDir.listFiles();
            return content.length > 0 && content[0].getName().length() >= 5 && content[0].length() > 5;
        }
        return false;
    }
}
