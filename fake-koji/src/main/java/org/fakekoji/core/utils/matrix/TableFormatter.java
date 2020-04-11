package org.fakekoji.core.utils.matrix;

import java.util.List;

public interface TableFormatter {


    String cellStart();

    String cellEnd();

    String rowStart();

    String rowEnd();

    String tableStart();

    String tableEnd();

    String getContext(List<MatrixGenerator.Leaf> l);


    public static class PlainTextTableFormatter implements TableFormatter {


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
            for (int i = 0; i < l.size(); i++){
                MatrixGenerator.Leaf leaf = l.get(i);
                sb.append("<a href=\"http://hydra.brq.redhat.com:8080/job/").append(leaf.toString()).append("\" a>").append("("+(i+1)+")").append("</a>");
            }
            return sb.toString();
        }


    }
}
