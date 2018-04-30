package org.fakekoji.http;

import java.util.List;
import java.util.Map;
import java.util.Set;

class ResponseContainer {

    private static final String DELIMITER = " ";

    interface Response {
        String respond() throws ProjectMappingExceptions.ProjectMappingException;
        String help();
    }

    static class GetPortResponse implements Response {

        private final int port;
        private final String help;

        GetPortResponse(int port) {
            this.port = port;
            help = null;
        }

        GetPortResponse(int port, String help) {
            this.port = port;
            this.help = help;
        }

        @Override
        public String respond() {
            return String.valueOf(port);
        }

        @Override
        public String help() {
            return help;
        }
    }

    static class GetPathResponse implements Response {

        private final String path;
        private final String help;

        GetPathResponse(String path) {
            this.path = path;
            this.help = null;
        }

        GetPathResponse(String path, String help) {
            this.path = path;
            this.help = help;
        }

        @Override
        public String respond() {
            return path;
        }

        @Override
        public String help() {
            return help;
        }
    }

    static class GetAllProductsResponse implements Response {

        private final ProjectMapping projectMapping;

        GetAllProductsResponse(ProjectMapping projectMapping) {
            this.projectMapping = projectMapping;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            return String.join(DELIMITER, projectMapping.getAllProducts());
        }

        @Override
        public String help() {
            return "Returns list of all available products";
        }
    }

    static class GetAllProjectsResponse implements Response {

        private final ProjectMapping projectMapping;

        GetAllProjectsResponse(ProjectMapping projectMapping) {
            this.projectMapping = projectMapping;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            return String.join(DELIMITER, projectMapping.getAllProjects());
        }

        @Override
        public String help() {
            return "Returns list of all available projects";
        }
    }

    static class GetProjectsOfProductResponse implements Response {

        private final ProjectMapping projectMapping;
        private String productName;

        GetProjectsOfProductResponse(ProjectMapping projectMapping, String productName) {
            this.projectMapping = projectMapping;
            this.productName = productName;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            List<String> list = projectMapping.getProjectsOfProduct(productName);
            return list == null ? null : String.join(DELIMITER, list);
        }

        @Override
        public String help() {
            return "Returns list of projects of a product, 1 argument required - product (use command allProducts to see the options)";
        }
    }

    static class GetProductOfProjectResponse implements Response {

        private final ProjectMapping projectMapping;
        private final String projectName;

        GetProductOfProjectResponse(ProjectMapping projectMapping, String projectName) {
            this.projectMapping = projectMapping;
            this.projectName = projectName;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            return projectMapping.getProductOfProject(projectName);
        }

        @Override
        public String help() {
            return "Returns product of a project, 1 argument required - project (use command allProjects to see the options)";
        }
    }

    static class GetProductOfNvraResponse implements Response {

        private final ProjectMapping projectMapping;
        private final String nvra;

        GetProductOfNvraResponse(ProjectMapping projectMapping, String nvra) {
            this.projectMapping = projectMapping;
            this.nvra = nvra;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            return projectMapping.getProductOfNvra(nvra);
        }

        @Override
        public String help() {
            return "Returns product of a NVRA, 1 argument required - NVRA (name-version-release-architecture)";
        }
    }

    static class GetProjectOfNvraResponse implements Response {

        private final ProjectMapping projectMapping;
        private final String nvra;

        GetProjectOfNvraResponse(ProjectMapping projectMapping, String nvra) {
            this.projectMapping = projectMapping;
            this.nvra = nvra;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            return projectMapping.getProjectOfNvra(nvra);
        }

        @Override
        public String help() {
            return "Returns project of NVRA, 1 argument required - NVRA (name-version-release-architecture)";
        }
    }

    static class GetExpectedArchesOfProjectResponse implements Response {

        private final ProjectMapping projectMapping;
        private final String projectName;

        GetExpectedArchesOfProjectResponse(ProjectMapping projectMapping, String projectName) {
            this.projectMapping = projectMapping;
            this.projectName = projectName;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            List<String> list = projectMapping.getExpectedArchesOfProject(projectName);
            return list == null ? null : String.join(DELIMITER, list);
        }

        @Override
        public String help() {
            return "Returns list of arches the project is supposed to be built on, 1 argument required - project ( use command allProjects to see the options)";
        }
    }

    static class GetExpectedArchesOfNVR implements Response {

        private final ProjectMapping projectMapping;
        private final String nvr;

        GetExpectedArchesOfNVR(ProjectMapping projectMapping, String nvr) {
            this.projectMapping = projectMapping;
            this.nvr = nvr;
        }

        @Override
        public String respond() throws ProjectMappingExceptions.ProjectMappingException {
            List<String> list = projectMapping.getExpectedArchesOfNVR(nvr);
            return list == null ? null : String.join(DELIMITER, list);
        }

        @Override
        public String help() {
            return "Returns list of arches the project of NVR is supposed to be built on, 1 argument required - NVR (name-version-release";
        }
    }

    static class GetHelpResponse implements Response {

        private final Map<String, Response> responseMap;

        GetHelpResponse(Map<String, Response> responseMap) {
            this.responseMap = responseMap;
        }

        @Override
        public String respond() {
            StringBuilder sb = new StringBuilder();
            Set<String> keys = responseMap.keySet();
            keys.forEach((key) -> {
                sb.append(key);
                String help = responseMap.get(key).help();
                if (help != null) {
                    sb.append(": ").append(help);
                }
                sb.append('\n');
            });

            return sb.toString();
        }

        @Override
        public String help() {
            return "Displays all possible commands and their meaning and required arguments";
        }
    }

}