package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.model.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/*
     <!-- ~ojdk11~shenandoah~upstream~cpu-otherStuf -->
    <!-- mind the dashes, mind the pull!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*|pull-.*-ojdk11~shenandoah~upstream~cpu -->
 */
public class JenkinsViewTemplateBuilder implements CharSequence {

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

    public static JenkinsViewTemplateBuilder getPlatformTemplate(String platform, List<Platform> platforms) throws IOException {
        return new JenkinsViewTemplateBuilder(
                getPlatformmViewName(platform),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW_DEFAULT_COLUMNS),
                getPlatformViewRegex(platform, platforms),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VIEW));
    }

    private static String getPlatformmViewName(String viewName) {
        return "." + viewName;
    }


    private static String getPlatformViewRegex(String viewName, List<Platform> platforms) {
        //.provider x -wahtever?
        for (Platform orig : platforms) {
            if (orig.getId().equals(viewName)) {
                return ".*" + getEscapedMajorDelimiter() + viewName + "(" + getEscapedMajorDelimiter() + "|" + getEscapedMinorDelimiter() + ")" + ".*";
            } else if (orig.getArchitecture().equals(viewName)) {
                return ".*" + getEscapedMinorDelimiter() + viewName + "(" + getEscapedMajorDelimiter() + "|" + getEscapedMinorDelimiter() + ")" + ".*";
            } else if ((orig.getOs() + orig.getVersion()).equals(viewName)) {
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMinorDelimiter() + ".*";
            } else if (orig.getOs().equals(viewName)) {
                //this may be naive, but afaik ncessary, otherwise el, f, w  would match everything
                return ".*" + getEscapedMajorDelimiter() + viewName + "[0-9].*";
            }
        }
        return viewName + " Is strange as was not found";
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
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMajorDelimiter() + getPlatformViewRegex(platform.get(), platforms.get()) + "|" + pull(viewName);
            } else {
                return ".*" + getEscapedMajorDelimiter() + viewName + getEscapedMajorDelimiter() + ".*" + platform.get() + ".*|" + pull(viewName);
            }
        }
    }

    @NotNull
    private static String pull(String viewName) {
        return "pull" + getEscapedMajorDelimiter() + ".*" + getEscapedMajorDelimiter() + viewName;
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
                return viewName + getEscapedMajorDelimiter() + getPlatformViewRegex(platform.get(), platforms.get());
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
