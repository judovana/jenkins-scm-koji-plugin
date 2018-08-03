package org.fakekoji.http;

import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.fakekoji.xmlrpc.server.core.FakeBuild;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.fakekoji.http.ProjectMappingExceptions.*;

public class ProjectMapping {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

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
            LOGGER.severe("Couldn\'t find any product");
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
            LOGGER.severe("Couldn\'t find any project");
            throw new ProjectsNotFoundException();
        }
        return projects;
    }

    public List<String> getProjectsOfProduct(String productName) throws ProjectMappingException {
        if (!getAllProducts().contains(productName)) {
            LOGGER.severe("Couldn't find a product with name " + productName);
            throw new ProductDoesNotMatchException();
        }
        List<String> projects = Arrays.stream(Objects.requireNonNull(settings.getLocalReposRoot().listFiles()))
                .filter(file -> file.getName().contains(productName))
                .map(File::getName)
                .collect(Collectors.toList());
        if (projects.isEmpty()) {
            LOGGER.severe("Couldn't find any project of " + productName);
            throw new ProjectsNotFoundException();
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
        LOGGER.severe("Couldn\'t find project of " + nvra);
        throw new ProjectDoesNotMatchException();
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
        LOGGER.severe("Couldn\'t find the product of " + nvra);
        throw new ProductDoesNotMatchException();
    }

    public String getProductOfProject(String project) throws ProjectMappingException {
        return getProductOfProject(project, getAllProducts(), getAllProjects());
    }

    String getProductOfProject(String project, List<String> productList, List<String> projectList) throws ProjectMappingException {
        if (!projectList.contains(project)) {
            LOGGER.severe("Couldn\'t find project " + project);
            throw new ProjectDoesNotMatchException();
        }
        for (String product : productList) {
            if (project.contains(product)) {
                return product;
            }
        }
        LOGGER.severe("Couldn\'t find the product of " + project);
        throw new ProjectDoesNotMatchException();
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
            LOGGER.severe("Couldn\'t find " + FakeBuild.archesConfigFileName + " file in " + projectFile.getAbsolutePath());
            throw new ConfigFileNotFoundException();
        }
        String[] arches;
        try {
            arches = FakeBuild.readArchesFile(expectedArchesFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occured while reading from " + FakeBuild.archesConfigFileName + " file", e);
            throw new InvalidConfigFileException();
        }
        if (arches == null) {
            LOGGER.severe("Couldn\'t read the expected architectures from " + FakeBuild.archesConfigFileName + " file");
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
            LOGGER.severe("Couldn\'t find file of " + projectName + " project");
            throw new ProjectDoesNotMatchException();
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