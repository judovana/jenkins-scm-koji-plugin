package org.fakekoji.jobmanager.views;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/*
     <!-- ~ojdk11~shenandoah~upstream~cpu-otherStuf -->
    <!-- mind the dashes, mind the pull!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*|pull-.*-ojdk11~shenandoah~upstream~cpu -->
    <!-- mind the dashes, mind the build!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*  instead of .*-ojdk11~shenandoah~upstream~cpu-.*-.*
 */
public class JenkinsViewTemplateBuilder implements  CharSequence{

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        JenkinsViewTemplateBuilder that = (JenkinsViewTemplateBuilder) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static class JenkinsViewTemplateBuilderFolder extends JenkinsViewTemplateBuilder {
        public static class ColumnsStyle{
            public enum ColumnsType {
                ALL, NONE, AUTO
            }

            private final ColumnsType type;
            private final int limit;
            private Optional<List<String>> jobs;

            public ColumnsStyle(ColumnsType type, int limit) {
                this.type = type;
                this.limit = limit;
            }

            public void setJobs(Optional<List<String>> jobs) {
                this.jobs = jobs;
            }
        }

        static final String SUBVIEWS = "%{SUBVIEWS}";
        static final String NESTED_DEFAULT_COLUMNS = " %{NESTEDVIEW_COLUMNS}";
        private final List<JenkinsViewTemplateBuilder> views = new ArrayList<>();
        private final ColumnsStyle columnsStyle;

        public JenkinsViewTemplateBuilderFolder(String name, String template, ColumnsStyle nestedVieColumnsStyle, ColumnsStyle listViewColumnStyle) throws IOException {
            super(name, null, null, template, listViewColumnStyle);
            this.columnsStyle = nestedVieColumnsStyle;
        }

        public void addView(JenkinsViewTemplateBuilder jvtb) {
            views.add(jvtb);
        }

        public void addAll(Collection<? extends JenkinsViewTemplateBuilder> jvtbs) {
            views.addAll(jvtbs);
        }

        @Override
        public String expand() throws IOException {
            return super.expand().replace(NESTED_DEFAULT_COLUMNS, getNestedColumnsStyle()).replace(SUBVIEWS, expandInners());
        }

        public List<String> getMatches(Optional<List<String>> jobs) {
            List<String> r = new ArrayList<>(jobs.get().size() / 10); //intentionally not set, jenkins is walking all branches anyway
            for(JenkinsViewTemplateBuilder jvb: views){
                r.addAll(jvb.getMatches(jobs));
            }
            return r;
        }

        private CharSequence getNestedColumnsStyle() {
            try {
                if (columnsStyle.type == ColumnsStyle.ColumnsType.ALL) {
                    return loadDefaultColumns();
                } else if (columnsStyle.type == ColumnsStyle.ColumnsType.NONE) {
                    return "";
                } else if (columnsStyle.type == ColumnsStyle.ColumnsType.AUTO) {
                    List<String> matches = getMatches(columnsStyle.jobs);
                    if (matches.size() > columnsStyle.limit) {
                        String cm="<!-- NW skipped as " + matches.size() + ">" + columnsStyle.limit + " in " + getName() + " -->\n";
                        System.err.print(cm);
                        return cm;
                    } else {
                        String cm = "<!-- NW included as " + matches.size() + "<=" + columnsStyle.limit + " in " + getName() + "-->\n";
                        System.err.print(cm);
                        return cm + loadDefaultColumns();
                    }
                } else {
                    throw new RuntimeException("No more nested view column styles supported right now");
                }
            }catch(IOException ex){
                throw new RuntimeException(ex);
            }
        }

        private String loadDefaultColumns() throws IOException {
            return JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.NESTEDVIEW_DEFAULT_COLUMNS);
        }

        private CharSequence expandInners() throws IOException {
            StringBuilder sb = new StringBuilder();
            for (JenkinsViewTemplateBuilder jvtb : views) {
                sb.append(jvtb.expand());
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toExtendedPrint(Optional.empty(), false, false, 0);
        }

        @Override
        public String toExtendedPrint(Optional<List<String>> jobs, boolean details, boolean matches) {
            return toExtendedPrint(jobs, details, matches, 0);
        }

        @Override
        protected String toExtendedPrint(Optional<List<String>> jobs, boolean details, boolean matches, int depth) {
            return spaces(depth) + getName() + " (" + views.size() + ")\n" + innersToString(jobs, details, matches, depth);
        }

        private String innersToString(Optional<List<String>> jobs, boolean details, boolean matches, int depth) {
            depth += 2;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < views.size(); i++) {
                JenkinsViewTemplateBuilder jvtb = views.get(i);
                sb.append(spaces(depth)).append(jvtb.toExtendedPrint(jobs, details, matches, depth));
            }
            if (sb.length() > 1) {
                sb = sb.delete(sb.length() - 1, sb.length() - 1);
            }
            return sb.toString();
        }

        @Override
        public boolean clearOutMatches(Optional<List<String>> jobs) {
            if (jobs.isPresent()) {
                for (int i = 0; i < views.size(); i++) {
                    JenkinsViewTemplateBuilder view = views.get(i);
                    if (view.clearOutMatches(jobs)) {
                        views.remove(i);
                        i--;
                    }
                }
                return views.isEmpty();
            } else {
                return false;
            }
        }

    }

    static final String VIEW_NAME = "%{VIEW_NAME}";
    static final String COLUMNS = "%{COLUMNS}";
    static final String VIEW_REGEX = "%{VIEW_REGEX}";

    private final String name;
    private final String columns;
    private final Pattern regex;
    private final String template;
    private final JenkinsViewTemplateBuilderFolder.ColumnsStyle columnStyle;

    public String getName() {
        return name;
    }

    public Pattern getRegex() {
        return regex;
    }

    public JenkinsViewTemplateBuilder(String name, String columns, String regex, String template, JenkinsViewTemplateBuilderFolder.ColumnsStyle listViewColumnsStyle) {
        this.name = name;
        this.columns = columns;
        if (regex == null) {
            this.regex = null;
        } else {
            this.regex = Pattern.compile(regex);
        }
        this.template = template;
        this.columnStyle = listViewColumnsStyle;
    }

    public String expand() throws IOException {
        return JenkinsJobTemplateBuilder.prettyPrint(template
                .replace(VIEW_NAME, name)
                .replace(COLUMNS, getColumnsByCount())
                .replace(VIEW_REGEX, regex == null ? "not_used_regex" : regex.toString()));
    }

    private CharSequence getColumnsByCount() throws IOException {
        if (columnStyle.type == JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.NONE) {
            return JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS);
        } else if (columnStyle.type == JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.ALL) {
            return columns == null ? "not_used_columns" : columns;
        } else if (columnStyle.type == JenkinsViewTemplateBuilderFolder.ColumnsStyle.ColumnsType.AUTO) {
            List<String> matches = getMatches(columnStyle.jobs);
            if (matches.size() <= columnStyle.limit) {
                String cm = "<!-- LV included as " + matches.size() + "<=" + columnStyle.limit + " in " + getName() + " -->\n";
                System.err.print(cm);
                return columns == null ? "not_used_columns" : cm + columns;
            } else {
                String cm = "<!-- LW skipped as " + matches.size() + ">" + columnStyle.limit + " in " + getName() + "-->\n";
                System.err.print(cm);
                return cm + JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS);
            }
        } else {
            throw new RuntimeException("No more list view column styles supported right now");
        }
    }

    public InputStream expandToStream() throws IOException {
        return new ByteArrayInputStream(expand().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public String toString() {
        return name;
    }

    public String toExtendedPrint(Optional<List<String>> jobs, boolean details, boolean matches) {
        if (jobs.isPresent()) {
            StringBuilder sb;
            List<String> matchingJobs = getMatches(jobs);
            if (details) {
                sb = new StringBuilder(name + " (" + matchingJobs.size() + ") " + getRegex() + "\n");
            } else {
                sb = new StringBuilder(name + "\n");
            }
            if (matches) {
                for (String job : matchingJobs) {
                    sb.append(job).append("\n");
                }
            }
            return sb.toString();
        } else {
            return name + "\n";
        }
    }

    protected String toExtendedPrint(Optional<List<String>> jobs, boolean details, boolean matches, int depth) {
        return spaces(depth) + toExtendedPrint(jobs, details, matches);
    }

    /**
     * @param jobs
     * @return true, if there is no match, and so should be dropped
     */
    public boolean clearOutMatches(Optional<List<String>> jobs) {
        if (jobs.isPresent()) {
            return getMatches(jobs).isEmpty();
        } else {
            return false;
        }
    }

    public List<String> getMatches(Optional<List<String>> jobs) {
        if (jobs.isPresent()) {
            List<String> r = new ArrayList<>(jobs.get().size() / 10); //intentionally not set, jenkins is walking all branhces anyway
            for (String job : jobs.get()) {
                if (getRegex().matcher(job).matches()) {
                    r.add(job);
                }
            }
            return r;
        } else {
            //and now what ::D
            return new ArrayList<>(0);
        }
    }


    private static String spaces(int depth) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < depth) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
