package org.fakekoji.jobmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


public class JenkinsVmUpdateTemplateBuilderTest {

    @Test
    public void basicVmTempalte() throws IOException {
        JenkinsUpdateVmTemplateBuilder template = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate("update-os.arch(node)", "node", "provider", new File("/some/path"), "os.arch");

        final String expectedTemplate = "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.scm.NullSCM\"/>\n" +
                "    <assignedNode>node</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers/>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>#!/bin/sh&#13;\n" +
                "                # set EXCLUDE_KERNEL=ture, if you wont to skip kernel update&#13;\n" +
                "                # set ALLOW_ALPHAREPO=false when you dont want to update from alpha-testing openjdkQA internal repo. Such update and testruns must be observed carefully&#13;\n" +
                "                # it is recommended to run normal update as separate step before update with ALLOW_ALPHAREPO&#13;\n" +
                "                # ALLOW_ALPHAREPO shold be run for el 8.y.z and 9.y.z only (keep it in mind&#13;\n" +
                "                export VM_J_ID=os.arch&#13;\n" +
                "                sh /some/path/jenkins/provider/updateBox.sh $VM_J_ID \\&#13;\n" +
                "                \"sudo EXCLUDE_KERNEL=false  ALLOW_ALPHAREPO=true  bash /mnt/shared/TckScripts/jenkins/provider/update/update-command.sh\"&#13;\n" +
                "                #\"sudo dnf -y install kernel-modules-extra &amp;&amp; sudo EXCLUDE_KERNEL=false  ALLOW_ALPHAREPO=false  bash /mnt/shared/TckScripts/jenkins/vagrant/update/update-command.sh\"&#13;\n" +
                "                #\"sudo dnf -y --enablerepo=rhel-8-buildroot upgrade libstdc++-static giflib-devel &amp;&amp; sudo  EXCLUDE_KERNEL=true  ALLOW_ALPHAREPO=false  bash /mnt/shared/TckScripts/jenkins/vagrant/update/update-command.sh\"&#13;\n" +
                "                #\"echo just repack\"&#13;\n" +
                "                #\"sudo dnf -y remove *photos* gnome-soft*  PackageKit  flatpak  gnome-contac* gnome-box*   gnome-w*  *abrt* *clock*\"&#13;\n" +
                "                #\"sudo yum -y  install *lcms*\"&#13;\n" +
                "                #\"sudo yum -y downgrade \\\"nss*\\\"\"&#13;\n" +
                "                #\"sudo yum -y downgrade \\\"nspr*\\\"\"&#13;\n" +
                "            </command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers>\n" +
                "        <hudson.plugins.postbuildtask.PostbuildTask plugin=\"postbuild-task@1.8\">\n" +
                "            <tasks>\n" +
                "                <hudson.plugins.postbuildtask.TaskProperties>\n" +
                "                    <logTexts>\n" +
                "                        <hudson.plugins.postbuildtask.LogProperties>\n" +
                "                            <logText>.*</logText>\n" +
                "                            <operator>OR</operator>\n" +
                "                        </hudson.plugins.postbuildtask.LogProperties>\n" +
                "                    </logTexts>\n" +
                "                    <EscalateStatus>true</EscalateStatus>\n" +
                "                    <RunIfJobSuccessful>false</RunIfJobSuccessful>\n" +
                "                    <script>#!/bin/bash&#13; bash /some/path/jenkins/provider/destroy.sh os.arch&#13;</script>\n" +
                "                </hudson.plugins.postbuildtask.TaskProperties>\n" +
                "            </tasks>\n" +
                "        </hudson.plugins.postbuildtask.PostbuildTask>\n" +
                "    </publishers>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = template.expand();

        Assertions.assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    public void basicHwTemplate() throws IOException {
        String name = "reproducers~security";
        JenkinsUpdateVmTemplateBuilder template = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate("update-machine(os.arch)", "machine", "provider", new File("/some/path"), "local");

        final String expectedTemplate = "<project>\n" +
                "    <actions/>\n" +
                "    <description/>\n" +
                "    <keepDependencies>false</keepDependencies>\n" +
                "    <properties/>\n" +
                "    <scm class=\"hudson.scm.NullSCM\"/>\n" +
                "    <assignedNode>machine</assignedNode>\n" +
                "    <canRoam>false</canRoam>\n" +
                "    <disabled>false</disabled>\n" +
                "    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "    <triggers/>\n" +
                "    <concurrentBuild>false</concurrentBuild>\n" +
                "    <builders>\n" +
                "        <hudson.tasks.Shell>\n" +
                "            <command>#!/bin/sh&#13;\n" +
                "                # set EXCLUDE_KERNEL=ture, if you wont to skip kernel update&#13;\n" +
                "                # set ALLOW_ALPHAREPO=false when you dont want to update from alpha-testing openjdkQA internal repo. Such update and testruns must be observed carefully&#13;\n" +
                "                # it is recommended to run normal update as separate step before update with ALLOW_ALPHAREPO&#13;\n" +
                "                # ALLOW_ALPHAREPO shold be run for el 8.y.z and 9.y.z only (keep it in mind&#13;\n" +
                "                export VM_J_ID=local&#13;\n" +
                "                sh /some/path/jenkins/provider/updateBox.sh $VM_J_ID \\&#13;\n" +
                "                \"sudo EXCLUDE_KERNEL=false  ALLOW_ALPHAREPO=true  bash /mnt/shared/TckScripts/jenkins/provider/update/update-command.sh\"&#13;\n" +
                "                #\"sudo dnf -y install kernel-modules-extra &amp;&amp; sudo EXCLUDE_KERNEL=false  ALLOW_ALPHAREPO=false  bash /mnt/shared/TckScripts/jenkins/vagrant/update/update-command.sh\"&#13;\n" +
                "                #\"sudo dnf -y --enablerepo=rhel-8-buildroot upgrade libstdc++-static giflib-devel &amp;&amp; sudo  EXCLUDE_KERNEL=true  ALLOW_ALPHAREPO=false  bash /mnt/shared/TckScripts/jenkins/vagrant/update/update-command.sh\"&#13;\n" +
                "                #\"echo just repack\"&#13;\n" +
                "                #\"sudo dnf -y remove *photos* gnome-soft*  PackageKit  flatpak  gnome-contac* gnome-box*   gnome-w*  *abrt* *clock*\"&#13;\n" +
                "                #\"sudo yum -y  install *lcms*\"&#13;\n" +
                "                #\"sudo yum -y downgrade \\\"nss*\\\"\"&#13;\n" +
                "                #\"sudo yum -y downgrade \\\"nspr*\\\"\"&#13;\n" +
                "            </command>\n" +
                "        </hudson.tasks.Shell>\n" +
                "    </builders>\n" +
                "    <publishers/>\n" +
                "    <buildWrappers/>\n" +
                "</project>\n";

        final String actualTemplate = template.expand();

        Assertions.assertEquals(expectedTemplate, actualTemplate);
    }

}
