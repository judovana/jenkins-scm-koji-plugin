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
package org.fakekoji.xmlrpc.server.core;

import hudson.plugins.scm.koji.Constants;
import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fakekoji.xmlrpc.server.IsFailedBuild;
import org.fakekoji.xmlrpc.server.utils.DirFilter;
import org.fakekoji.xmlrpc.server.utils.FileFileFilter;

public class FakeBuild {

    private final String name;
    private final String version;
    private final String release;
    private final String nvr;
    private final File dir;

    private static final String logs = "logs";
    private static final String data = "data";

    public FakeBuild(String name, String version, String release, File releaseDir) {
        this.dir = releaseDir;
        this.name = name;
        this.version = version;
        this.release = release;
        this.nvr = name + "-" + version + "-" + release;
    }

    public Map toBuildMap() {
        Map<String, Object> buildMap = new HashMap();
        buildMap.put(Constants.name, name);
        buildMap.put(Constants.version, version);
        buildMap.put(Constants.release, release);
        buildMap.put(Constants.nvr, getNVR());
        buildMap.put(Constants.build_id, getBuildID());
        //"2016-08-02 21:23:37.487583");
        buildMap.put(Constants.completion_time, Constants.DTF.format(getFinishingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
        buildMap.put(Constants.rpms, getRpmsAsArrayOfMaps(getArches().toArray(new Object[0])));
        return buildMap;
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

    File getDataDir() {
        File[] possibleArchesDirs = dir.listFiles(new DirFilter());
        List<String> arches = new ArrayList<>(possibleArchesDirs.length);
        for (File archDir : possibleArchesDirs) {
            if (archDir.getName().equalsIgnoreCase(data)) {
                return archDir;
            } else {

            }
        }
        return null;
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
     *
     * This method is trying to guess tags based on content of the build is it
     * win? Windows is it specific rhel/fedora - that one is it static? all!
     *
     * x
     *
     * is it openjdk? is it oracle? is it ibm??
     *
     * x
     *
     * is the build finished?
     *
     * @return
     */
    public String[] guessTags() {
        List<File> files = this.getNonLogs();
        for (File file : files) {
            if (file.getName().toLowerCase().contains("win") && file.getParentFile().getName().toLowerCase().contains("win")) {
                return TagsProvider.getWinTags();
            }
            if (name.endsWith("openjdk")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.toLowerCase().contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getFedoraTags(), TagsProvider.getRHELtags(), TagsProvider.getRhelTags()));
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
            }
            //oracle and ibm are far from being done. But at least start is sumarised there
            if (name.endsWith("oracle")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getOracleTags()));
                }
            }
            if (name.endsWith("ibm")) {
                //currently we have static only for linux
                if (file.getName().toLowerCase().contains("static") || release.contains("upstream")) {
                    return prefixIfNecessary(connect(TagsProvider.getSuplementaryRhel5LikeTag(), TagsProvider.getSuplementaryRhel6LikeTag(), TagsProvider.getSuplementaryRhel7LikeTag()));
                }
            }
        }
        return new String[0];
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

    /*
     [ 0]	"size = 16"	HashMap	ObjectVariable 	
     [ 0]	"release => 1.b14.fc24"	HashMap$Node	ObjectVariable 	
     [ 2]	"nvr => java-1.8.0-openjdk-debuginfo-1.8.0.102-1.b14.fc24"	HashMap$Node	ObjectVariable 	
     [ 3]	"external_repo_id => 0"	HashMap$Node	ObjectVariable 	
     [ 4]	"version => 1.8.0.102"	HashMap$Node	ObjectVariable 	
     [ 5]	"external_repo_name => INTERNAL"	HashMap$Node	ObjectVariable 	
     [ 6]	"size => 82989458"	HashMap$Node	ObjectVariable 	
     [ 7]	"build_id => 794434"	HashMap$Node	ObjectVariable 	
     [ 8]	"buildtime => 1472142006"	HashMap$Node	ObjectVariable 	
     [ 9]	"metadata_only => false"	HashMap$Node	ObjectVariable 	
     [10]	"extra => null"	HashMap$Node	ObjectVariable 	
     [11]	"buildroot_id => 6287882"	HashMap$Node	ObjectVariable 	
     [12]	"name => java-1.8.0-openjdk-debuginfo"	HashMap$Node	ObjectVariable 	
     [13]	"payloadhash => a94abb6777419cfd8a3e9db537554293"	HashMap$Node	ObjectVariable 	
     [14]	"arch => x86_64"	HashMap$Node	ObjectVariable 	
     [15]	"id => 7988968"	HashMap$Node	ObjectVariable 	
     [ 1]	"epoch => 1"	HashMap$Node	ObjectVariable 	
     [ 1]	"size = 16"	HashMap	ObjectVariable 	
    
     */
    public Object[] getRpmsAsArrayOfMaps(Object[] archs) {
        List<File> files = getNonLogs();
        List<Object> r = new ArrayList(files.size());
        for (int i = 0; i < files.size(); i++) {
            File get = files.get(i);
            String fname = get.getName();
            String pkgNAme = replaceLast(fname, "-.*", "");
            pkgNAme = replaceLast(pkgNAme, "-.*", "");
            String pkgFile = replaceLast(fname, "\\..*", ""); //.suffix
            pkgFile = replaceLast(pkgFile, "\\..*", ""); //.arch
            String arch = get.getParentFile().getName();
            boolean mayBeFailed = new  IsFailedBuild(get.getParentFile().getParentFile()).reCheck().getLastResult();
            if (mayBeFailed){
                System.out.println(" Warning: " + get + " seems to be from failed build!");
            }
            if (arrayContains(archs, arch)) {
                Map m = new HashMap();
                r.add(m);
                m.put(Constants.release, release);
                m.put(Constants.version, version);
                m.put(Constants.name, pkgNAme);
                /*IMPORTANT filename is originally only for archives. misusing here*/
                m.put(Constants.filename, fname);
                m.put(Constants.nvr, pkgFile);
                m.put(Constants.arch, arch);
                m.put(Constants.build_id, getBuildID());
            }

        }
        return r.toArray();
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    private Date getFinishingDate() {
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
                return 1;
            }
            return -1;
        });
        return files.get(0);
    }

    /**
     * Each src archive must be be attempted on all those archs.
     *
     * If the build fails, or arch is not accesible, the column must have FAILED
     * file instead of build. (+ ideally logs).
     *
     * Little bitmore generally - if the arch dir dont exists or is empty, is
     * considered as not build
     */
    private final String[] maxSupportedArchs = new String[]{"x86_64", "i686"/*, "aarch64"*/};
    //FIXME enable aarch64 builder

    private String[] prefixIfNecessary(String[] connectedTags) {
        List<String> arches = getArches();
        Collections.sort(getArches());
        //primary case - the build have src, so we expect to build it in time onall arches
        if (arches.contains("src")) {
            boolean allBuilt = true;
            List<String> tags = new ArrayList<>(connectedTags.length * maxSupportedArchs.length);
            for (String connectedTag : connectedTags) {
                for (String arch : maxSupportedArchs) {
                    File archDir = new File(dir, arch);
                    if (archDir.exists() && archDir.isDirectory() && archDir.list().length > 0) {
                        //hmm no op?
                    } else {
                        allBuilt = false;
                        tags.add(arch + "_notBuild-" + connectedTag);
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
    
    
}
