package hudson.plugins.scm.koji;

import hudson.FilePath;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.jenkinsci.remoting.RoleChecker;

import static hudson.plugins.scm.koji.Constants.KOJI_CHECKOUT_NVR;

public class KojiCheckoutNvr implements FilePath.FileCallable<Build> {

    private final Build build;

    public KojiCheckoutNvr(Build build) {
        this.build = build;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Files.write(new File(workspace, KOJI_CHECKOUT_NVR).toPath(),
                Arrays.asList(build.getNvr()),
                Charset.forName("UTF-8"),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        return build;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
