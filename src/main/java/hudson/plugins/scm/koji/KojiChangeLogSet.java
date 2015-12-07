package hudson.plugins.scm.koji;

import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.scm.koji.model.Build;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class KojiChangeLogSet extends ChangeLogSet<ChangeLogSet.Entry> {

    private final Build build;
    private final List<Entry> entries;

    public KojiChangeLogSet(Build build, Run<?, ?> run, RepositoryBrowser<?> browser) {
        super(run, browser);
        this.build = build;

        if (build != null) {
            List<Entry> list = Arrays.asList(
                    new KojiChangeEntry("Build Name", build.getName()),
                    new KojiChangeEntry("Build Version", build.getVersion()),
                    new KojiChangeEntry("Build Release", build.getRelease()),
                    new KojiChangeEntry("Build NVR", build.getNvr()),
                    new KojiChangeEntry("Build Tags", build.getTags().stream().collect(Collectors.joining(", "))),
                    new KojiChangeEntry(
                            "Build RPMs",
                            build.getRpms().stream().map(r -> r.getNvr() + '.' + r.getArch() + ".rpm").collect(Collectors.joining("<br/>")))
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
        private final String value;

        public KojiChangeEntry(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public String getField() {
            return field;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String getMsg() {
            return field + ": " + value;
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

}
