package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.BuildsSerializer;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import org.jenkinsci.remoting.RoleChecker;

import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import static hudson.plugins.scm.koji.KojiSCM.DESCRIPTOR;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiListBuilds implements FilePath.FileCallable<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(KojiListBuilds.class);

    private final KojiScmConfig config;
    private final GlobPredicate tagPredicate;
    private final Predicate<String> notProcessedNvrPredicate;

    public KojiListBuilds(KojiScmConfig config, Predicate<String> notProcessedNvrPredicate) {
        this.config = config;
        this.tagPredicate = new GlobPredicate(config.getTag());
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Optional<Build> buildOpt = Optional.empty();
        for (String url : config.getKojiTopUrls()) {
            BuildMatcher bm = new BuildMatcher(url, notProcessedNvrPredicate, tagPredicate, config.getMaxPreviousBuilds(), config.getPackageName(), config.getArch());
            buildOpt = bm.getResult();
            if (buildOpt.isPresent()) {
                break;
            }
        }
        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            LOG.info("oldest not processed build: " + build.getNvr());
            if (!DESCRIPTOR.getKojiSCMConfig()) {
                // do NOT save save BUILD_XML in no-worksapce mode. By creating it, you will  cause the ater pooling to fail
                // and most suprisingly  - NVR get comelty lost
                // I dont know what exactly is causing the lsot of NVRE, but following NPEs missing builds, even not  called koiscm.checkout ...
                // ..fatality. See the rest of "I have no idea what I have done" commit
                // and good new at the end. The  file is writtne later, to workspace anyway....
            } else {
                new BuildsSerializer().write(build, new File(workspace, BUILD_XML));
            }
            return build;
        }
        return null;
    }

  
    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
