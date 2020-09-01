package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.TitleCell;
import org.fakekoji.core.utils.matrix.cell.UpperCornerCell;

public class PlainTextFormatter implements Formatter {

    @Override
    public String upperCorner(final UpperCornerCell cell) {
        if (cell.projectCells().size() > 1) {
            return cell.projectCells().size() + " projects";
        }
        return cell.projectCells().get(0).getTitle();
    }

    @Override
    public String lowerCorner(int found, int all) {
        return found + "/" + all;
    }

    @Override
    public String rowStart() {
        return "";
    }

    @Override
    public String rowEnd() {
        return "\n";
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
    public String edge(final TitleCell titleCell, final int span) {
        return titleCell.getTitle();
    }

    @Override
    public String cells(final CellGroup cellGroup, int maxInColumn, final String rowTitle, final String colTitle) {
        return "" + cellGroup.size();
    }
}
