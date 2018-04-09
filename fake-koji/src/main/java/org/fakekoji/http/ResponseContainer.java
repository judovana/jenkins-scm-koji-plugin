package org.fakekoji.http;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

class ResponseContainer {

    private static final String DELIMITER = " ";

    interface Response {
        String respond();
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
        public String respond() {
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
        public String respond() {
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
        public String respond() {
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
        public String respond() {
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
        public String respond() {
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
        public String respond() {
            return projectMapping.getProjectOfNvra(nvra);
        }

        @Override
        public String help() {
            return "Returns project of NVRA, 1 argument required - NVRA (name-version-release-architecture)";
        }
    }

    static class GetHelpResponse implements Response {

        private final TreeMap<String, Response> treeMap;

        GetHelpResponse(TreeMap<String, Response> treeMap) {
            this.treeMap = treeMap;
        }

        @Override
        public String respond() {
            StringBuilder sb = new StringBuilder();
            Set<String> keys = treeMap.keySet();
            keys.forEach((key) -> {
                sb.append(key);
                String help = treeMap.get(key).help();
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