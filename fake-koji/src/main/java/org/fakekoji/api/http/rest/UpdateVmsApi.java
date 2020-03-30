package org.fakekoji.api.http.rest;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.JenkinsUpdateVmTemplateBuilder;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdateVmsApi {

    @NotNull
    static  List<JenkinsUpdateVmTemplateBuilder> getJenkinsUpdateVmTemplateBuilders(AccessibleSettings settings, PlatformManager platformManager) throws StorageException, IOException {
        List<Platform> platforms = platformManager.readAll();
        List<JenkinsUpdateVmTemplateBuilder> vagrantVmUpdates = new ArrayList<>(platforms.size());
        List<JenkinsUpdateVmTemplateBuilder> vagrantHwUpdates = new ArrayList<>(platforms.size());
        for (Platform platform : platforms) {
            for (Platform.Provider provider : platform.getProviders()) {
                //todo, make configurable?
                String currentlyOnlyUpdateAbleProvidr = "vagrant";
                if (provider.getId().equals(currentlyOnlyUpdateAbleProvidr)) {
                    for (String vmProvider : provider.getVmNodes()) {
                        String name = "update-" + platform.getId() + "(" + vmProvider + ")";
                        JenkinsUpdateVmTemplateBuilder juvt = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate(
                                name, vmProvider, currentlyOnlyUpdateAbleProvidr, settings.getScriptsRoot(), platform.getVmName());
                        vagrantVmUpdates.add(juvt);
                    }
                    for (String hwProvider : provider.getHwNodes()) {
                        String name = "update-" + hwProvider + "(" + platform.getId() + ")";
                        JenkinsUpdateVmTemplateBuilder juvt = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate(
                                name, hwProvider, currentlyOnlyUpdateAbleProvidr, settings.getScriptsRoot(), "local");
                        vagrantHwUpdates.add(juvt);
                    }
                }
            }
        }
        List<JenkinsUpdateVmTemplateBuilder> allUpdates = new ArrayList<>(vagrantVmUpdates.size() + vagrantHwUpdates.size());
        allUpdates.addAll(vagrantVmUpdates);
        allUpdates.addAll(vagrantHwUpdates);
        return allUpdates;
    }
}
