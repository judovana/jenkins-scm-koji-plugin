package org.fakekoji.jobmanager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Objects;

public class JenkinsUpdateVmTemplateBuilder implements CharSequence {


    static final String NODE = "%{NODE}";
    static final String PROVIDER = "%{PROVIDER}";
    static final String SCRIPTS_ROOT = "%{SCRIPTS_ROOT}";

    private final String name;

    private final String node;
    private final String provider;
    private final String scriptsRoot;
    private final String shutdownVariables;
    private final String destroyScript;
    private final String platformName;

    private final String template;
    private final String postTask;


    public JenkinsUpdateVmTemplateBuilder(
            String name, String node, String provider, String scriptsRoot, String shutdownVariables,
            String destroyScript, String platformName, String template, String postTask) {
        this.name = name;
        this.node = node;
        this.provider = provider;
        this.scriptsRoot = scriptsRoot;
        this.shutdownVariables = shutdownVariables;
        this.destroyScript = destroyScript;
        this.platformName = platformName;
        this.template = template;
        this.postTask = postTask;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JenkinsUpdateVmTemplateBuilder that = (JenkinsUpdateVmTemplateBuilder) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(node, that.node) &&
                Objects.equals(provider, that.provider) &&
                Objects.equals(scriptsRoot, that.scriptsRoot) &&
                Objects.equals(shutdownVariables, that.shutdownVariables) &&
                Objects.equals(destroyScript, that.destroyScript) &&
                Objects.equals(platformName, that.platformName) &&
                Objects.equals(template, that.template) &&
                Objects.equals(postTask, that.postTask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, node, provider, scriptsRoot, shutdownVariables, destroyScript, platformName, template, postTask);
    }

    public static JenkinsUpdateVmTemplateBuilder getUpdateTemplate(
            String name, String node, String provider, File scriptsRoot, String platformName) throws IOException {
        return new JenkinsUpdateVmTemplateBuilder(
                name, node, provider, scriptsRoot.getAbsolutePath(), "",
                Paths.get(scriptsRoot.getAbsolutePath(), JenkinsJobTemplateBuilder.JENKINS, provider, JenkinsJobTemplateBuilder.DESTROY_SCRIPT_NAME).toAbsolutePath().toString(),
                platformName,
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.UPDATE_VM_JOB),
                JenkinsJobTemplateBuilder.loadTemplate(JenkinsJobTemplateBuilder.JenkinsTemplate.VM_POST_BUILD_TASK_TEMPLATE)
        );
    }


    public String expand() {
        String r = template
                .replace(NODE, node)
                .replace(PROVIDER, provider)
                .replace(SCRIPTS_ROOT, scriptsRoot);
        if (platformName.equals("local")) {
            r = r.replace(JenkinsJobTemplateBuilder.VM_POST_BUILD_TASK, "");
        } else {
            r = r.replace(JenkinsJobTemplateBuilder.VM_POST_BUILD_TASK, postTask);
        }
        r = r.replace(JenkinsJobTemplateBuilder.SHUTDOWN_VARIABLES, shutdownVariables)
                .replace(JenkinsJobTemplateBuilder.DESTROY_SCRIPT, destroyScript)
                .replace(JenkinsJobTemplateBuilder.PLATFORM_NAME, platformName);
        return JenkinsJobTemplateBuilder.prettyPrint(r);
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
