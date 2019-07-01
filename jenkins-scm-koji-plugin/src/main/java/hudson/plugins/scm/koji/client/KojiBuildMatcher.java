package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Stream;

class KojiBuildMatcher extends BuildMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(KojiBuildMatcher.class);

    private final GlobPredicate tagPredicate;
    private final String pkgName;
    private final List<String> archs;

    KojiBuildMatcher(
            Iterable<KojiBuildProvider> kojiBuildProviders,
            Predicate<String> notProcessedNvrPredicate,
            int maxBuilds,
            RealKojiXmlRpcApi kojiXmlRpcApi
    ) {
        super(kojiBuildProviders, notProcessedNvrPredicate, maxBuilds);
        this.tagPredicate = new GlobPredicate(kojiXmlRpcApi.getTag());
        this.pkgName = kojiXmlRpcApi.getPackageName();
        this.archs = composeArchList(kojiXmlRpcApi.getArch());
    }

    List<Build> getBuilds(String url) {
        final List<Build> builds = new ArrayList<>();
        for (final Build build : listPackageBuilds(url, pkgName)) {
            final Set<String> tags = retrieveTags(url, build);
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
                                url,
                                null
                        )
                );
            }
        }
        return builds;
    }

    Build getBuild(Stream<Build> buildStream) {
        final Optional<Build> buildOptional = buildStream.findFirst();
        if (buildOptional.isPresent()) {
            final Build build = buildOptional.get();
            LOG.info("Oldest not processed build: " + build.getNvr());
            final List<RPM> rpms = new ArrayList<>();
            rpms.addAll(retrieveRPMs(build));
            rpms.addAll(retrieveArchives(build));
            return
                new Build(
                        build.getId(),
                        build.getName(),
                        build.getVersion(),
                        build.getRelease(),
                        build.getNvr(),
                        build.getCompletionTime(),
                        rpms,
                        build.getTags(),
                        build.getProviderUrl(),
                        null
            );
        }
        return null;
    }

    private Integer getPackageId(String url, String packageName) {
        final XmlRpcRequestParams params = new GetPackageId(packageName);
        final PackageId response = PackageId.create(execute(url, params));
        return response.getValue();
    }

    private List<Build> listPackageBuilds(String url, String packageName) {
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
        final RPMList response = RPMList.create(execute(build.getProviderUrl(), params));
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
        final List<String> supportedArches = new ArrayList<>(1);
        supportedArches.add("win");
        final XmlRpcRequestParams params = new ListArchives(build.getId(), null);
        final ArchiveList response = ArchiveList.create(execute(build.getProviderUrl(), params));
        final List<String> archivefilenames = response.getValue();
        if (archivefilenames == null || archivefilenames.isEmpty()) {
            return Collections.emptyList();
        }
        final List<RPM> archives = new ArrayList<>(archivefilenames.size());
        for (String archiveName : archivefilenames) {
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
        return archives;
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
}
