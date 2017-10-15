package hudson.plugins.scm.koji;

import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class KojiChangeLogSet extends ChangeLogSet<ChangeLogSet.Entry> {

    private final Build build;
    private final List<Entry> entries;

    public KojiChangeLogSet(Build build, Run<?, ?> run, RepositoryBrowser<?> browser) {
        super(run, browser);
        this.build = build;

        if (build != null) {
            List<Entry> list = Arrays.asList(
                    new KojiChangeEntry("Build Name", new HyperlinkStringContainer(build.getName())),
                    new KojiChangeEntry("Build Version", new HyperlinkStringContainer(build.getVersion())),
                    new KojiChangeEntry("Build Release", new HyperlinkStringContainer(build.getRelease())),
                    new KojiChangeEntry("Build NVR", new HyperlinkStringContainer(build.getNvr())),
                    new KojiChangeEntry("Build Tags", new HyperlinkStringContainer(build.getTags().stream().collect(Collectors.joining(", ")))),
                    new KojiChangeEntry("Build RPMs/Tarballs", new HyperlinkStringContainer(build.getRpms())),
                    new KojiChangeEntry("Build Sources", new HyperlinkStringContainer(build.getSrcUrl()))
            );
            entries = Collections.unmodifiableList(list);
        } else {
            entries = Collections.emptyList();
        }
    }

    @Override
    public boolean isEmptySet() {
        return false;
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public static class KojiChangeEntry extends ChangeLogSet.Entry {

        private final String field;
        private final HyperlinkStringContainer container;

        public KojiChangeEntry(String field, HyperlinkStringContainer container) {
            this.field = field;
            this.container = container;
        }

        public String getField() {
            return field;
        }

        public HyperlinkStringContainer getContainer() {
            return container;
        }

        @Override
        public String getMsg() {
            return field + ": " + container.toString();
        }

        @Override
        public User getAuthor() {
            return User.current();
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return Collections.emptyList();
        }

    }

    public static class HyperlinkStringContainer {

        private List<HyperlinkString> hyperlinks;

        public HyperlinkStringContainer(String string) {
            hyperlinks = new ArrayList();
            hyperlinks.add(new HyperlinkString(string));
        }

        public HyperlinkStringContainer(List<RPM> rpms) {
            this.hyperlinks = new ArrayList();
            storeRpms(rpms);
        }

        public HyperlinkStringContainer(URL url) {
            this.hyperlinks = new ArrayList();
            if (url != null) {
                if (RPM.Suffix.INSTANCE.endsWithSuffix(url.toString())) {
                    hyperlinks.add(new HyperlinkString(new File(url.getPath()).getName(), url.toString()));
                } else {
                    hyperlinks.add(new HyperlinkString("Source file not found. You can search for it here.", url.toString()));
                }
            }
        }

        public List<HyperlinkString> getHyperlinks() {
            return hyperlinks;
        }

        public boolean isNotEmpty() {
            return !hyperlinks.isEmpty();
        }

        private void storeRpms(List<RPM> rpms) {
            for (RPM rpm : rpms) {
                if (rpm.hasUrl()) {
                    hyperlinks.add(new HyperlinkString(rpm.toString(), rpm.getUrl()));
                }
            }
        }

        @Override
        public String toString() {
            String string = "";
            for (HyperlinkString s : hyperlinks) {
                string += s.getString() + ", ";
            }
            return string;
        }
    }

    public static class HyperlinkString {

        private final String string;
        private final String url;

        public HyperlinkString(String string) {
            this.string = string;
            url = null;
        }

        public HyperlinkString(String string, String url) {
            this.string = string;
            this.url = url;
        }

        public String getString() {
            return string;
        }

        public String getUrl() {
            return url;
        }

        public boolean isContainingUrl() {
            return url != null;
        }
    }
}
