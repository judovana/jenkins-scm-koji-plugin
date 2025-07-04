package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiXmlRpcApi;
import hudson.plugins.scm.koji.LoggerHelp;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

public class KojiListBuilds implements FilePath.FileCallable<Build> {

    private final List<KojiBuildProvider> kojiBuildProviders;
    private final KojiXmlRpcApi kojiXmlRpcApi;
    private final Predicate<String> notProcessedNvrPredicate;
    private final int maxPreviousBuilds;
    private final LoggerHelp logger;

    KojiListBuilds(
            List<KojiBuildProvider> kojiBuildProviders,
            KojiXmlRpcApi kojiXmlRpcApi,
            Predicate<String> notProcessedNvrPredicate,
            int maxPreviousBuilds
           ) {
        this(kojiBuildProviders, kojiXmlRpcApi, notProcessedNvrPredicate, maxPreviousBuilds, null);
    }

    public KojiListBuilds(
            List<KojiBuildProvider> kojiBuildProviders,
            KojiXmlRpcApi kojiXmlRpcApi,
            Predicate<String> notProcessedNvrPredicate,
            int maxPreviousBuilds,
            LoggerHelp logger
    ) {
        this.kojiBuildProviders = kojiBuildProviders;
        this.kojiXmlRpcApi = kojiXmlRpcApi;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.maxPreviousBuilds = maxPreviousBuilds;
        this.logger = logger;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) {
        final BuildMatcher buildMatcher;

        if (kojiXmlRpcApi instanceof RealKojiXmlRpcApi) {
            buildMatcher = new KojiBuildMatcher(
                    kojiBuildProviders,
                    notProcessedNvrPredicate,
                    maxPreviousBuilds,
                    (RealKojiXmlRpcApi) kojiXmlRpcApi,
                    logger
            );
        } else if (kojiXmlRpcApi instanceof FakeKojiXmlRpcApi) {

            buildMatcher = new FakeKojiBuildMatcher(
                    kojiBuildProviders,
                    notProcessedNvrPredicate,
                    maxPreviousBuilds,
                    (FakeKojiXmlRpcApi) kojiXmlRpcApi,
                    logger
            );
        } else {
            throw new RuntimeException("Unknown XML-RPC API: " + kojiXmlRpcApi.getDescriptor().getDisplayName());
        }

        // so I understand that jenkins serializes return value of this function on slave machines and since Optional
        // is not Serializable, it would always throw an error, so we need to unwrap it here before returning it
        return buildMatcher.getBuild().orElse(null);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
