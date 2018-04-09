package org.fakekoji.http;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class ProjectMapping {

    private final AccessibleSettings settings;


    ProjectMapping(AccessibleSettings settings) {
        this.settings = settings;
    }

    List<String> getAllProducts() {
        return Arrays.stream(Objects.requireNonNull(settings.getDbFileRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    List<String> getAllProjects() {
        return Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    List<String> getProjectsOfProduct(String productName) {
        if (!getAllProducts().contains(productName)) {
            return null;
        }
        return Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(file -> file.getName().contains(productName))
                .map(File::getName)
                .collect(Collectors.toList());
    }

    String getProjectOfNvra(String nvra) {
        List<String> projectList = getAllProjects();
        projectList.sort((String s1, String s2) -> s2.length() - s1.length());
        for (String project : projectList) {
            String[] dashSplit = project.split("-");
            boolean match = true;
            for (String str : dashSplit) {
                if (!nvra.contains(str)) {
                    match = false;
                }
            }
            if (match) {
                return project;
            }
        }
        return null;
    }

    String getProductOfNvra(String nvra) {
        List<String> productList = getAllProducts();
        for (String product : productList) {
            if (nvra.contains(product)) {
                return product;
            }
        }
        return null;
    }

    String getProductOfProject(String project) {
        List<String> productList = getAllProducts();
        for (String product : productList) {
            if (project.contains(product)) {
                return product;
            }
        }
        return null;
    }
}