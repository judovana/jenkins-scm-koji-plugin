package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

class KojiBuildMatcher extends BuildMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(KojiBuildMatcher.class);

    private final GlobPredicate tagPredicate;
    private final String pkgName;
    private final List<String> archs;

    KojiBuildMatcher(
            List<KojiBuildProvider> kojiBuildProviders,
            Predicate<String> notProcessedNvrPredicate,
            int maxBuilds,
            RealKojiXmlRpcApi kojiXmlRpcApi
    ) {
        super(kojiBuildProviders, notProcessedNvrPredicate, maxBuilds);
        this.tagPredicate = new GlobPredicate(kojiXmlRpcApi.getTag(), null);
        this.pkgName = kojiXmlRpcApi.getPackageName();
        this.archs = composeArchList(kojiXmlRpcApi.getArch());
    }

    @Override
    List<Build> getBuilds(BuildProvider buildProvider) {
        final List<Build> builds = new ArrayList<>();
        for (final Build build : listPackageBuilds(buildProvider.getTopUrl(), pkgName)) {
            final Set<String> tags = retrieveTags(buildProvider.getTopUrl(), build);
            if (matchesTagPredicate(tags)) {
                builds.add(
                        new Build(
                                build.getId(),
                                build.getName(),
                                build.getVersion(),
                                build.getRelease(),
                                build.getNvr(),
                                build.getCompletionTime(),
                                null,
                                tags,
                                buildProvider,
                                null
                        )
                );
            }
        }
        return builds;
    }

    Build getBuild(Build build) {
        LOG.info("Oldest not processed build: " + build.getNvr());
        final List<RPM> rpms = new ArrayList<>();
        rpms.addAll(retrieveRPMs(build));
        rpms.addAll(retrieveArchives(build));
        return new Build(
                build.getId(),
                build.getName(),
                build.getVersion(),
                build.getRelease(),
                build.getNvr(),
                build.getCompletionTime(),
                rpms,
                build.getTags(),
                build.getProvider(),
                null
        );
    }

    private Integer getPackageId(String url, String packageName) {
        final XmlRpcRequestParams params = new GetPackageId(packageName);
        final PackageId response = PackageId.create(execute(url, params));
        return response.getValue();
    }

    private List<Build> listPackageBuilds(String url, String packageName) {
        String[] packages = packageName.split("\\s+");
        List<Build> r = new ArrayList<>();
        for (String pkg: packages){
            r.addAll(listPackageBuild(url, pkg));
        }
        return r;
    }

    private List<Build> listPackageBuild(String url, String packageName) {
        Integer packageId = getPackageId(url, packageName);
        if (packageId == null) {
            return Collections.emptyList();
        }
        final XmlRpcRequestParams params = new ListBuilds(packageId);
        final BuildList response = BuildList.create(execute(url, params));
        List<Build> builds = response.getValue();
        if (builds == null || builds.isEmpty()) {
            return Collections.emptyList();
        }
        return builds;
    }

    private Set<String> retrieveTags(String url, Build build) {
        final XmlRpcRequestParams params = new ListTags(build.getId());
        final TagSet response = TagSet.create(execute(url, params));
        return response.getValue();
    }

    private boolean matchesTagPredicate(Set<String> tags) {
        return tags
                .stream()
                .anyMatch(tagPredicate);
    }

    private List<RPM> retrieveRPMs(Build build) {
        final XmlRpcRequestParams params = new ListRPMs(build.getId(), archs);
        final RPMList response = RPMList.create(execute(build.getProvider().getTopUrl(), params));
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
        final XmlRpcRequestParams params = new ListArchives(build.getId(), null);
        final ArchiveList response = ArchiveList.create(execute(build.getProvider().getTopUrl(), params));
        final List<String> archivefilenames = response.getValue();
        if (archivefilenames == null || archivefilenames.isEmpty()) {
            return Collections.emptyList();
        }
        final List<RPM> archives = new ArrayList<>(archivefilenames.size());
        if (isBuildContainer(build)) {
            //for contaiers, we use different approach(because they havee arch in name only), we list all the archives, and include only those of arch (all if no arch is specified)
            //luckily for us, they are mess.arch.tar.gz
            if (archs == null || archs.isEmpty()) {
                addAllContainerArchives(archivefilenames, archives, build);
            } else {
                addSelectedArchesContainers(archivefilenames, archives, build);
            }
        } else {
            addWindowsArchives(archivefilenames, archives, build);
        }
        return archives;
    }

    private void addWindowsArchives(final List<String> archivefilenames, final List<RPM> archives, Build build) {
        final List<String> supportedArches = new ArrayList<>(1);
        supportedArches.add("win");
        for (String archiveName : archivefilenames) {
            //warning! this do not support empty arches  field!
            //and windows archives are arcehd in the xml response
            //to list all, similar hack like in containers wouldbeneeded
            for (String arch : archs) {
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

    private void addSelectedArchesContainers(final List<String> archivefilenames, final List<RPM> archives, Build build) {
        for (String archiveName : archivefilenames) {
            for (String arch : archs) {
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

    private void addAllContainerArchives(final List<String> archivefilenames, final List<RPM> archives, Build build) {
        for (String archiveName : archivefilenames) {
            archives.add(new RPM(
                    build.getName(),
                    build.getVersion(),
                    build.getRelease(),
                    archiveName,
                    /*Is this necessary at all?*/ deductArchFromImage(archiveName),
                    archiveName
            ));
        }
    }

    private static List<String> composeArchList(String arch) {
        if (arch == null || arch.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> list = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(arch, ",;\n\r\t ");
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            final String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
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
