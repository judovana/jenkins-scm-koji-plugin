package hudson.plugins.scm.koji;

import hudson.model.Run;
import hudson.plugins.scm.koji.model.Build;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;

public class KojiChangeLogParser extends ChangeLogParser {

    @Override
    public ChangeLogSet<? extends ChangeLogSet.Entry> parse(Run run, RepositoryBrowser<?> browser, File changelogFile) throws IOException, SAXException {
        Build build = new BuildsSerializer().read(changelogFile);
        return new KojiChangeLogSet(build, run, browser);
    }

}
