package hudson.plugins.scm.koji;

import hudson.FilePath;
import static hudson.plugins.scm.koji.Constants.PROCESSED_BUILDS_HISTORY;
import hudson.plugins.scm.koji.model.Build;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.jenkinsci.remoting.RoleChecker;

public class KojiProcessedAppender implements FilePath.FileCallable<Build> {

    private final Build build;

    public KojiProcessedAppender(Build build) {
        this.build = build;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Files.write(
                new File(workspace, PROCESSED_BUILDS_HISTORY).toPath(),
                Arrays.asList(build.getNvr()),
                Charset.forName("UTF-8"),
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        return build;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

}
