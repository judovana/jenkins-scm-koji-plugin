package org.fakekoji.http;

public class ProjectMappingExceptions {

    public static class ProjectMappingException extends Exception {

        ProjectMappingException(String message) {
            super(message);
        }
        
        public ProjectMappingException(Exception e) {
            super(e);
        }
    }

    static class ProjectsNotFoundException extends ProjectMappingException {

        ProjectsNotFoundException() {
            super("No project found");
        }
    }

    static class ProductsNotFoundException extends ProjectMappingException {

        ProductsNotFoundException() {
            super("No product found");
        }
    }

    static class ProjectDoesNotMatchException extends ProjectMappingException {

        ProjectDoesNotMatchException() {
            super("Project does not match any available project");
        }
    }

    static class ProductDoesNotMatchException extends ProjectMappingException {

        ProductDoesNotMatchException() {
            super("Product does not match any available product");
        }
    }

    static class BadRequestException extends ProjectMappingException {

        BadRequestException() {
            super("Bad request, use get?help to list values");
        }
    }

    static class ConfigFileNotFoundException extends ProjectMappingException {

        ConfigFileNotFoundException() {
            super("Config file not found");
        }
    }

    static class InvalidConfigFileException extends ProjectMappingException {

        InvalidConfigFileException() {
            super("Invalid config file");
        }
    }
}