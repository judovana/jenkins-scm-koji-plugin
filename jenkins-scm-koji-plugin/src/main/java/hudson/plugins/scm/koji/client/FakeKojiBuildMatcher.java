package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildDetail;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.FakeBuildDetail;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.FakeBuildList;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

class FakeKojiBuildMatcher extends BuildMatcher {

    private final FakeKojiXmlRpcApi xmlRpcApi;

    public FakeKojiBuildMatcher(
            Iterable<KojiBuildProvider> buildProviders,
            Predicate<String> notProcessedNvrPredicate,
            int maxBuilds,
            FakeKojiXmlRpcApi xmlRpcApi
    ) {
        super(buildProviders, notProcessedNvrPredicate, maxBuilds);
        this.xmlRpcApi = xmlRpcApi;
    }

    @Override
    List<Build> getBuilds(BuildProvider buildProvider) {
        final GetBuildList getBuildListParams = new GetBuildList(
                xmlRpcApi.getProjectName(),
                xmlRpcApi.getBuildVariants(),
                xmlRpcApi.isBuilt()
        );
        final FakeBuildList buildList = FakeBuildList.create(execute(buildProvider.getTopUrl(), getBuildListParams));
        return buildList.getValue();
    }

    @Override
    Build getBuild(Stream<Build> buildStream) {
        final Optional<Build> buildOptional = buildStream.findFirst();
        if (buildOptional.isPresent()) {
            final Build build = buildOptional.get();
            final GetBuildDetail getBuildDetailParams = new GetBuildDetail(build.getName(), build.getVersion(), build.getRelease());
            final FakeBuildDetail fakeBuildDetailResponse = FakeBuildDetail.create(execute(
                    build.getProvider().getTopUrl(),
                    getBuildDetailParams
            ));
            return fakeBuildDetailResponse.getValue();
        }
        return null;
    }
}
