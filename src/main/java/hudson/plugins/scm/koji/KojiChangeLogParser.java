package hudson.plugins.scm.koji;

import hudson.model.Run;
import hudson.plugins.scm.koji.model.Build;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.xml.sax.SAXException;

public class KojiChangeLogParser extends ChangeLogParser {

    @Override
    public ChangeLogSet<? extends ChangeLogSet.Entry> parse(Run run, RepositoryBrowser<?> browser, File changelogFile) throws IOException, SAXException {
        Optional<Build> buildOpt = new BuildsSerializer().read(changelogFile);
        Build build = buildOpt.get();
        return new KojiChangeLogSet(build, run, browser);
    }

}
