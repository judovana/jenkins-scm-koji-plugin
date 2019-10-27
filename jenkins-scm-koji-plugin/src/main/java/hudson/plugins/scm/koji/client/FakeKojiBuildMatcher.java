package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.FakeBuildList;

import java.util.List;
import java.util.function.Predicate;

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
                xmlRpcApi.getBuildPlatform(),
                xmlRpcApi.isBuilt()
        );
        final FakeBuildList buildList = FakeBuildList.create(execute(buildProvider.getTopUrl(), getBuildListParams));
        return buildList.getValue();
    }

    @Override
    Build getBuild(Build build) {
        return build;
    }
}
