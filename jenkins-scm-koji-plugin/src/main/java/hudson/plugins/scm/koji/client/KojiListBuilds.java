package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

public class KojiListBuilds implements FilePath.FileCallable<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(KojiListBuilds.class);

    private final Iterable<KojiBuildProvider> kojiBuildProviders;
    private final KojiScmConfig config;
    private final GlobPredicate tagPredicate;
    private final Predicate<String> notProcessedNvrPredicate;

    public KojiListBuilds(
            Iterable<KojiBuildProvider> kojiBuildProviders,
            KojiScmConfig config,
            Predicate<String> notProcessedNvrPredicate
    ) {
        this.kojiBuildProviders = kojiBuildProviders;
        this.config = config;
        this.tagPredicate = new GlobPredicate(config.getTag());
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Optional<Build> buildOpt = Optional.empty();
        for (KojiBuildProvider kojiBuildProvider : kojiBuildProviders) {
            BuildMatcher bm = new BuildMatcher(kojiBuildProvider.getTopUrl(), notProcessedNvrPredicate, tagPredicate, config.getMaxPreviousBuilds(), config.getPackageName(), config.getArch());
            buildOpt = bm.getResult();
            if (buildOpt.isPresent()) {
                break;
            }
        }
        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            LOG.info("oldest not processed build: " + build.getNvr());
            return build;
        }
        return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
