package org.fakekoji.http;

import org.fakekoji.xmlrpc.server.core.FakeBuild;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.fakekoji.http.ProjectMappingExceptions.*;

public class ProjectMapping {

    private final AccessibleSettings settings;

    ProjectMapping(AccessibleSettings settings) {
        this.settings = settings;
    }

    public List<String> getAllProducts() throws ProjectMappingException {
        List<String> products = Arrays.stream(Objects.requireNonNull(settings.getDbFileRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
        if (products.isEmpty()) {
            throw new ProductsNotFoundException();
        }
        return products;
    }

    public List<String> getAllProjects() throws ProjectMappingException {
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            throw new ProjectsNotFoundException();
        }
        return projects;
    }

    public List<String> getProjectsOfProduct(String productName) throws ProjectMappingException {
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

    public String getProjectOfNvra(String nvra) throws ProjectMappingException {
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

    public String getProductOfNvra(String nvra) throws ProjectMappingException {
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

    public String getProductOfProject(String project) throws ProjectMappingException {
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

    public List<String> getExpectedArchesOfProject(String project) throws ProjectMappingException {
        File expectedArchesFile = null;
        for (File file : Objects.requireNonNull(getProjectFile(project).listFiles())) {
            if (file.getName().equals(FakeBuild.archesConfigFileName)) {
                expectedArchesFile = file;
                break;
            }
        }
        if (expectedArchesFile == null) {
            throw new ConfigFileNotFoundException();
        }
        String[] arches;
        try {
            arches = FakeBuild.readArchesFile(expectedArchesFile);
        } catch (IOException e) {
            throw new InvalidConfigFileException();
        }
        if (arches == null) {
            throw new InvalidConfigFileException();
        }
        return Arrays.asList(arches);
    }

    public List<String> getExpectedArchesOfNVR(String nvr) throws ProjectMappingException{
        return getExpectedArchesOfProject(getProjectOfNvra(nvr));
    }

    private File getProjectFile(String projectName) throws ProjectMappingException {
        File projectFile = null;
        for (File file : Objects.requireNonNull(settings.getLocalReposRoot().listFiles())) {
            if (file.getName().equals(projectName)) {
                projectFile = file;
            }
        }
        if (projectFile == null) {
            throw new ProjectDoesNotMatchException();
        }
        return projectFile;
    }
}