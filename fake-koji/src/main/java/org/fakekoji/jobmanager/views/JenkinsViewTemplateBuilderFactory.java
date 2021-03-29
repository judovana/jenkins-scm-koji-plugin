package org.fakekoji.jobmanager.views;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/*
     <!-- ~ojdk11~shenandoah~upstream~cpu-otherStuf -->
    <!-- mind the dashes, mind the pull!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*|pull-.*-ojdk11~shenandoah~upstream~cpu -->
    <!-- mind the dashes, mind the build!-->
    <!-- .*-ojdk11~shenandoah~upstream~cpu-.*  instead of .*-ojdk11~shenandoah~upstream~cpu-.*-.*
 */
public class JenkinsViewTemplateBuilderFactory {

    public static JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder getJenkinsViewTemplateBuilderFolder(String tab) throws IOException {
        return new JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder(tab, new NestedViewTemplateProvider().loadTemplate());
    }


    static String getMajorDelimiter() {
        return Job.DELIMITER;
    }

    static String getMinorDelimiter() {
        return Job.VARIANTS_DELIMITER;
    }

    static String getEscapedMajorDelimiter() {
        return escape(getMajorDelimiter());
    }

    static String getEscapedMinorDelimiter() {
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
        return prefix + platform.getOs() + "[0-9a-zA-Z]{1,6}" + getEscapedMinorDelimiter() + platform.getArch() + getPlatformSuffixRegexString(isBuild) + ".*";
    }

    public static JenkinsViewTemplateBuilder getPlatformTemplate(String platform, List<Platform> platforms) throws IOException {
        ViewTemplateProvider vtp = new ViewTemplateProvider();
        return new JenkinsViewTemplateBuilder(
                vtp.getPlatformmViewName(platform),
                vtp.loadColumnsTemplate(),
                getPlatformViewRegex(false, platform, platforms, false),
                vtp.loadTemplate());
    }

    public static JenkinsViewTemplateBuilder getJavaPlatformTemplate(JDKVersion jp, Optional<String> platform, Optional<List<Platform>> platforms) throws IOException {
        return getProjectTemplate(jp.getId(), platform, platforms);
    }
    public static JenkinsViewTemplateBuilder getJavaPlatformTemplate(JDKVersion jp, VersionlessPlatform vp) throws IOException {
        return getProjectTemplate(jp.getId(), vp);
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
            } else if ((orig.getOsVersion()).equals(platformPart)) {
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
                vtp.getPlatformmViewName(id),
                vtp.loadColumnsTemplate(),
                (".*"+id.replaceAll("\\.{2,}","MANYDOTS").replace(".","\\.").replace("|",".*").replace("MANYDOTS",".*")+".*").replaceAll("(\\.\\*)+",".*"),/*The last replace is VERY important time savior*/
                /*note, .*.*.*.* takes HOURS, wher eif you compres sit to .* it is seconds*/
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

}
