package org.fakekoji.api.http.rest;

import io.javalin.http.Context;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.JenkinsUpdateVmTemplateBuilder;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UpdateVmsApi {

    private final Pattern filter;
    private final boolean onlyHw;
    private final boolean onlyVM;

    UpdateVmsApi(Context context) {
        this.filter = Pattern.compile(context.queryParam(OToolService.FILTER) == null ? ".*": context.queryParam(OToolService.FILTER));
        this.onlyHw = OToolService.notNullBoolean(context, OToolService.ONLY_HW, false);
        this.onlyVM = OToolService.notNullBoolean(context, OToolService.ONLY_VM, false);
    }

    @NotNull
    List<JenkinsUpdateVmTemplateBuilder> getJenkinsUpdateVmTemplateBuilders(AccessibleSettings settings, PlatformManager platformManager) throws StorageException, IOException {
        if (onlyHw && onlyVM){
            return new ArrayList<>();
        }
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
                        if (filter.matcher(name).matches()) {
                            JenkinsUpdateVmTemplateBuilder juvt = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate(
                                    name, vmProvider, currentlyOnlyUpdateAbleProvidr, settings.getScriptsRoot(), platform.getVmName());
                            vagrantVmUpdates.add(juvt);
                        }
                    }
                    for (String hwProvider : provider.getHwNodes()) {
                        String name = "update-" + hwProvider + "(" + platform.getId() + ")";
                        if (filter.matcher(name).matches()) {
                            JenkinsUpdateVmTemplateBuilder juvt = JenkinsUpdateVmTemplateBuilder.getUpdateTemplate(
                                    name, hwProvider, currentlyOnlyUpdateAbleProvidr, settings.getScriptsRoot(), "local");
                            vagrantHwUpdates.add(juvt);
                        }
                    }
                }
            }
        }
        if (onlyVM){
            return vagrantVmUpdates;
        }
        if (onlyHw){
            return vagrantHwUpdates;
        }
        List<JenkinsUpdateVmTemplateBuilder> allUpdates = new ArrayList<>(vagrantVmUpdates.size() + vagrantHwUpdates.size());
        allUpdates.addAll(vagrantVmUpdates);
        allUpdates.addAll(vagrantHwUpdates);
        return allUpdates;
    }

    public String getXmls(List<JenkinsUpdateVmTemplateBuilder> allUpdates ) {
        StringBuilder list = new StringBuilder();
        for (JenkinsUpdateVmTemplateBuilder update : allUpdates) {
            list.append(" #### " + update.getName() + " ####\n");
            list.append(update.expand() + "\n");
        }
        return list.toString();
    }

    public String getList(List<JenkinsUpdateVmTemplateBuilder> allUpdates) {
        return String.join("\n", allUpdates);
    }
}
