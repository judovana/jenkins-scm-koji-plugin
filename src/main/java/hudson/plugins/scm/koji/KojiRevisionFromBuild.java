package hudson.plugins.scm.koji;

import hudson.FilePath;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import org.jenkinsci.remoting.RoleChecker;

public class KojiRevisionFromBuild implements FilePath.FileCallable<Build> {

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Build build = new BuildsSerializer().read(new File(workspace, "changelog.xml"));
        return build;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
