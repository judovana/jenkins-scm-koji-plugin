package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
public class JenkinsViewTemplateBuilder implements CharSequence {

    public static class JenkinsViewTemplateBuilderFolder extends JenkinsViewTemplateBuilder {
        static final String SUBVIEWS = "%{SUBVIEWS}";
        private final List<JenkinsViewTemplateBuilder> views = new ArrayList<>();

        public JenkinsViewTemplateBuilderFolder(String name) throws IOException {
            super(name, "columns-unused-nvdir", "regex-unused-nvdir", new NestedViewTemplateProvider().loadTemplate());
        }

        public List<JenkinsViewTemplateBuilder> getViews() {
            return views;
        }

        @Override
        public String expand() {
            return super.expand().replace(SUBVIEWS, expandInners());
        }

        private CharSequence expandInners() {
            StringBuilder sb = new StringBuilder();
            for (JenkinsViewTemplateBuilder jvtb : views) {
                sb.append(jvtb.expand());
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(int depth) {
            return spaces(depth) + super.toString() + " (" + views.size() + ")" + endConditionally() + innersToString(depth);
        }

        private String endConditionally() {
            if (views.isEmpty()) {
                return "";
            } else {
                return "\n";
            }
        }

        private String innersToString(int depth) {
            depth += 2;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < views.size(); i++) {
                JenkinsViewTemplateBuilder jvtb = views.get(i);
                sb.append(spaces(depth)).append(jvtb.toString(depth));
                if (i < views.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    public static class VersionlessPlatform implements CharSequence, Comparable<VersionlessPlatform> {
        private final String os;
        private final String arch;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionlessPlatform that = (VersionlessPlatform) o;
            return Objects.equals(os, that.os) &&
                    Objects.equals(arch, that.arch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(os, arch);
        }

        public VersionlessPlatform(String os, String arch) {
            this.os = os;
            this.arch = arch;
        }

        public String getOs() {
            return os;
        }

        public String getArch() {
            return arch;
        }

        public String getId() {
            return os + getMinorDelimiter() + arch;
        }

        @Override
        public int length() {
            return getId().length();
        }

        @Override
        public char charAt(int index) {
            return getId().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return getId().subSequence(start, end);
        }

        @Override
        public int compareTo(@NotNull VersionlessPlatform o) {
            return getId().compareTo(o.getId());
        }
    }

    static final String VIEW_NAME = "%{VIEW_NAME}";
    static final String COLUMNS = "%{COLUMNS}";
    static final String VIEW_REGEX = "%{VIEW_REGEX}";

    private final String name;
    private final String columns;
    private final Pattern regex;
    private final String template;

    public String getName() {
        return name;
    }

    public Pattern getRegex() {
        return regex;
    }

    private static String getMajorDelimiter() {
        return Job.DELIMITER;
    }

    private static String getMinorDelimiter() {
        return Job.VARIANTS_DELIMITER;
    }

    private static String getEscapedMajorDelimiter() {
        return escape(getMajorDelimiter());
    }

    private static String getEscapedMinorDelimiter() {
        return escape(getMinorDelimiter());
    }

    private static String escape(String d) {
        //there is much more of them, but this is unlikely to change
        if (d.equals(".")) {
            return "\\.";
        } else {
            return d;
        }
    }

    public JenkinsViewTemplateBuilder(String name, String columns, String regex, String template) {
        this.name = name;
        this.columns = columns;
        this.regex = Pattern.compile(regex);
        this.template = template;
    }

    public static class NestedViewTemplateProvider extends ViewTemplateProvider {
        protected JenkinsJobTemplateBuilder.JenkinsTemplate getTemplate() {
            return JenkinsJobTemplateBuilder.JenkinsTemplate.NESTED_VIEW;
        }

        @Override
        protected String getPlatformmViewName(String viewName) {
            return viewName;
        }

        @Override
        protected String getProjectViewName(String viewName, Optional<String> platform) {
            if (!platform.isPresent()) {
                return viewName;
            } else {
                return viewName + "-" + platform.get();
            }
        }
    }

    public static class ViewTemplateProvider {

        protected JenkinsJobTemplateBuilder.JenkinsTemplate getTemplate() {
            return JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW;
        }

        protected String loadTemplate() throws IOException {
            return JenkinsJobTemplateBuilder.loadTemplate(getTemplate());
        }

        protected JenkinsJobTemplateBuilder.JenkinsTemplate getColumnsTemplate() {
            return JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS;
        }

        protected String loadColumnsTemplate() throws IOException {
            return JenkinsJobTemplateBuilder.loadTemplate(getColumnsTemplate());
        }

        protected String getPlatformmViewName(String viewName) {
            return "." + viewName;
        }

        protected String getProjectViewName(String viewName, Optional<String> platform) {
            if (!platform.isPresent()) {
                return "~" + viewName;
            } else {
                return "~" + viewName + "-" + platform.get();
            }
        }
    }

    public static JenkinsViewTemplateBuilder getPlatformTemplate(VersionlessPlatform platform) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                vtp.getPlatformmViewName(platform.getId()),
                vtp.loadColumnsTemplate(),
                getPlatformViewRegex(false, platform, false),
                vtp.loadTemplate());
    }

    private static String getPlatformViewRegex(boolean isBuild, VersionlessPlatform platform, boolean isForBuild) {
        String prefix = ".*" + getEscapedMajorDelimiter();
        if (isForBuild) {
            prefix = "";
        }
        return prefix + platform.os + "[0-9a-zA-Z]{1,6}" + getEscapedMinorDelimiter() + platform.arch + getPlatformSuffixRegexString(isBuild) + ".*";
    }

    public static JenkinsViewTemplateBuilder getPlatformTemplate(String platform, List<Platform> platforms) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                vtp.getPlatformmViewName(platform),
                vtp.loadColumnsTemplate(),
                getPlatformViewRegex(false, platform, platforms, false),
                vtp.loadTemplate());
    }

    public static JenkinsViewTemplateBuilder getJavaPlatformTemplate(JDKVersion jp) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                jp.getId(),
                vtp.loadColumnsTemplate(),
                ".*" + getMajorDelimiter() + jp.getId() + getMinorDelimiter() + ".*",
                vtp.loadTemplate());
    }


    private static String getPlatformViewRegex(boolean isBuild, String platformPart, List<Platform> platforms, boolean isForBuild) {
        //.provider x -wahtever?
        //thsi is tricky and bug-prone
        String suffix = getPlatformSuffixRegexString(isBuild);
        String prefix = ".*" + getEscapedMajorDelimiter();
        if (isForBuild) {
            prefix = "";
        }
        for (Platform orig : platforms) {
            if (orig.getId().equals(platformPart)) {
                return prefix + platformPart + suffix + ".*";
            } else if (orig.getArchitecture().equals(platformPart)) {
                //ignoring prefix, we are behind os anyway
                return ".*" + getEscapedMinorDelimiter() + platformPart + suffix + ".*";
            } else if ((orig.getOs() + orig.getVersion()).equals(platformPart)) {
                return prefix + platformPart + getEscapedMinorDelimiter() + "[0-9a-zA-Z_]{2,8}"/*arch*/ + suffix + ".*";
            } else if (orig.getOs().equals(platformPart)) {
                //this may be naive, but afaik ncessary, otherwise el, f, w  would match everything
                return prefix + platformPart + "[0-9]{1,1}" + "[0-9a-zA-Z]{0,5}" + getEscapedMinorDelimiter() + "[0-9a-zA-Z_]{2,8}"/*arch*/ + suffix + ".*";
            }
        }
        return platformPart + " Is strange as was not found";
    }

    private static String getPlatformSuffixRegexString(boolean isBuild) {
        if (isBuild) {
            //if it is build platform, then it is blah-os.arch-something
            return getEscapedMajorDelimiter();
        } else {
            //if it is run platform, then it is blah-os.arch.provider-something
            return getEscapedMinorDelimiter();
        }
        //for both use, if necessary
        //(major|minor)
    }

    public static JenkinsViewTemplateBuilder getProjectTemplate(String project, VersionlessPlatform platform) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                vtp.getProjectViewName(project, Optional.of(platform.getId())),
                vtp.loadColumnsTemplate(),
                getProjectViewRegex(project, platform),
                vtp.loadTemplate());
    }

    private static String getProjectViewRegex(String project, VersionlessPlatform platform) {
        return ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform, false) + "|"
                + "build" + getEscapedMajorDelimiter() + ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform, true) + "|"
                + pull(project);
    }

    public static JenkinsViewTemplateBuilder getProjectTemplate(String viewName, Optional<String> platform, Optional<List<Platform>> platforms) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                vtp.getProjectViewName(viewName, platform),
                vtp.loadColumnsTemplate(),
                getProjectViewRegex(viewName, platform, platforms),
                vtp.loadTemplate());
    }

    public static JenkinsViewTemplateBuilder getVariantTempalte(String id) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                id,
                vtp.loadColumnsTemplate(),
                "^" + id + "[_\\.-].*" + "|" + ".*[_\\.-]" + id + "[_\\.-].*" + "|" + ".*[_\\.-]" + id + "$",
                vtp.loadTemplate());
    }

    private static String getProjectViewRegex(String project, Optional<String> platform, Optional<List<Platform>> platforms) {
        if (!platform.isPresent()) {
            return ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + ".*" + "|"
                    + pull(project);
        } else {
            if (platforms.isPresent()) {
                return ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform.get(), platforms.get(), false) + "|" +
                        "build" + getEscapedMajorDelimiter() + ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform.get(), platforms.get(),
                        true) + "|"
                        + pull(project);

            } else {
                return ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + ".*" + platform.get() + ".*" + "|"
                        + pull(project);
            }
        }
    }

    @NotNull
    private static String pull(String project) {
        return "pull" + getEscapedMajorDelimiter() + ".*" + getEscapedMajorDelimiter() + project;
    }

    public static JenkinsViewTemplateBuilder getTaskTemplate(String task, Optional<String> columns, VersionlessPlatform platform) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                getTaskViewName(task, Optional.of(platform.getId())),
                columns.orElse(vtp.loadColumnsTemplate()),
                getTaskViewRegex(task, platform),
                vtp.loadTemplate());
    }

    private static String getTaskViewRegex(String task, VersionlessPlatform platform) {
        return task + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform, false);
    }

    public static JenkinsViewTemplateBuilder getTaskTemplate(String viewName, Optional<String> columns, Optional<String> platform, Optional<List<Platform>> platforms) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                getTaskViewName(viewName, platform),
                columns.orElse(vtp.loadColumnsTemplate()),
                getTaskViewRegex(viewName, platform, platforms),
                vtp.loadTemplate());
    }

    private static String getTaskViewName(String viewName, Optional<String> platform) {
        if (!platform.isPresent()) {
            return viewName;
        } else {
            return viewName + "-" + platform.get();
        }
    }

    private static String getTaskViewRegex(String task, Optional<String> platform, Optional<List<Platform>> platforms) {
        if (!platform.isPresent()) {
            return task + getEscapedMajorDelimiter() + ".*";
        } else {
            if (platforms.isPresent()) {
                return task + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform.get(), platforms.get(), false);
            } else {
                return task + getEscapedMajorDelimiter() + ".*" + platform.get() + ".*";
            }
        }
    }

    public String expand() {
        return JenkinsJobTemplateBuilder.prettyPrint(template
                .replace(VIEW_NAME, name)
                .replace(COLUMNS, columns)
                .replace(VIEW_REGEX, regex.toString()));
    }

    public InputStream expandToStream() {
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

    public String toString(int depth) {
        return spaces(depth) + toString();
    }

    private static String spaces(int depth) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < depth) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
