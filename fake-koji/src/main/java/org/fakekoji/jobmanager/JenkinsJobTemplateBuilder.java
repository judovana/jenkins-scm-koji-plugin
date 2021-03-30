package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.model.OToolVariable;
import org.jetbrains.annotations.NotNull;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JenkinsJobTemplateBuilder {

    static final String NEW_LINE = System.getProperty("line.separator");
    public static final String XML_DECLARATION = "<?xml version=\"1.1\" encoding=\"UTF-8\" ?>\n";

    static final String JENKINS_TEMPLATES = "jenkins-templates";

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
    static final String POST_BUILD_TASK_PLUGIN = "%{POST_BUILD_TASK_PLUGIN}";
    static final String POST_BUILD_TASK_PLUGIN_ANALYSE = "%{POST_BUILD_TASK_PLUGIN_ANALYSE}";
    static final String POST_BUILD_TASK_PLUGIN_DESTROYVM = "%{POST_BUILD_TASK_PLUGIN_DESTROYVM}";
    static final String POST_BUILD_TASKS = "%{POST_BUILD_TASKS}";
    static final String NODES = "%{NODES}";
    static final String BUILDER_SCRIPT = "%{BUILDER}";
    static final String TASK_SCRIPT = "%{TASK_SCRIPT}";
    static final String RUN_SCRIPT = "%{RUN_SCRIPT}";
    static final String EXPORTED_VARIABLES = "%{EXPORTED_VARIABLES}";
    static final String TIMEOUT_MINUTES = "%{TIMEOUT_MINUTES}";
    static final String SHUTDOWN_VARIABLES = "%{SHUTDOWN_VARIABLES}";
    static final String PLATFORM_NAME = "%{PLATFORM_NAME}";
    static final String PULL_SCRIPT = "%{PULL_SCRIPT}";
    static final String MASTER_LABEL = "%{MASTER_LABEL}";
    static final String DESTROY_SCRIPT = "%{DESTROY_SCRIPT}";
    static final String ANALYSE_SCRIPT = "%{ANALYSE_SCRIPT}";
    static final String SCM_POLL_SCHEDULE = "%{SCM_POLL_SCHEDULE}";
    static final String TRIGGER = "%{TRIGGER}";

    static final String XML_NEW_LINE = "&#13;";
    static final String XML_APOS = "&apos;";
    static final String LOCAL = "local";
    static final String O_TOOL = "otool";
    static final String VAGRANT = "vagrant";
    static final String PULL_SCRIPT_NAME = "pull.sh";
    static final String RUN_SCRIPT_NAME = "run.sh";
    public static final String DESTROY_SCRIPT_NAME = "destroy.sh";
    public static final String ANALYSE_SCRIPT_NAME = "otool/wrappers/analyzeAndReportJenkinsJob.sh";
    public static final String BASH = "bash";
    public static final String SHEBANG = "#!/bin/sh";

    public static final String JENKINS = "jenkins";

    public static final String OTOOL_BASH_VAR_PREFIX = "OTOOL_";
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
    public static final String JOB_NAME_SHORTENED = "JOB_NAME_SHORTENED";
    public static final String OS_VAR = "OS";
    public static final String OS_NAME_VAR = "OS_NAME";
    public static final String OS_VERSION_VAR = "OS_VERSION";

    public static final String SOURCES= "src";
    public static final String NOARCH= "noarch";

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
        ).replace(MASTER_LABEL, AccessibleSettings.master.label);
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
            Task.FileRequirements fileRequirements,
            List<String> subpackageBlacklist,
            List<String> subpackageWhitelist
    ) throws IOException {
        template = template
                .replace(XML_RPC_API, loadTemplate(JenkinsTemplate.KOJI_XML_RPC_API_TEMPLATE))
                .replace(PACKAGE_NAME, packageName)
                .replace(ARCH, fillArch(platform, fileRequirements))
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

    public static String fillArch(Platform platform, Task.FileRequirements fileRequirements) {
        final List<String> archs = new LinkedList<>();
        if (fileRequirements.isSource()) {
            archs.add(SOURCES);
        }
        if (fileRequirements.isNoarch()) {
            archs.add(NOARCH);
        }
        switch (fileRequirements.getBinary()) {
            case BINARY:
                archs.add(platform.getKojiArch().orElse(platform.getArchitecture()));
                break;
            case BINARIES:
                return "";
            case NONE:
                break;
        }
        return String.join(" ", archs);
    }

    public static String fillBuildPlatform(Platform platform, Task.FileRequirements fileRequirements) {
        final List<String> platforms = new LinkedList<>();
        if (fileRequirements.isSource()) {
            platforms.add(SOURCES);
        }
        if (fileRequirements.isNoarch()) {
            platforms.add(NOARCH);
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
    private static class VmWithNodes {
        final String vmName;
        final List<String> nodes;

        public VmWithNodes(String vmName, List<String> nodes) {
            this.vmName = vmName;
            this.nodes = nodes;
        }
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
        exportedVariables.add(new OToolVariable(PLATFORM_PROVIDER_VAR, platformProvider.getId()));
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
        final  VmWithNodes  mWithNodes = getVmWithNodes(task, platform, exportedVariables, platformProvider);
        exportedVariables.add(new OToolVariable(VM_NAME_OR_LOCAL_VAR, mWithNodes.vmName));
        final String usedBuilder;
        if (task.getTimeoutInHours() <= 0 /*from help on this field, minimal timeout is 3 minutes. We are in hours here*/) {
            usedBuilder = loadTemplate(JenkinsTemplate.PLAINSHELL_SCRIPT_TEMPLATE);
        } else {
            usedBuilder = loadTemplate(JenkinsTemplate.TIMEOUTSHELL_SCRIPT_TEMPLATE);
        }
        template = template
                .replace(NODES, String.join("||", mWithNodes.nodes))
                .replace(BUILDER_SCRIPT, usedBuilder)
                .replace(TASK_SCRIPT, task.getScript())
                .replace(RUN_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), O_TOOL, RUN_SCRIPT_NAME).toString())
                .replace(EXPORTED_VARIABLES, getExportedVariablesString(exportedVariables))
                .replace(TIMEOUT_MINUTES, ""+(task.getTimeoutInHours()*60));
        if (!mWithNodes.vmName.equals(LOCAL)) {
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
            template = buildPostBuildTaskTemplate(template, provider, platform.getVmName(), scriptsRoot, shutdownVars, true, true);
            return this;
        }
        template = buildPostBuildTaskTemplate(template, provider, platform.getVmName(), scriptsRoot, new ArrayList<>(), false, true);
        return this;
    }

    @NotNull
    private static VmWithNodes getVmWithNodes(Task task, Platform platform, List<OToolVariable> exportedVariables, Platform.Provider platformProvider) {
        final VmWithNodes mWithNodes;
        switch (task.getMachinePreference()) {
            case HW:
                if (platformProvider.getHwNodes().isEmpty()) {
                    mWithNodes = new VmWithNodes(platform.getVmName(), expand(platformProvider.getVmNodes(), exportedVariables));
                } else {
                    mWithNodes = new VmWithNodes(LOCAL, expand(platformProvider.getHwNodes(), exportedVariables));
                }
                break;
            case HW_ONLY:
                mWithNodes = new VmWithNodes(LOCAL, expand(platformProvider.getHwNodes(), exportedVariables));
                break;
            case VM:
                if (platformProvider.getVmNodes().isEmpty()) {
                    mWithNodes = new VmWithNodes(LOCAL, expand(platformProvider.getHwNodes(), exportedVariables));
                } else {
                    mWithNodes = new VmWithNodes(platform.getVmName(), expand(platformProvider.getVmNodes(), exportedVariables));
                }
                break;
            case VM_ONLY:
                mWithNodes = new VmWithNodes(platform.getVmName(), expand(platformProvider.getVmNodes(), exportedVariables));
                break;
            default:
                throw new RuntimeException("Unknown machine preference");
        }
        return mWithNodes;
    }

    public static List<String> expand(List<String> nodes, List<OToolVariable> exportedVariables) {
        List<String> r = new ArrayList<>(nodes.size());
        for (String node : nodes) {
            String nwNode = node;
            for (OToolVariable var : exportedVariables) {
                nwNode = nwNode.replace("%{" + var.getFullName() + "}", var.getValue());
            }
            r.add(nwNode);
        }
        return r;
    }

    JenkinsJobTemplateBuilder buildPostBuildTaskTemplate(
            final String platformProvider,
            final String platformVMName,
            final File scriptsRoot,
            final List<OToolVariable> shutdownVariables,
            final boolean shutdownVm,
            final boolean analyseResults
    ) throws IOException {
        template = buildPostBuildTaskTemplate(template, platformProvider, platformVMName, scriptsRoot, shutdownVariables, shutdownVm, analyseResults);
        return this;
    }
   static String  buildPostBuildTaskTemplate(
            final String input,
            final String platformProvider,
            final String platformVMName,
            final File scriptsRoot,
            final List<OToolVariable> shutdownVariables,
            final boolean shutdownVm,
            final boolean analyseResults
    ) throws IOException {
       String i = input;
       String postbuild = "";
       if (shutdownVm || analyseResults) {
           postbuild = loadTemplate(JenkinsTemplate.POST_BUILD_TASK_PLUGIN);
       }
       i = i.replace(POST_BUILD_TASK_PLUGIN, postbuild);
       String shutdownTemplate = "";
       if (shutdownVm){
           shutdownTemplate = loadTemplate(JenkinsTemplate.POST_BUILD_TASK_PLUGIN_DESTROYVM);
       }
       i = i.replace(POST_BUILD_TASK_PLUGIN_DESTROYVM, shutdownTemplate);
       String analyseTemplate = "";
       if (analyseResults){
           analyseTemplate = loadTemplate(JenkinsTemplate.POST_BUILD_TASK_PLUGIN_ANALYSE);
       }
       i = i.replace(POST_BUILD_TASK_PLUGIN_ANALYSE, analyseTemplate);
       i = i.replace(DESTROY_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), JENKINS, platformProvider, DESTROY_SCRIPT_NAME).toString())
               .replace(ANALYSE_SCRIPT, Paths.get(scriptsRoot.getAbsolutePath(), ANALYSE_SCRIPT_NAME).toString())
               .replace(SHUTDOWN_VARIABLES, getExportedVariablesString(shutdownVariables, ""))
               .replace(PLATFORM_NAME, platformVMName);
       return i;
    }

    public JenkinsJobTemplateBuilder buildPostBuildTasks(String postBuildTasksTemplate) {
        template = template.replace(POST_BUILD_TASKS, postBuildTasksTemplate);
        return this;
    }

    private String getExportedVariablesString(final List<OToolVariable> exportedVariables) {
        return getExportedVariablesString(exportedVariables, XML_NEW_LINE);
    }

    private static  String getExportedVariablesString(final List<OToolVariable> exportedVariables, String terminal) {
        return exportedVariables
                .stream()
                .map(var -> var.getVariableString(terminal))
                .sorted()
                .collect(Collectors.joining());
    }


    String getTemplate() {
        return template;
    }

    public String prettyPrint() {
        return prettyPrint(template);
    }

    public static String prettyPrint(String template) {
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

    private static  Map<JenkinsTemplate,String> viewCache = new HashMap<>();
    public static String loadTemplate(JenkinsTemplate jenkinsTemplate) throws IOException {
        String o = viewCache.get(jenkinsTemplate);
        if (o == null){
         o =  Utils.readResource(jenkinsTemplate.getValue());
         viewCache.put(jenkinsTemplate, o);
        }
        return o;
    }

    public enum JenkinsTemplate {
        BUILD_PROVIDERS_TEMPLATE("/providers"),
        BUILD_PROVIDER_TEMPLATE("provider"),
        FAKEKOJI_XML_RPC_API_TEMPLATE("fakekoji-xml-rpc-api"),
        KOJI_XML_RPC_API_TEMPLATE("koji-xml-rpc-api"),
        PULL_JOB_TEMPLATE("pull-job"),
        PLAINSHELL_SCRIPT_TEMPLATE("shell-script"),
        TIMEOUTSHELL_SCRIPT_TEMPLATE("timeoutedshell-script"),
        TASK_JOB_TEMPLATE("task-job"),
        TRIGGER("trigger"),
        POST_BUILD_TASK_PLUGIN("post-build-task-plugin"),
        POST_BUILD_TASK_PLUGIN_ANALYSE("post-build-task-plugin-analyse"),
        POST_BUILD_TASK_PLUGIN_DESTROYVM("post-build-task-plugin-destroyvm"),
        VIEW("view"),
        NESTED_VIEW("nested-view"),
        VIEW_DEFAULT_COLUMNS("view-columns"),
        NESTEDVIEW_DEFAULT_COLUMNS("nested-view-columns"),
        UPDATE_VM_JOB("update-vm-job");

        private final String value;

        JenkinsTemplate(final String template) {
            this.value = Paths.get(JENKINS_TEMPLATES, template + ".xml").toString();
        }

        public String getValue() {
            return value;
        }
    }
}
