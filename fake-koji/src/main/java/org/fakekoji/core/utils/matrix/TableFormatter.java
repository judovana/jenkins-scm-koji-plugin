package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public interface TableFormatter {


    String MASTER = AccessibleSettings.master.baseUrl;
    String JENKINS_PORT = "8080"; //FIXME made accessible

    String cellStart();

    String cellEnd();

    String rowStart();

    String rowEnd();

    String tableStart();

    String tableEnd();

    String getContext(List<MatrixGenerator.Leaf> l);

    String initialCell(String[] project);

    String lastCell(int total, int i);


    public static class PlainTextTableFormatter implements TableFormatter {

        @Override
        public String initialCell(String[] project) {
            if (project == null || project.length == 0) {
                return "all projects";
            }
            if (project.length == 1) {
                return project[0];
            } else {
                return project.length + " projects";
            }
        }

        @Override
        public String lastCell(int found, int all) {
            return found + "/" + all;
        }

        @Override
        public String cellStart() {
            return "";
        }

        @Override
        public String cellEnd() {
            return "";
        }

        @Override
        public String rowStart() {
            return "";
        }

        @Override
        public String rowEnd() {
            return "";
        }

        @Override
        public String tableStart() {
            return "";
        }

        @Override
        public String tableEnd() {
            return "";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l) {
            return "" + l.size();
        }


    }

    public static class HtmlTableFormatter implements TableFormatter {

        @Override
        public String cellStart() {
            return "<td>";
        }

        @Override
        public String cellEnd() {
            return "</td>";
        }

        @Override
        public String rowStart() {
            return "<tr>";
        }

        @Override
        public String rowEnd() {
            return "</tr>";
        }

        @Override
        public String tableStart() {
            return "<table>";
        }

        @Override
        public String tableEnd() {
            return "</table>";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l) {
            if (l.isEmpty()) {
                return "0";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                MatrixGenerator.Leaf leaf = l.get(i);
                String url = leaf.toString();
                try {
                    new URL(url);
                    sb.append("<a href=\"" + url + "\" a>").append("(" + (i + 1) + ")").append("</a>");
                } catch (MalformedURLException ex) {
                    sb.append("<a href=\"" + MASTER + ":" + JENKINS_PORT + "/job/").append(url.toString()).append("\" a>").append("(" + (i + 1) + ")").append("</a>");
                }
            }
            return sb.toString();
        }

        @Override
        public String initialCell(String[] project) {
            if (project == null || project.length == 0) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/\" a>all projects</a>";
            }
            if (project.length == 1) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~" + project[0] + "\" a>" + project[0] + "</a>";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < project.length; i++) {
                    String leaf = project[i];
                    sb.append("<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~").append(leaf).append("\" a>").append("(" + (i + 1) + ")").append("</a>");
                }
                return sb.toString();
            }
        }

        @Override
        public String lastCell(int found, int all) {
            return found + "/" + all;
        }


    }
}
