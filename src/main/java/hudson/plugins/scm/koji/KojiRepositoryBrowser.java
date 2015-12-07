package hudson.plugins.scm.koji;

import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import java.io.IOException;
import java.net.URL;

public class KojiRepositoryBrowser extends RepositoryBrowser {

    @Override
    public URL getChangeSetLink(ChangeLogSet.Entry changeSet) throws IOException {
        // TODO implement
        return null;
    }

}
