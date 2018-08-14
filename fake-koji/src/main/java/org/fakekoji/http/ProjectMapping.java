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
            throw new ProductsNotFoundException(settings.getDbFileRoot());
        }
        return products;
    }

    public List<String> getAllProjects() throws ProjectMappingException {
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            throw new ProjectsNotFoundException(settings.getLocalReposRoot());
        }
        return projects;
    }

    public List<String> getProjectsOfProduct(String productName) throws ProjectMappingException {
        if (!getAllProducts().contains(productName)) {
            throw new ProductNotFoundException(productName);
        }
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(file -> file.getName().contains(productName))
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            throw new ProjectsNotFoundException(productName);
        }
        return projects;
    }

    public String getProjectOfNvra(String nvra) throws ProjectMappingException {
        return getProjectOfNvra(nvra, getAllProjects());
    }

    String getProjectOfNvra(String nvra, List<String> projectList) throws ProjectMappingException {
        nvra = processUnordinaryNVR(nvra);
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
        throw new ProjectOfNvraNotFoundException(nvra);
    }

    public String getProductOfNvra(String nvra) throws ProjectMappingException {
        return getProductOfNvra(nvra, getAllProducts());
    }

    String getProductOfNvra(String nvra, List<String> productList) throws ProjectMappingException {
        nvra = processUnordinaryNVR(nvra);
        for (String product : productList) {
            if (nvra.contains(product)) {
                return product;
            }
        }
        throw new ProductOfNvraNotFoundException(nvra);
    }

    public String getProductOfProject(String project) throws ProjectMappingException {
        return getProductOfProject(project, getAllProducts(), getAllProjects());
    }

    String getProductOfProject(String project, List<String> productList, List<String> projectList) throws ProjectMappingException {
        if (!projectList.contains(project)) {
            throw new ProjectNotFoundException(project);
        }
        for (String product : productList) {
            if (project.contains(product)) {
                return product;
            }
        }
        throw new ProductOfProjectNotFoundException(project);
    }

    public List<String> getExpectedArchesOfProject(String project) throws ProjectMappingException {
        File expectedArchesFile = null;
        final File projectFile = getProjectFile(project);
        for (File file : projectFile.listFiles()) {
            if (file.getName().equals(FakeBuild.archesConfigFileName)) {
                expectedArchesFile = file;
                break;
            }
        }
        if (expectedArchesFile == null) {
            throw new ConfigFileNotFoundException(projectFile);
        }
        String[] arches;
        try {
            arches = FakeBuild.readArchesFile(expectedArchesFile);
        } catch (IOException e) {
            throw new InvalidConfigFileException(e);
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
            throw new ProjectNotFoundException(projectName);
        }
        return projectFile;
    }

    /*
    * for historic reasons, some builds don't share our naming convention and therefore we need to modify them so
    * project mapping api can determine their project or product.
    * for instance, we can't determine project/product of 'openjdk8-win-jdk8u121.b13-52.dev.upstream', so it needs
    * to be changed to 'java-1.8.0-openjdk-win-jdk8u121.b13-52.dev.upstream'
    */
    private String processUnordinaryNVR(String nvr) {
        nvr = nvr.replace("openjdk8", "java-1.8.0-openjdk");
        return nvr;
    }
}