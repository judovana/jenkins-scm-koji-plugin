package org.fakekoji.api.http.rest.args;

import org.fakekoji.api.http.rest.BumperAPI;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
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

public class VariantBumpArgs extends BumpArgs {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    public final String originalVariant;
    public final String targetVariant;
    public final List<Project> projects;
    public final Pattern filter;

    public VariantBumpArgs(Map<String, List<String>> paramsMap, AccessibleSettings settings) throws StorageException {
        super(BumpArgs.parseBumpArgs(paramsMap).getValue());
        if (paramsMap.get(BumperAPI.PROJECTS) == null || paramsMap.get(BumperAPI.PROJECTS).isEmpty() ||
                paramsMap.get(BumperAPI.PROJECTS).contains("")) {
            throw new RuntimeException("projects is mandatory. is empty or missing. You have " +
                    settings.getConfigManager().jdkProjectManager.readAll().stream().map(a->a.getId()).collect(Collectors.joining(",")) +
                    " and " +
                    settings.getConfigManager().jdkTestProjectManager.readAll().stream().map(a->a.getId()).collect(Collectors.joining(",")));
        }
        if (paramsMap.get(BumperAPI.FILTER) == null || paramsMap.get(BumperAPI.FILTER).size() != 1 ||
                paramsMap.get(BumperAPI.FILTER).contains("")) {
            throw new RuntimeException("filter is mandatory, and exactly one");
        }
        filter = Pattern.compile(paramsMap.get(BumperAPI.FILTER).get(0));
        String fromGroup = checkVariant(paramsMap.get(BumperAPI.BUMP_FROM), settings.getConfigManager().taskVariantManager.readAll());
        String toGroup = checkVariant(paramsMap.get(BumperAPI.BUMP_TO), settings.getConfigManager().taskVariantManager.readAll());

        if (!fromGroup.equals(toGroup)) {
            throw new RuntimeException("from/to variants must be from same group. Are not: " + fromGroup + "/" + toGroup);
        }

        //we already knows that each list is exactly one item long, and that one is full
        originalVariant = paramsMap.get(BumperAPI.BUMP_FROM).get(0);
        targetVariant = paramsMap.get(BumperAPI.BUMP_TO).get(0);
        if (originalVariant.equals(targetVariant)) {
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
                throw new RuntimeException(project + " do not exists, use any from: " + allProjects.stream().map(a -> a.getId()).collect(Collectors.joining(",")));
            }
        }
        this.projects = Collections.unmodifiableList(projects);
    }


    private static String checkVariant(List<String> provider, List<TaskVariant> variants) {
        if (provider == null || provider.size() != 1 ||
                provider.get(0).trim().equals("")) {
            throw new RuntimeException(BumperAPI.BUMP_FROM + "/" + BumperAPI.BUMP_TO + " must have exactly one value");
        }
        String checkedVaraint = provider.get(0);
        Set<String> parents = new HashSet<>();
        for (TaskVariant variant : variants) {
            String variantId = variant.getId();
            for (Map.Entry<String, TaskVariantValue> pp : variant.getVariants().entrySet()) {
                String key = pp.getKey(); //variantId?
                TaskVariantValue value = pp.getValue();
                if (value.getId().equals(checkedVaraint)) {
                    parents.add(variantId);
                }
            }
        }
        if (parents.size() != 1) {
            throw new RuntimeException(BumperAPI.BUMP_FROM + "/" + BumperAPI.BUMP_TO + " must be known variant, and from exactly one group. : [" + String.join(",", parents) + "]");
        }
        return parents.toArray(new String[1])[0];
    }
}
