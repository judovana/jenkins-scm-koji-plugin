package org.fakekoji.http;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.fakekoji.http.ProjectMappingExceptions.*;

class ProjectMapping {

    private final AccessibleSettings settings;


    ProjectMapping(AccessibleSettings settings) {
        this.settings = settings;
    }

    List<String> getAllProducts() throws ProjectMappingException {
        List<String> products = Arrays.stream(Objects.requireNonNull(settings.getDbFileRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
        if (products.isEmpty()) {
            throw new ProductsNotFoundException();
        }
        return products;
    }

    List<String> getAllProjects() throws ProjectMappingException {
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            throw new ProjectsNotFoundException();
        }
        return projects;
    }

    List<String> getProjectsOfProduct(String productName) throws ProjectMappingException {
        if (!getAllProducts().contains(productName)) {
            throw new ProductDoesNotMatchException();
        }
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(file -> file.getName().contains(productName))
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            throw new ProjectsNotFoundException();
        }
        return projects;
    }

    String getProjectOfNvra(String nvra) throws ProjectMappingException {
        return getProjectOfNvra(nvra, getAllProjects());
    }

    String getProjectOfNvra(String nvra, List<String> projectList) throws ProjectMappingException {
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
        throw new ProjectDoesNotMatchException();
    }

    String getProductOfNvra(String nvra) throws ProjectMappingException {
        return getProductOfNvra(nvra, getAllProducts());
    }

    String getProductOfNvra(String nvra, List<String> productList) throws ProjectMappingException {
        for (String product : productList) {
            if (nvra.contains(product)) {
                return product;
            }
        }
        throw new ProductDoesNotMatchException();
    }

    String getProductOfProject(String project) throws ProjectMappingException {
        return getProductOfProject(project, getAllProducts(), getAllProjects());
    }

    String getProductOfProject(String project, List<String> productList, List<String> projectList) throws ProjectMappingException {
        if (!projectList.contains(project)) {
            throw new ProjectDoesNotMatchException();
        }
        for (String product : productList) {
            if (project.contains(product)) {
                return product;
            }
        }
        throw new ProjectDoesNotMatchException();
    }
}