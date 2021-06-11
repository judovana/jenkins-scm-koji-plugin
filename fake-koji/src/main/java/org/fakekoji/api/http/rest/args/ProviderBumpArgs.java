package org.fakekoji.api.http.rest.args;

import org.fakekoji.api.http.rest.BumperAPI;
import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProviderBumpArgs extends BumpArgs {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    public final String originalProvider;
    public final String targetProvider;
    public final List<Project> projects;
    public final Pattern filter;

    public ProviderBumpArgs(Map<String, List<String>> paramsMap, AccessibleSettings settings) throws StorageException {
        super(BumpArgs.parseBumpArgs(paramsMap).getValue());
        if (paramsMap.get(BumperAPI.PROJECTS) == null || paramsMap.get(BumperAPI.PROJECTS).isEmpty() ||
                paramsMap.get(BumperAPI.PROJECTS).contains("")) {
            throw new RuntimeException("projects is mandatory. is empty or missing");
        }
        if (paramsMap.get(BumperAPI.FILTER) == null || paramsMap.get(BumperAPI.FILTER).size() != 1 ||
                paramsMap.get(BumperAPI.FILTER).contains("")) {
            throw new RuntimeException("filter is mandatory, and exactly one");
        }
        filter = Pattern.compile(paramsMap.get(BumperAPI.FILTER).get(0));
        checkProvider(paramsMap.get(BumperAPI.BUMP_FROM), settings.getConfigManager().platformManager.readAll());
        checkProvider(paramsMap.get(BumperAPI.BUMP_TO), settings.getConfigManager().platformManager.readAll());


        //we already knows that each list is exactly one item long, and that one is full
        originalProvider = paramsMap.get(BumperAPI.BUMP_FROM).get(0);
        targetProvider = paramsMap.get(BumperAPI.BUMP_TO).get(0);
        if (originalProvider.equals(targetProvider)) {
            throw new RuntimeException(BumperAPI.BUMP_FROM + "/" + BumperAPI.BUMP_TO + " must be different");
        }
        List<Project> projects = new ArrayList<>();
        List<JDKProject> ps = settings.getConfigManager().jdkProjectManager.readAll();
        List<JDKTestProject> tps = settings.getConfigManager().jdkTestProjectManager.readAll();
        List<Project> allProjects = new ArrayList(ps.size() + tps.size());
        allProjects.addAll(ps);
        allProjects.addAll(tps);
        for (String project : paramsMap.get(BumperAPI.PROJECTS)) {
            boolean found = false;
            for (Project p : allProjects) {
                if (p.getId().equals(project)) {
                    projects.add(p);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException(project + " do not exists, use anys from: " + allProjects.stream().map(a -> a.getId()).collect(Collectors.joining(",")));
            }
        }
        this.projects= Collections.unmodifiableList(projects);
    }


    private static void checkProvider(List<String> provider, List<Platform> platforms) {
        if (provider == null || provider.size() != 1 ||
                provider.get(0).trim().equals("")) {
            throw new RuntimeException(BumperAPI.BUMP_FROM + "/" + BumperAPI.BUMP_TO + " must have exactly one value");
        }
        Set<String> providers = new HashSet<>();
        for (Platform platform : platforms) {
            for (Platform.Provider pp : platform.getProviders()) {
                if (provider.get(0).equals(pp.getId())) {
                    return;
                }
                providers.add(pp.getId());
            }
        }

        throw new RuntimeException(BumperAPI.BUMP_FROM + "/" + BumperAPI.BUMP_TO + " must be known provider: " + String.join(",", providers));
    }
}
