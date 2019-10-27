package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiXmlRpcApi;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;

public class KojiListBuilds implements FilePath.FileCallable<Optional<Build>> {

    private final Iterable<KojiBuildProvider> kojiBuildProviders;
    private final KojiXmlRpcApi kojiXmlRpcApi;
    private final Predicate<String> notProcessedNvrPredicate;
    private final int maxPreviousBuilds;

    public KojiListBuilds(
            Iterable<KojiBuildProvider> kojiBuildProviders,
            KojiXmlRpcApi kojiXmlRpcApi,
            Predicate<String> notProcessedNvrPredicate,
            int maxPreviousBuilds
    ) {
        this.kojiBuildProviders = kojiBuildProviders;
        this.kojiXmlRpcApi = kojiXmlRpcApi;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxPreviousBuilds = maxPreviousBuilds;
    }

    @Override
    public Optional<Build> invoke(File workspace, VirtualChannel channel) {
        final BuildMatcher buildMatcher;

        if (kojiXmlRpcApi instanceof RealKojiXmlRpcApi) {
            buildMatcher = new KojiBuildMatcher(
                    kojiBuildProviders,
                    notProcessedNvrPredicate,
                    maxPreviousBuilds,
                    (RealKojiXmlRpcApi) kojiXmlRpcApi
            );
        } else if (kojiXmlRpcApi instanceof FakeKojiXmlRpcApi) {

            buildMatcher = new FakeKojiBuildMatcher(
                    kojiBuildProviders,
                    notProcessedNvrPredicate,
                    maxPreviousBuilds,
                    (FakeKojiXmlRpcApi) kojiXmlRpcApi
            );
        } else {
            throw new RuntimeException("Unknown XML-RPC API: " + kojiXmlRpcApi.getDescriptor().getDisplayName());
        }

        return buildMatcher.getBuild();
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
