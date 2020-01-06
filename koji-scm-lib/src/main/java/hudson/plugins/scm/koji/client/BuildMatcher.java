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
package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetPackageId;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListArchives;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListBuilds;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListRPMs;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListTags;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.ArchiveList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.BuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.PackageId;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.RPMList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.TagSet;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.XmlRpcResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

public class BuildMatcher {

public enum OrderBy {
        DATE, VERSION
    }

    private final String currentURL;
    private final Predicate<String> notProcessedNvrPredicate;
    private final GlobPredicate tagPredicate;
    private final int maxBuilds;
    private final String pkgName;
    private final String arch;
    private static final OrderBy orderBy = OrderBy.DATE;

    public BuildMatcher(String currentURL, Predicate<String> notProcessedNvrPredicate, GlobPredicate tagPredicate, int maxBuilds, String pkgName, String arch) {
        this.currentURL = currentURL;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxBuilds = maxBuilds;
        this.pkgName = pkgName;
        this.arch = arch;
        this.tagPredicate = tagPredicate;
    }

    public Optional<Build> getResult() {
       return listMatchingBuilds().stream()
                .filter(build -> notProcessedNvrPredicate.test(build.getNvr()))
                .findFirst();
    }

    public Build[] getAll() {
        return listMatchingBuilds().stream()
                .filter(build -> notProcessedNvrPredicate.test(build.getNvr()))
                .toArray(Build[]::new);
    }

    private List<Build> listMatchingBuilds() {
    	final List<Build> builds = new ArrayList<>();
    	for (String packageName : pkgName.split(" ")) {
    		builds.addAll(listPackageBuilds(packageName));
		}
		builds.sort(this::compare);
		final List<Build> wantedBuilds = new ArrayList<>();
    	for (Build build : builds) {
    		final Set<String> tags = retrieveTags(build.getId());
    		if (matchesTagPredicate(tags)) {
				final List<RPM> rpms = new ArrayList<>();
				rpms.addAll(retrieveRPMs(build.getId()));
				rpms.addAll(retrieveArchives(build));
				if (!rpms.isEmpty()) {
					wantedBuilds.add(new Build(
							build.getId(),
							build.getName(),
							build.getVersion(),
							build.getRelease(),
							build.getNvr(),
							build.getCompletionTime(),
							rpms,
							tags,
							null
					));
				}
				if (wantedBuilds.size() >= maxBuilds) {
					break;
				}
			}
		}
    	wantedBuilds.sort(Comparator.reverseOrder());
		return wantedBuilds;
    }

    private Integer getPackageId(String packageName) {
        final XmlRpcRequestParams params = new GetPackageId(packageName);
        final PackageId response = PackageId.create(execute(params));
        return response.getValue();
    }

    private List<Build> listPackageBuilds(String packageName) {
        Integer packageId = getPackageId(packageName);
        if (packageId == null) {
            return Collections.emptyList();
        }
        final XmlRpcRequestParams params = new ListBuilds(packageId);
        final BuildList response = BuildList.create(execute(params));
        List<Build> builds = response.getValue();
        if (builds == null || builds.isEmpty()) {
            return Collections.emptyList();
        }
        return builds;
    }

    private Set<String> retrieveTags(Integer buildId) {
        final XmlRpcRequestParams params = new ListTags(buildId);
        final TagSet response = TagSet.create(execute(params));
        return response.getValue();
    }

    private boolean matchesTagPredicate(Set<String> tags) {
        return tags
				.stream()
                .anyMatch(tagPredicate);
    }

    private List<RPM> retrieveRPMs(Integer buildId) {
        final XmlRpcRequestParams params = new ListRPMs(buildId, composeArchesList());
        final RPMList response = RPMList.create(execute(params));
        final List<RPM> rpms = response.getValue();
        return rpms == null ? Collections.emptyList() : rpms;
    }

    /**
     * Archives are stored under {@link Constants#rpms} key, together with RPMs.
     * <p>
     * Name, Version and Release are not received with info about archive. We
     * need to get it from the build. Arch is taken from configuration and is
     * later used to compose filepath. Unlike with RPMs, filename is received
     * here so we can store it.
     */
    private List<RPM> retrieveArchives(Build build) {
        final List<String> desiredArches = composeArchesList();
        final XmlRpcRequestParams params = new ListArchives(build.getId(), null);
        final ArchiveList response = ArchiveList.create(execute(params));
        final List<String> archivefilenames = response.getValue();
        if (archivefilenames == null || archivefilenames.isEmpty()) {
            return Collections.emptyList();
        }
        final List<RPM> archives = new ArrayList<>(archivefilenames.size());
        if (isBuildContainer(build)) {
            //for contaiers, we use different approach(because they havee arch in name only), we list all the archives, and include only those of arch (all if no arch is specified)
            //luckily for us, they are mess.arch.tar.gz
            if (desiredArches == null) {
                for (String archiveName : archivefilenames) {
                    archives.add(new RPM(
                            build.getName(),
                            build.getVersion(),
                            build.getRelease(),
                            archiveName,
                            /*Is this necessary at all?*/deductArchFromImage(archiveName),
                            archiveName
                    ));
                }
            } else {
                for (String archiveName : archivefilenames) {
                    for (String arch : desiredArches) {
                        if (archiveName.contains("." + arch + ".")) {
                            archives.add(new RPM(
                                    build.getName(),
                                    build.getVersion(),
                                    build.getRelease(),
                                    archiveName,
                                    arch,
                                    archiveName
                            ));
                        }
                    }
                }
            }
        } else {
            final List<String> supportedArches = new ArrayList<>(1);
            supportedArches.add("win");
            for (String archiveName : archivefilenames) {
                        //warning! this do not support empty arches  field!
                        //and windows archives are arcehd in the xml response
                        //to list all, similar hack like in containers wouldbeneeded
			for (String arch : desiredArches) {
				if (supportedArches.contains(arch)) {
					archives.add(new RPM(
							build.getName(),
							build.getVersion(),
							build.getRelease(),
							archiveName,
							arch,
							archiveName
					));
				}
			}
		}
        }
        return archives;
    }

    private int compare(Build b1, Build b2) {
        switch (orderBy) {
            case DATE:
                return compareBuildsByCompletionTime(b1, b2);
            case VERSION:
                return compareBuildVersions(b1, b2);
        }
        throw new RuntimeException("Unknown order");
    }

    private List<String> composeArchesList() {
        return composeList(arch);
    }

    protected Object execute(XmlRpcRequestParams params) {
        return new XmlRpcHelper.XmlRpcExecutioner(currentURL).execute(params);
    }

    private static List<String> composeList(String values) {
        if (values == null || values.trim().isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(values, ",;\n\r\t ");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    public static int compareBuildsByCompletionTime(Build b1, Build b2) {
        return compareKojiTime(b1.getCompletionTime(), b2.getCompletionTime(), Constants.DTF);

    }

    public static int compareKojiTime(String s1, String s2, DateTimeFormatter d) {
        LocalDateTime thisCompletionTime = LocalDateTime.parse(s1, d);
        LocalDateTime thatCompletionTime = LocalDateTime.parse(s2, d);
        return thatCompletionTime.compareTo(thisCompletionTime);
    }

    public static int compareBuildVersions(Build b1, Build b2) {
        // comparing versions:
        int res = compareStrings(b1.getVersion(), b2.getVersion());
        if (res != 0) {
            return res;
        }
        // version are identical, comparing releases:
        return compareStrings(b1.getRelease(), b2.getRelease());
    }

    public static int compareStrings(String s1, String s2) {
        StringTokenizer tokenizer1 = new StringTokenizer(s1, "-.");
        StringTokenizer tokenizer2 = new StringTokenizer(s2, "-.");
        while (tokenizer1.hasMoreTokens() && tokenizer2.hasMoreTokens()) {
            String t1 = tokenizer1.nextToken();
            String t2 = tokenizer2.nextToken();
            if (allDigits(t1) && allDigits(t2)) {
                int i1 = Integer.parseInt(t1);
                int i2 = Integer.parseInt(t2);
                int intCompared = i1 - i2;
                if (intCompared != 0) {
                    return intCompared > 0 ? -1 : 1;
                }
                continue;
            }
            int stringCompared = t1.compareTo(t2);
            if (stringCompared != 0) {
                return stringCompared > 0 ? -1 : 1;
            }
        }
        // if we are here then one of strings has ended,
        // longer will be considered bigger version:
        if (tokenizer1.hasMoreTokens()) {
            return -1;
        }
        if (tokenizer2.hasMoreTokens()) {
            return 1;
        }
        return 0;
    }

    public static boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    
    public static boolean isRpmContainer(RPM rpm) {
        return rpm.getName().contains("container") && rpm.getFilename("").contains("docker-image");
    }

    public static boolean isBuildContainer(Build b) {
        //we do not know the rpms/archives in time of checking this
        return b.getName().contains("container");
    }

  private String deductArchFromImage(String archiveName) {
        return archiveName.replaceAll("\\.tar.*", "").replaceAll(".*\\.", "");
    }
}
