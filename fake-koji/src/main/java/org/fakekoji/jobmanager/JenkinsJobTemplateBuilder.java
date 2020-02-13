package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.model.OToolVariable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JenkinsJobTemplateBuilder {

    private static final String NEW_LINE = System.getProperty("line.separator");
    public static final String XML_DECLARATION = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n";

    private static final String JENKINS_TEMPLATES = "jenkins-templates";

    static final String BUILD_PROVIDER_TOP_URL = "%{BUILD_PROVIDER_TOP_URL}";
    static final String BUILD_PROVIDER_DOWNLOAD_URL = "%{BUILD_PROVIDER_DOWNLOAD_URL}";
    static final String BUILD_PROVIDERS = "%{BUILD_PROVIDERS}";
    static final String XML_RPC_API = "%{XML_RPC_API}";
    static final String PACKAGE_NAME = "%{PACKAGE_NAME}";
    static final String ARCH = "%{ARCH}";
    static final String TAGS = "%{TAGS}";
    static final String SUBPACKAGE_BLACKLIST = "%{SUBPACKAGE_BLACKLIST}";
    static final String SUBPACKAGE_WHITELIST = "%{SUBPACKAGE_WHITELIST}";
    static final String PROJECT_NAME = "%{PROJECT_NAME}";
    static final String BUILD_VARIANTS = "%{BUILD_VARIANTS}";
    static final String PLATFORM = "%{PLATFORM}";
    static final String IS_BUILT = "%{IS_BUILT}";
    static final String VM_POST_BUILD_TASK = "%{VM_POST_BUILD_TASK}";
    static final String POST_BUILD_TASKS = "%{POST_BUILD_TASKS}";
    static final String NODES = "%{NODES}";
    static final String SHELL_SCRIPT = "%{SHELL_SCRIPT}";
    static final String TASK_SCRIPT = "%{TASK_SCRIPT}";
    static final String RUN_SCRIPT = "%{RUN_SCRIPT}";
    static final String EXPORTED_VARIABLES = "%{EXPORTED_VARIABLES}";
    static final String SHUTDOWN_VARIABLES = "%{SHUTDOWN_VARIABLES}";
    static final String PLATFORM_NAME = "%{PLATFORM_NAME}";
    static final String PULL_SCRIPT = "%{PULL_SCRIPT}";
    static final String DESTROY_SCRIPT = "%{DESTROY_SCRIPT}";
    static final String SCM_POLL_SCHEDULE = "%{SCM_POLL_SCHEDULE}";
    static final String TRIGGER = "%{TRIGGER}";

    static final String XML_NEW_LINE = "&#13;";
    static final String XML_APOS = "&apos;";
    static final String LOCAL = "local";
    static final String EXPORT = "export";
    static final String O_TOOL = "otool";
    static final String VAGRANT = "vagrant";
    static final String PULL_SCRIPT_NAME = "pull.sh";
    static final String RUN_SCRIPT_NAME = "run.sh";
    static final String DESTROY_SCRIPT_NAME = "destroy.sh";
    static final String BASH = "bash";
    static final String SHEBANG = "#!/bin/sh";

    static final String JENKINS = "jenkins";

    static final String OTOOL_BASH_VAR_PREFIX = "OTOOL_";
    static final String VM_NAME_OR_LOCAL_VAR = "VM_NAME_OR_LOCAL";
    public static final String PROJECT_PATH_VAR = "PROJECT_PATH";
    static final String ARCH_VAR = "ARCH";
    public static final String JDK_VERSION_VAR = "JDK_VERSION";
    public static final String OJDK_VAR = "OJDK";
    static final String PLATFORM_PROVIDER_VAR = "PLATFORM_PROVIDER";
    public static final String RELEASE_SUFFIX_VAR = "RELEASE_SUFFIX";
    public static final String PROJECT_NAME_VAR = "PROJECT_NAME";
    public static final String PACKAGE_NAME_VAR = "PACKAGE_NAME";
    public static final String NO_CHANGE_RETURN_VAR = "NO_CHANGE_RETURN";
    static final String JOB_NAME = "JOB_NAME";
    //this name is used to creation of VM name, which vagrant enforces to 60 chars
    static final String JOB_NAME_SHORTENED = "JOB_NAME_SHORTENED";
    static final String OS_VAR = "OS";
    static final String OS_NAME_VAR = "OS_NAME";
    static final String OS_VERSION_VAR = "OS_VERSION";

    private String template;
    private final NamesProvider job;

    public JenkinsJobTemplateBuilder(String template, NamesProvider job) {
        this.template = template;
        this.job = job;
    }

    public JenkinsJobTemplateBuilder buildPullScriptTemplate(
            final List<OToolVariable> exportedVariables,
            final File scriptsRoot
    ) {
        template = template.replace(
                PULL_SCRIPT,
                SHEBANG + XML_NEW_LINE + getExportedVariablesString(exportedVariables) +
                        BASH + " '" + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, PULL_SCRIPT_NAME) + "'"
        );
        return this;
    }

    public JenkinsJobTemplateBuilder buildBuildProvidersTemplate(Set<BuildProvider> buildProviders) throws IOException {
        final String buildProviderTemplate = loadTemplate(JenkinsTemplate.BUILD_PROVIDER_TEMPLATE);
        final String buildProviderTemplates = buildProviders.stream()
                .map(buildProvider -> buildProviderTemplate
                        .replace(BUILD_PROVIDER_TOP_URL, buildProvider.getTopUrl())
                        .replace(BUILD_PROVIDER_DOWNLOAD_URL, buildProvider.getDownloadUrl()))
                .collect(Collectors.joining(NEW_LINE));
        template = template.replace(
                BUILD_PROVIDERS,
                loadTemplate(JenkinsTemplate.BUILD_PROVIDERS_TEMPLATE)
                        .replace(BUILD_PROVIDERS, buildProviderTemplates)
        );
        return this;
    }

    public JenkinsJobTemplateBuilder buildKojiXmlRpcApiTemplate(
            String packageName,
            Platform platform,
            List<String> subpackageBlacklist,
            List<String> subpackageWhitelist
    ) throws IOException {
        template = template
                .replace(XML_RPC_API, loadTemplate(JenkinsTemplate.KOJI_XML_RPC_API_TEMPLATE))
                .replace(PACKAGE_NAME, packageName)
                .replace(ARCH, platform.getKojiArch().orElse(platform.getArchitecture()))
                .replace(TAGS, String.join(" ", platform.getTags()))
                .replace(SUBPACKAGE_BLACKLIST, String.join(" ", subpackageBlacklist))
                .replace(SUBPACKAGE_WHITELIST, String.join(" ", subpackageWhitelist));
        return this;
    }

    public JenkinsJobTemplateBuilder buildFakeKojiXmlRpcApiTemplate(
            String projectName,
            Map<TaskVariant, TaskVariantValue> buildVariants,
            String platform,
            boolean isBuilt
    ) throws IOException {
        template = template
                .replace(XML_RPC_API, loadTemplate(JenkinsTemplate.FAKEKOJI_XML_RPC_API_TEMPLATE))
                .replace(PROJECT_NAME, projectName)
                .replace(BUILD_VARIANTS, buildVariants.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                        .map(entry -> entry.getKey().getId() + '=' + entry.getValue().getId())
                        .collect(Collectors.joining(" ")))
                .replace(PLATFORM, platform)
                .replace(IS_BUILT, String.valueOf(isBuilt));
        return this;
    }

    public static String fillBuildPlatform(Platform platform, Task.FileRequirements fileRequirements) {
        final List<String> platforms = new LinkedList<>();
        if (fileRequirements.isSource()) {
            platforms.add("src");
        }
        switch (fileRequirements.getBinary()) {
            case BINARY:
                platforms.add(platform.getId());
                break;
            case BINARIES:
                return "";
            case NONE:
                break;
        }
        return String.join(" ", platforms);
    }

    public JenkinsJobTemplateBuilder buildTriggerTemplate(
            final String scmPollSchedule
    ) throws IOException {
        template = template.replace(
                TRIGGER,
                loadTemplate(JenkinsTemplate.TRIGGER).replace(SCM_POLL_SCHEDULE, scmPollSchedule)
        );
        return this;
    }

    public JenkinsJobTemplateBuilder buildScriptTemplate(
            Task task,
            String provider,
            Platform platform,
            File scriptsRoot,
            List<OToolVariable> exportedVariables
    ) throws IOException {
        final Platform.Provider platformProvider = platform.getProviders()
                .stream()
                .filter(p -> p.getId().equals(provider))
                .findFirst()
                .get(); // TODO: should throw an exception (ManagementException I guess)
        final String vmName;
        final List<String> nodes;
        switch (task.getMachinePreference()) {
            case HW:
                if (platformProvider.getHwNodes().isEmpty()) {
                    vmName = platform.getVmName();
                    nodes = platformProvider.getVmNodes();
                } else {
                    vmName = LOCAL;
                    nodes = platformProvider.getHwNodes();
                }
                break;
            case HW_ONLY:
                vmName = LOCAL;
                nodes = platformProvider.getHwNodes();
                break;
            case VM:
                if (platformProvider.getVmNodes().isEmpty()) {
                    vmName = LOCAL;
                    nodes = platformProvider.getHwNodes();
                } else {
                    vmName = platform.getVmName();
                    nodes = platformProvider.getVmNodes();
                }
                break;
            case VM_ONLY:
                vmName = platform.getVmName();
                nodes = platformProvider.getVmNodes();
                break;
            default:
                throw new RuntimeException("Unknown machine preference");
        }
        exportedVariables.add(new OToolVariable(PLATFORM_PROVIDER_VAR, platformProvider.getId()));
        exportedVariables.add(new OToolVariable(VM_NAME_OR_LOCAL_VAR, vmName));
        if (job != null) {
            if (job.getName() != null) {
                exportedVariables.add(new OToolVariable(JOB_NAME, job.getName()));
            }
            if (job.getShortName() != null) {
                exportedVariables.add(new OToolVariable(JOB_NAME_SHORTENED, job.getShortName()));
            }
        }
        exportedVariables.add(new OToolVariable(OS_VAR, platform.toOsVar()));
        exportedVariables.add(new OToolVariable(OS_NAME_VAR, platform.getOs()));
        exportedVariables.add(new OToolVariable(OS_VERSION_VAR, platform.getVersionNumber()));
        exportedVariables.add(new OToolVariable(ARCH_VAR, platform.getArchitecture()));
        template = template
                .replace(NODES, String.join(" ", nodes))
                .replace(SHELL_SCRIPT, loadTemplate(JenkinsTemplate.SHELL_SCRIPT_TEMPLATE))
                .replace(TASK_SCRIPT, task.getScript())
                .replace(RUN_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME).toString())
                .replace(EXPORTED_VARIABLES, getExportedVariablesString(exportedVariables));
        if (!vmName.equals(LOCAL)) {
            List<OToolVariable> shutdownVars = Collections.singletonList(
                    new OToolVariable(
                            JOB_NAME_SHORTENED,
                            exportedVariables.stream()
                                    .filter(var -> var.getName().equals(JOB_NAME_SHORTENED))
                                    .map(OToolVariable::getValue)
                                    .findFirst()
                                    .orElse(""),
                            null,
                            true,
                            false,
                            false
                    )
            );
            return buildVmPostBuildTaskTemplate(provider, platform.getVmName(), scriptsRoot, shutdownVars);
        }
        template = template.replace(VM_POST_BUILD_TASK, "");
        return this;
    }

    JenkinsJobTemplateBuilder buildVmPostBuildTaskTemplate(
            String platformProvider,
            String platformVMName,
            File scriptsRoot,
            List<OToolVariable> shutdownVariables
    ) throws IOException {
        template = template
                .replace(VM_POST_BUILD_TASK, loadTemplate(JenkinsTemplate.VM_POST_BUILD_TASK_TEMPLATE))
                .replace(DESTROY_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, platformProvider, DESTROY_SCRIPT_NAME).toString())
                .replace(SHUTDOWN_VARIABLES, getExportedVariablesString(shutdownVariables, ""))
                .replace(PLATFORM_NAME, platformVMName);
        return this;
    }

    public JenkinsJobTemplateBuilder buildPostBuildTasks(String postBuildTasksTemplate) {
        template = template.replace(POST_BUILD_TASKS, postBuildTasksTemplate);
        return this;
    }

    private String getExportedVariablesString(final List<OToolVariable> exportedVariables) {
        return getExportedVariablesString(exportedVariables, XML_NEW_LINE);
    }

    private String getExportedVariablesString(final List<OToolVariable> exportedVariables, String terminal) {
        return exportedVariables
                .stream()
                .map(var -> getVariableString(var, terminal))
                .sorted()
                .collect(Collectors.joining());
    }

    static String getVariableString(final OToolVariable variable, final String terminal) {
        final String name = (variable.isDefaultPrefix() ? OTOOL_BASH_VAR_PREFIX : "") + variable.getName();
        return (variable.isCommentedOut() ? '#' : "") +
                (variable.isExported() ? EXPORT + ' ' : "") +
                name +
                '=' +
                variable.getValue() +
                variable.getComment().map(comment -> " # " + comment).orElse("") +
                terminal;
    }

    String getTemplate() {
        return template;
    }

    public String prettyPrint() {
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            final Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(template)));
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate(
                    "//text()[normalize-space()='']",
                    document,
                    XPathConstants.NODESET
            );
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadTemplate(JenkinsTemplate jenkinsTemplate) throws IOException {
        return Utils.readResource(jenkinsTemplate.getValue());
    }

    public enum JenkinsTemplate {
        BUILD_PROVIDERS_TEMPLATE("/providers"),
        BUILD_PROVIDER_TEMPLATE("provider"),
        FAKEKOJI_XML_RPC_API_TEMPLATE("fakekoji-xml-rpc-api"),
        KOJI_XML_RPC_API_TEMPLATE("koji-xml-rpc-api"),
        PULL_JOB_TEMPLATE("pull-job"),
        SHELL_SCRIPT_TEMPLATE("shell-script"),
        TASK_JOB_TEMPLATE("task-job"),
        TRIGGER("trigger"),
        VM_POST_BUILD_TASK_TEMPLATE("vm-post-build-task");

        private final String value;

        JenkinsTemplate(final String template) {
            this.value = Paths.get(JENKINS_TEMPLATES, template + ".xml").toString();
        }

        public String getValue() {
            return value;
        }
    }
}
