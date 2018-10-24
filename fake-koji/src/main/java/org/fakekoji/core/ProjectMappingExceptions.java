package org.fakekoji.core;

import java.io.File;
import java.io.IOException;

public class ProjectMappingExceptions {

    public static class ProjectMappingException extends Exception {

        public ProjectMappingException(String message) {
            super(message);
        }

        public ProjectMappingException(Exception e) {
            super(e);
        }
        
        public ProjectMappingException(String message, Exception e) {
            super(message, e);
        }
    }

    static class ProjectsNotFoundException extends ProjectMappingException {

        public ProjectsNotFoundException(File projectFile) {
            super("No projects found in " + projectFile.getAbsolutePath());
        }

        public ProjectsNotFoundException(String productName) {
            super("No projects of " + productName + " found");
        }
    }

    static class ProductsNotFoundException extends ProjectMappingException {

        public ProductsNotFoundException(File productFile) {
            super("No products found in " + productFile.getAbsolutePath());
        }
    }

    static class ProductNotFoundException extends ProjectMappingException {

        public ProductNotFoundException(String productName) {
            super("Product " + productName + " not found");
        }
    }

    static class ProductOfProjectNotFoundException extends ProjectMappingException {

        public ProductOfProjectNotFoundException(String projectName) {
            super("Product of " + projectName + " not found");
        }
    }

    static class ProjectNotFoundException extends ProjectMappingException {

        public ProjectNotFoundException(String projectName) {
            super("Project " + projectName + " not found");
        }
    }

    static class BadRequestException extends ProjectMappingException {

        public BadRequestException() {
            super("Bad request, use get?help to list values");
        }
    }

    static class ProductOfNvraNotFoundException extends ProjectMappingException {

        public ProductOfNvraNotFoundException(String nvra) {
            super("Product of " + nvra + " not found");
        }
    }

    static class ProjectOfNvraNotFoundException extends ProjectMappingException {

        public ProjectOfNvraNotFoundException(String nvra) {
            super("Project of" + nvra + " not found");
        }
    }

    static class ConfigFileNotFoundException extends ProjectMappingException {

        public ConfigFileNotFoundException(File projectFile) {
            super("File " + FakeBuild.archesConfigFileName + " not found in " + projectFile.getAbsolutePath());
        }
    }

    static class InvalidConfigFileException extends ProjectMappingException {

        public InvalidConfigFileException() {
            super("Couldn\'t read the expected architectures from " + FakeBuild.archesConfigFileName + " file");
        }

        public InvalidConfigFileException(IOException e) {
            super("Exception occured while reading from " + FakeBuild.archesConfigFileName + " file", e);
        }
    }
}