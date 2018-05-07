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
package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fakekoji.http.AccessibleSettings;
import org.fakekoji.http.ProjectMappingExceptions;
import org.fakekoji.xmlrpc.server.core.FakeBuild;
import org.fakekoji.xmlrpc.server.utils.DirFilter;

/**
 * Hart beat of fake koji. This class works over directory, with similar
 * structure as koji have, and is trying to deduct informations just on content
 * and names. On those deductions it offers its content and even tags.
 */
public class FakeKojiDB {

    private final String[] projects;
    private final List<FakeBuild> builds;
    private final AccessibleSettings settings;

    FakeKojiDB(AccessibleSettings settings) {
        ServerLogger.log(new Date().toString() + " (re)initizing fake koji DB");
        this.settings = settings;
        File[] projectDirs = settings.getDbFileRoot().listFiles(new DirFilter());
        projects = new String[projectDirs.length];
        builds = new ArrayList<>();
        //read all projects
        for (int i = 0; i < projectDirs.length; i++) {
            File projectDir = projectDirs[i];
            projects[i] = projectDir.getName();
            //and all builds in those project
            File[] versions = projectDir.listFiles(new DirFilter());
            for (File version : versions) {
                File[] releases = version.listFiles(new DirFilter());
                for (File release : releases) {
                    FakeBuild b = new FakeBuild(projectDir.getName(), version.getName(), release.getName(), release, settings.getProjectMapping());
                    builds.add(b);
                }
            }

        }

    }

    Integer getPkgId(String requestedProject) {
        String trayed = "";
        for (String project : projects) {
            trayed += " " + project;
            if (project.equals(requestedProject)) {
                //is there better str->int function?
                //indeed, the file. But number of projects is small.
                return project.hashCode();
            }
        }
        ServerLogger.log("Unknown project " + requestedProject + ". Tried: " + trayed + ".");
        return null;
    }

    /*
     Return is scary Object[] where mebers are hashmaps
         "size = 21"	HashMap	ObjectVariable 	
    [ 0]	"start_ts => 1.47013406794314E9"	 	
    [ 2]	"owner_name => jvanek"	 	
    [ 3]	"completion_time => 2016-08-02 21:23:37.487583"	 	
    [ 4]	"volume_id => 0"	 	
    [ 5]	"owner_id => 1487"	 	
    [ 6]	"release => 3.b14.fc26"	 	
    [ 7]	"volume_name => DEFAULT"	 	
    [ 8]	"task_id => 15101251"	 	
    [ 9]	"epoch => 1"	 	
    [10]	"nvr => java-1.8.0-openjdk-1.8.0.101-3.b14.fc26"	 	
    [11]	"package_id => 15685"	 	
    [12]	"creation_event_id => 17490426"	 	
    [13]	"version => 1.8.0.101"	 	
    [14]	"start_time => 2016-08-02 10:34:27.943136"	 	
    [15]	"build_id => 787467"	 	
    [16]	"package_name => java-1.8.0-openjdk"	 	
    [17]	"name => java-1.8.0-openjdk"	 	
    [18]	"creation_ts => 1.47013406794314E9"	 	
    [19]	"completion_ts => 1.47017301748758E9"	 	
    [20]	"state => 1"	 	
    [ 1]	"creation_time => 2016-08-02 10:34:27.943136"	 	
     */
    Object[] getProjectBuildsByProjectIdAsMaps(Integer projectId) {
        List<FakeBuild> matchingBuilds = getProjectBuildsByProjectId(projectId);
        for (int i = 0; i < matchingBuilds.size(); i++) {
            FakeBuild get = matchingBuilds.get(i);
            boolean mayBeFailed = new IsFailedBuild(get.getDir()).reCheck().getLastResult();
            if (mayBeFailed) {
                ServerLogger.log("Removing build (" + i + "): " + get.toString() + " from result. Contains FAILED records");
                matchingBuilds.remove(i);
                i--;
            }
        }
        Object[] res = new Object[matchingBuilds.size()];
        for (int i = 0; i < matchingBuilds.size(); i++) {
            FakeBuild get = matchingBuilds.get(i);
            res[i] = get.toBuildMap();

        }
        return res;
    }

    List<FakeBuild> getProjectBuildsByProjectId(Integer projectId) {
        List<FakeBuild> res = new ArrayList(builds.size());
        for (int i = 0; i < builds.size(); i++) {
            FakeBuild get = builds.get(i);
            if (get.getProjectID() == projectId) {
                res.add(get);
            }
        }
        return res;
    }

    FakeBuild getBuildById(Integer buildId) {
        for (int i = 0; i < builds.size(); i++) {
            FakeBuild get = builds.get(i);
            if (get.getBuildID() == buildId) {
                return get;
            }
        }
        return null;
    }

    /**
     * This method is trying to deduct tags from content of build. Purpose is,
     * that if the build is for some os only, it should be tagged accodringly.
     * On contrary, if it is static, then it should pass anywhere
     *
     * @param parameter
     * @return
     */
    Object[] getTags(Map parameter) {
        Integer buildId = (Integer) parameter.get(Constants.build);
        String[] tagNames = getTags(buildId);
        Map[] result = new Map[tagNames.length];
        for (int i = 0; i < tagNames.length; i++) {
            String tagName = tagNames[i];
            Map map = new HashMap(1);
            map.put(Constants.name, tagName);
            result[i] = map;

        }
        return result;
    }

    private String[] getTags(Integer buildId) {
        for (int i = 0; i < builds.size(); i++) {
            FakeBuild get = builds.get(i);
            if (get.getBuildID() == buildId) {
                try {
                    return get.guessTags();
                } catch (ProjectMappingExceptions.ProjectMappingException e) {
                    return new String[0];
                }
            }
        }
        return new String[0];
    }

    void checkAll() {
        for (String project : projects) {
            check(project);
        }
    }

    void check(String string) {
        Integer id = getPkgId(string);
        if (id == null) {
            return;
        }
        ServerLogger.log(string + " id=" + id);
        Object[] chBuilds = getProjectBuildsByProjectIdAsMaps(id);
        ServerLogger.log(string + " builds#=" + chBuilds.length);
        int bn = 0;
        for (Object chBuild : chBuilds) {
            bn++;
            ServerLogger.log("####### " + bn + " #######");
            Map m = (Map) chBuild;
            Set keys = m.keySet();
            for (Object key : keys) {
                Object value = m.get(key);
                ServerLogger.log("  " + key + ": " + value);
                if (key.equals(Constants.build_id)) {
                    ServerLogger.log("    tags:");
                    Object[] tags = getTags((Integer) value);
                    for (Object tag : tags) {
                        ServerLogger.log("      " + tag);
                    }
                    ServerLogger.log("Artifacts for given build");
                    FakeBuild bld = getBuildById((Integer) value);
                    bld.printExpectedArchesForThisBuild();
                    List<String> arches = bld.getArches();
                    ServerLogger.log("  archs: " + arches.size());
                    for (String arch : arches) {
                        ServerLogger.log("  logs: " + arch);
                        //list logs
                        List<File> logs = bld.getLogs(arch);
                        logs.stream().forEach((log) -> {
                            ServerLogger.log(log.toString());
                        });
                        //list others
                        ServerLogger.log("  all: " + arch);
                        List<File> files = bld.getNonLogs(arch);
                        files.stream().forEach((f) -> {
                            ServerLogger.log(f.toString());
                        });
                    }
                }
                if (key.equals(Constants.rpms)) {
                    ServerLogger.log("  mapped: ");
                    Object[] rpms = (Object[]) value;
                    for (Object rpm : rpms) {
                        Map mrpms = (Map) rpm;
                        Set rks = mrpms.keySet();
                        rks.stream().forEach((rk) -> {
                            ServerLogger.log("    " + rk + ": " + mrpms.get(rk));
                        });

                    }
                }

            }
        }
        ServerLogger.log("Artifacts for given project " + string + " " + id);
        List<FakeBuild> blds = getProjectBuildsByProjectId(id);
        for (FakeBuild bld : blds) {
            List<String> arches = bld.getArches();
            for (String arch : arches) {
                ServerLogger.log("  arch: " + arch);
                //list logs
                List<File> logs = bld.getLogs(arch);
                logs.stream().forEach((log) -> {
                    ServerLogger.log(log.toString());
                });
                //list others
                List<File> files = bld.getNonLogs(arch);
                files.stream().forEach((f) -> {
                    ServerLogger.log(f.toString());
                });
            }
        }
    }

    Object[] getRpms(Object get, Object get0) {
        return getRpmsI((Integer) get, (Object[]) get0);
    }

    /**
     *
     * @param buildDd
     * @param archs String[]
     * @return array of hashmaps
     */
    Object[] getRpmsI(Integer buildDd, Object[] archs) {
        FakeBuild build = getBuildById(buildDd);
        if (build == null) {
            return new Object[0];
        }
        return build.getRpmsAsArrayOfMaps(archs);
    }

    Object[] getArchives(Object get, Object get0) {
        return getArchivesI((Integer) get, (Object[]) get0);
    }

    /**
     * same as rpms but on windows (solaris?) archives
     */
    Object[] getArchivesI(Integer buildDd, Object[] archs) {
        return new Object[0];
    }

}
