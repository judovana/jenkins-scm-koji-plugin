package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface TableFormatter {


    String MASTER = AccessibleSettings.master.baseUrl;
    String JENKINS_PORT = "8080"; //FIXME made accessible

    String cellStart(int span);

    String cellStart();

    String cellEnd();

    String rowStart();

    String rowEnd();

    String tableStart();

    String tableEnd();

    String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn);

    String initialCell(String[] project);

    String lastCell(int total, int i);


    public static class PlainTextTableFormatter implements TableFormatter {

        public String cellStart(int span) {
            return "";
        }

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
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return "" + l.size();
        }


    }

    public static class HtmlTableFormatter implements TableFormatter {

        private static class DummyLeaf extends MatrixGenerator.Leaf {

            public DummyLeaf(String s) {
                super(s);
            }
        }

        public String cellStart(int span) {
            return cellStart();
        }

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
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return getContextImpl(l, false, maxInColumn);
        }

        protected String getContextImpl(List<MatrixGenerator.Leaf> origL, boolean td, int maxInColumn) {
            List<MatrixGenerator.Leaf> l = new ArrayList<>(maxInColumn);
            l.addAll(origL);
            if (l.isEmpty() && !td) {
                return "0";
            }
            if (td) {
                while (l.size() < maxInColumn) {
                    if (l.size() == 0) {
                        l.add(new DummyLeaf("0"));
                    } else {
                        l.add(new DummyLeaf(""));
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                MatrixGenerator.Leaf leaf = l.get(i);
                String url = leaf.toString();
                String tdopen = "";
                String tdclose = "";
                if (i == 0) {
                    if (td) {
                        tdclose = "</td>";
                    }
                } else if (i == l.size() - 1) {
                    if (td) {
                        tdopen = "<td>";
                    }
                } else {
                    if (td) {
                        tdopen = "<td>";
                        tdclose = "</td>";
                    }
                }
                if (leaf instanceof DummyLeaf) {
                    sb.append(tdopen + leaf.toString() + tdclose);
                } else
                    try {
                        new URL(url);
                        sb.append(tdopen + openAdd(url) + "<a href=\"" + url + "\">").append("[" + (i + 1) + "]").append("</a>" + closeAdd() + tdclose);
                    } catch (MalformedURLException ex) {
                        sb.append(tdopen + openAdd(url) + "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/job/").append(url.toString()).append("\">").append("[" + (i + 1) + "]").append("</a>" + closeAdd() + tdclose);
                    }
            }
            return sb.toString();
        }

        protected String openAdd(String job) {
            return "";
        }

        protected String closeAdd() {
            return "";
        }

        @Override
        public String initialCell(String[] project) {
            if (project == null || project.length == 0) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/\">all projects</a>";
            }
            if (project.length == 1) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~" + project[0] + "\">" + project[0] + "</a>";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < project.length; i++) {
                    String leaf = project[i];
                    sb.append("<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~").append(leaf).append("\">").append("[" + (i + 1) + "]").append("</a>");
                }
                return sb.toString();
            }
        }

        @Override
        public String lastCell(int found, int all) {
            return found + "/" + all;
        }
    }

    public static class SpanningHtmlTableFormatter extends HtmlTableFormatter {

        @Override
        public String cellStart(int span) {
            return "<td colspan=\"" + span + "\">";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return getContextImpl(l, true, maxInColumn);
        }
    }
}
