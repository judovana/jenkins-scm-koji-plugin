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
                    new KojiChangeEntry("Build Name", Collections.singletonList(new Hyperlink(build.getName()))),
                    new KojiChangeEntry("Build Version", Collections.singletonList(new Hyperlink(build.getVersion()))),
                    new KojiChangeEntry("Build Release", Collections.singletonList(new Hyperlink(build.getRelease()))),
                    new KojiChangeEntry("Build NVR", Collections.singletonList(new Hyperlink(build.getNvr()))),
                    new KojiChangeEntry("Build Tags", Collections.singletonList(new Hyperlink(String.join(", ", build.getTags())))),
                    new KojiChangeEntry("Build RPMs/Tarballs", getListFromRPMs(build.getRpms())),
                    new KojiChangeEntry("Build Sources", getListFromUrl(build.getSrcUrl()))
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
        private final List<Hyperlink> hyperlinks;

        public KojiChangeEntry(String field, List<Hyperlink> hyperlinks) {
            this.field = field;
            this.hyperlinks = hyperlinks;
        }

        public String getField() {
            return field;
        }

        public List<Hyperlink> getHyperlinks() {
            return hyperlinks;
        }

        @Override
        public String getMsg() {
            return field + ": " + hyperlinks.toString();
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

    private static List<Hyperlink> getListFromRPMs(List<RPM> rpms) {
        return rpms.stream()
                   .filter(RPM::hasUrl)
                   .map(rpm -> new Hyperlink(rpm.toString(), rpm.getUrl()))
                   .collect(Collectors.toList());
    }

    private static List<Hyperlink> getListFromUrl(URL url) {
        List<Hyperlink> hyperlinks = new ArrayList<>();
        if (url != null) {
            if (RPM.Suffix.INSTANCE.endsWithSuffix(url.toString())) {
                hyperlinks.add(new Hyperlink(new File(url.getPath()).getName(), url.toString()));
            } else {
                hyperlinks.add(new Hyperlink("Source file not found. You can search for it here.", url.toString()));
            }
        }
        return hyperlinks;
    }

    public static class Hyperlink {

        private final String displayedString;
        private final String url;

        public Hyperlink(String displayedString) {
            this.displayedString = displayedString;
            url = null;
        }

        public Hyperlink(String displayedString, String url) {
            this.displayedString = displayedString;
            this.url = url;
        }

        public String getDisplayedString() {
            return displayedString;
        }

        public String getUrl() {
            return url;
        }

        public boolean isContainingUrl() {
            return url != null;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Hyperlink{");
            sb.append("displayedString='").append(displayedString).append('\'');
            sb.append(", url='").append(url).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
