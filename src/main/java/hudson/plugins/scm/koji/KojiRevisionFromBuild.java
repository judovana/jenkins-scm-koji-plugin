package hudson.plugins.scm.koji;

import hudson.FilePath;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.jenkinsci.remoting.RoleChecker;

public class KojiRevisionFromBuild implements FilePath.FileCallable<Optional<Build>> {

    @Override
    public Optional<Build> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Optional<Build> buildOpt = new BuildsSerializer().read(new File(workspace, "changelog.xml"));
        return buildOpt;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
