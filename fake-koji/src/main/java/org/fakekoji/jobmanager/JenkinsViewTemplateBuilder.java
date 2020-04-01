package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.model.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/*
     <!-- ~ojdk11~shenandoah~upstream~cpu-otherStuf -->
    <!-- mind the dashes, mind the pull!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*|pull-.*-ojdk11~shenandoah~upstream~cpu -->
 */
public class JenkinsViewTemplateBuilder implements CharSequence {

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

    public static JenkinsViewTemplateBuilder getPlatformTemplate(VersionlessPlatform platform) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getPlatformmViewName(platform.getId()),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS),
                getPlatformViewRegex(false, platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getPlatformViewRegex(boolean isBuild, VersionlessPlatform platform) {
        return ".*" + getEscapedMajorDelimiter() + platform.os + "[0-9a-zA-Z]{1,6}" + getEscapedMinorDelimiter() + platform.arch + getPlatformSuffixRegexString(isBuild)+ ".*";
    }

    public static JenkinsViewTemplateBuilder getPlatformTemplate(String platform, List<Platform> platforms) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getPlatformmViewName(platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS),
                getPlatformViewRegex(false, platform, platforms),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getPlatformmViewName(String viewName) {
        return "." + viewName;
    }


    private static String getPlatformViewRegex(boolean isBuild, String viewName, List<Platform> platforms) {
        //.provider x -wahtever?
        //thsi is tricky and bug-prone
        String suffix = getPlatformSuffixRegexString(isBuild);
        for (Platform orig : platforms) {
            if (orig.getId().equals(viewName)) {
                return ".*" + getEscapedMajorDelimiter() + viewName + suffix + ".*";
            } else if (orig.getArchitecture().equals(viewName)) {
                return ".*" + getEscapedMinorDelimiter() + viewName + suffix + ".*";
            } else if ((orig.getOs() + orig.getVersion()).equals(viewName)) {
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMinorDelimiter()+"[0-9a-zA-Z]{2,8}"/*arch*/+suffix + ".*";
            } else if (orig.getOs().equals(viewName)) {
                //this may be naive, but afaik ncessary, otherwise el, f, w  would match everything
                return ".*" + getEscapedMajorDelimiter() + viewName + "[0-9]{1,1}"+"[0-9a-zA-Z]{0,5}" + getEscapedMinorDelimiter()+"[0-9a-zA-Z]{2,8}"/*arch*/+suffix + ".*";
            }
        }
        return viewName + " Is strange as was not found";
    }

    private static String getPlatformSuffixRegexString(boolean isBuild) {
        if (isBuild){
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
        return new JenkinsViewTemplateBuilder(
                getProjectViewName(project, Optional.of(platform.getId())),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS),
                getProjectViewRegex(project, platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getProjectViewRegex(String project, VersionlessPlatform platform) {
        return ".*" + getEscapedMajorDelimiter() + project + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform) + "|" + pull(project);
    }

    public static JenkinsViewTemplateBuilder getProjectTemplate(String viewName, Optional<String> platform, Optional<List<Platform>> platforms) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getProjectViewName(viewName, platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS),
                getProjectViewRegex(viewName, platform, platforms),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getProjectViewName(String viewName, Optional<String> platform) {
        if (!platform.isPresent()) {
            return "~" + viewName;
        } else {
            return "~" + viewName + "-" + platform.get();
        }
    }

    private static String getProjectViewRegex(String viewName, Optional<String> platform, Optional<List<Platform>> platforms) {
        if (!platform.isPresent()) {
            return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMajorDelimiter() + ".*|" + pull(viewName);
        } else {
            if (platforms.isPresent()) {
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform.get(), platforms.get()) + "|" + pull(viewName);
            } else {
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMajorDelimiter() + ".*" + platform.get() + ".*|" + pull(viewName);
            }
        }
    }

    @NotNull
    private static String pull(String viewName) {
        return "pull" + getEscapedMajorDelimiter() + ".*" + getEscapedMajorDelimiter() + viewName;
    }

    public static JenkinsViewTemplateBuilder getTaskTemplate(String task, Optional<String> columns, VersionlessPlatform platform) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getTaskViewName(task, Optional.of(platform.getId())),
                columns.orElse(JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS)),
                getTaskViewRegex(task, platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getTaskViewRegex(String task, VersionlessPlatform platform) {
        return task + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform);
    }

    public static JenkinsViewTemplateBuilder getTaskTemplate(String viewName, Optional<String> columns, Optional<String> platform, Optional<List<Platform>> platforms) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getTaskViewName(viewName, platform),
                columns.orElse(JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS)),
                getTaskViewRegex(viewName, platform, platforms),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getTaskViewName(String viewName, Optional<String> platform) {
        if (!platform.isPresent()) {
            return viewName;
        } else {
            return viewName + "-" + platform.get();
        }
    }

    private static String getTaskViewRegex(String viewName, Optional<String> platform, Optional<List<Platform>> platforms) {
        if (!platform.isPresent()) {
            return viewName + getEscapedMajorDelimiter() + ".*";
        } else {
            if (platforms.isPresent()) {
                return viewName + getEscapedMajorDelimiter() + getPlatformViewRegex(false, platform.get(), platforms.get());
            } else {
                return viewName + getEscapedMajorDelimiter() + ".*" + platform.get() + ".*|";
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
}
