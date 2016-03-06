package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;

public class KojiListBuildsTest {

    KojiScmConfig createConfig() {
        return new KojiScmConfig(
                "http://koji.fedoraproject.org/kojihub",
                "https://kojipkgs.fedoraproject.org/packages/",
                "java-1.8.0-openjdk",
                "x86_64",
                "f22-updates*",
                null,
                null,
                false,
                false
        );
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testListMatchingBuilds() throws Exception {
        KojiListBuilds worker = new KojiListBuilds(createConfig(), s -> true);
        Build build = worker.invoke(temporaryFolder.newFolder(), null);
        assertNotNull(build);
    }

}
