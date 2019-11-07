package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
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
    static final String PLATFORM_NAME = "%{PLATFORM_NAME}";
    static final String PULL_SCRIPT = "%{PULL_SCRIPT}";
    static final String DESTROY_SCRIPT = "%{DESTROY_SCRIPT}";
    static final String SCM_POLL_SCHEDULE = "%{SCM_POLL_SCHEDULE}";

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
    static final String VM_NAME_OR_LOCAL_VAR = OTOOL_BASH_VAR_PREFIX + "VM_NAME_OR_LOCAL=";
    static final String PROJECT_PATH_VAR = OTOOL_BASH_VAR_PREFIX + "PROJECT_PATH=";
    static final String ARCH_VAR = OTOOL_BASH_VAR_PREFIX + "ARCH=";
    static final String JDK_VERSION_VAR = OTOOL_BASH_VAR_PREFIX + "JDK_VERSION=";
    static final String OJDK_VAR = OTOOL_BASH_VAR_PREFIX + "OJDK=";
    static final String PLATFORM_PROVIDER_VAR = OTOOL_BASH_VAR_PREFIX + "PLATFORM_PROVIDER=";
    static final String RELEASE_SUFFIX_VAR = OTOOL_BASH_VAR_PREFIX + "RELEASE_SUFFIX=";
    static final String PROJECT_NAME_VAR = OTOOL_BASH_VAR_PREFIX + "PROJECT_NAME=";
    static final String PACKAGE_NAME_VAR = OTOOL_BASH_VAR_PREFIX + "PACKAGE_NAME=";
    static final String OS_VAR = OTOOL_BASH_VAR_PREFIX + "OS=";

    private String template;

    public JenkinsJobTemplateBuilder(String template) {
        this.template = template;
    }

    public JenkinsJobTemplateBuilder buildPullScriptTemplate(
            String projectName,
            Product product,
            String repositoriesRootPath,
            File scriptsRoot
    ) {
        final String pullScript = SHEBANG + XML_NEW_LINE +
                EXPORT + " " + PROJECT_NAME_VAR + XML_APOS + projectName + XML_APOS + XML_NEW_LINE +
                EXPORT + " " + PROJECT_PATH_VAR + XML_APOS + Paths.get(repositoriesRootPath, projectName) + XML_APOS + XML_NEW_LINE +
                EXPORT + " " + JDK_VERSION_VAR + XML_APOS + product.getVersion() + XML_APOS + XML_NEW_LINE +
                EXPORT + " " + PACKAGE_NAME_VAR + XML_APOS + product.getPackageName() + XML_APOS + XML_NEW_LINE +
                BASH +  " '" + Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, PULL_SCRIPT_NAME) + "'";

        template = template.replace(PULL_SCRIPT, pullScript);
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

    JenkinsJobTemplateBuilder buildKojiXmlRpcApiTemplate(
            String packageName,
            String arch,
            List<String> tags,
            String subpackageBlacklist,
            String subpackageWhitelist
    ) throws IOException {
        template = template
                .replace(XML_RPC_API, loadTemplate(JenkinsTemplate.KOJI_XML_RPC_API_TEMPLATE))
                .replace(PACKAGE_NAME, packageName)
                .replace(ARCH, arch)
                .replace(TAGS, '{' + String.join(",", tags) + '}')
                .replace(SUBPACKAGE_BLACKLIST, subpackageBlacklist)
                .replace(SUBPACKAGE_WHITELIST, subpackageWhitelist);
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

    String fillExportedVariablesForBuildTask(
            Platform platform,
            String jdkVersion,
            String jdkLabel,
            String projectName,
            String releaseSuffix
    ) {
        return EXPORT + ' ' + OJDK_VAR + jdkLabel+ XML_NEW_LINE
                + EXPORT + ' ' + JDK_VERSION_VAR + jdkVersion + XML_NEW_LINE
                + EXPORT + ' ' + PROJECT_NAME_VAR + projectName + XML_NEW_LINE
                + EXPORT + ' ' + RELEASE_SUFFIX_VAR + releaseSuffix + XML_NEW_LINE
                + EXPORT + ' ' + ARCH_VAR + platform.getArchitecture() + XML_NEW_LINE
                + EXPORT + ' ' + OS_VAR + platform.getOs() + '.' + platform.getVersion() + XML_NEW_LINE;
    }

    String fillExportedVariables(
            Map<TaskVariant, TaskVariantValue> variants,
            String platformName,
            String platformProvider
    ) {
        return variants.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                .map(entry -> EXPORT + ' ' + OTOOL_BASH_VAR_PREFIX + entry.getKey().getId() + '=' + entry.getValue().getId())
                .collect(Collectors.joining(XML_NEW_LINE)) +
                XML_NEW_LINE + EXPORT + ' ' + VM_NAME_OR_LOCAL_VAR + platformName +
                XML_NEW_LINE + EXPORT + ' ' + PLATFORM_PROVIDER_VAR + platformProvider + XML_NEW_LINE;
    }

    public static String fillBuildPlatform(Platform platform, Task.FileRequirements fileRequirements) {
        final List<String> platforms = new LinkedList<>();
        if (fileRequirements.isSource()) {
            platforms.add("src");
        }
        switch (fileRequirements.getBinary()) {
            case BINARY:
                platforms.add(platform.getString());
                break;
            case BINARIES:
                return "";
            case NONE:
                break;
        }
        return String.join(" ", platforms);
    }

    public JenkinsJobTemplateBuilder buildScriptTemplate(
            String projectName,
            Product product,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            File scriptsRoot
    ) throws IOException {
        final String vmName;
        final List<String> nodes;
        switch (task.getMachinePreference()) {
            case HW:
                if (platform.getHwNodes().isEmpty()) {
                    vmName = platform.getVmName();
                    nodes = platform.getVmNodes();
                } else {
                    vmName = LOCAL;
                    nodes = platform.getHwNodes();
                }
                break;
            case HW_ONLY:
                vmName = LOCAL;
                nodes = platform.getHwNodes();
                break;
            case VM:
                if (platform.getVmNodes().isEmpty()) {
                    vmName = LOCAL;
                    nodes = platform.getHwNodes();
                } else {
                    vmName = platform.getVmName();
                    nodes = platform.getVmNodes();
                }
                break;
            case VM_ONLY:
                vmName = platform.getVmName();
                nodes = platform.getVmNodes();
                break;
            default:
                throw new RuntimeException("Unknown machine preference");
        }
        final String exportedVariables = fillExportedVariables(
                variants,
                vmName,
                platform.getProvider()
        ) + fillExportedVariablesForBuildTask(
                platform,
                product.getVersion(),
                'o' + product.getId(),
                projectName,
                variants.entrySet()
                        .stream()
                        .filter(e -> e.getKey().getType() == Task.Type.BUILD)
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .map(e -> e.getValue().getId())
                        .collect(Collectors.joining(".")) + '.' + platform.getString()
        );
        template = template
                .replace(NODES, String.join(" ", nodes))
                .replace(SCM_POLL_SCHEDULE, task.getScmPollSchedule())
                .replace(SHELL_SCRIPT, loadTemplate(JenkinsTemplate.SHELL_SCRIPT_TEMPLATE))
                .replace(TASK_SCRIPT, task.getScript())
                .replace(RUN_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME).toString())
                .replace(EXPORTED_VARIABLES, exportedVariables);
        if (!vmName.equals(LOCAL)) {
            return buildVmPostBuildTaskTemplate(platform, scriptsRoot);
        }
        template = template.replace(VM_POST_BUILD_TASK, "");
        return this;
    }

    JenkinsJobTemplateBuilder buildVmPostBuildTaskTemplate(Platform platform, File scriptsRoot) throws IOException {
        template = template
                .replace(VM_POST_BUILD_TASK, loadTemplate(JenkinsTemplate.VM_POST_BUILD_TASK_TEMPLATE))
                .replace(DESTROY_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, platform.getProvider(), DESTROY_SCRIPT_NAME).toString())
                .replace(PLATFORM_NAME, platform.getVmName());
        return this;
    }

    public JenkinsJobTemplateBuilder buildPostBuildTasks(String postBuildTasksTemplate) {
        template = template.replace(POST_BUILD_TASKS, postBuildTasksTemplate);
        return this;
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
        KOJI_XML_RPC_API_TEMPLATE("koji-xml-rpc-api"),
        SHELL_SCRIPT_TEMPLATE("shell-script"),
        FAKEKOJI_XML_RPC_API_TEMPLATE("fakekoji-xml-rpc-api"),
        BUILD_PROVIDER_TEMPLATE("provider"),
        BUILD_PROVIDERS_TEMPLATE("/providers"),
        TASK_JOB_TEMPLATE("task-job"),
        PULL_JOB_TEMPLATE("pull-job"),
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
