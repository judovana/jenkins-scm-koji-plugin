package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.TitleCell;

public class HtmlSpanningFormatter extends HtmlFormatter {

    public HtmlSpanningFormatter(boolean expandNames, String[] projects) {
        super(expandNames, projects);
    }

    @Override
    public String edge(final TitleCell cell, final int span) {
        return renderTableCell(cell.getTitle(), span);
    }

    String renderSpanningCells(
            final CellGroup cellGroup,
            final int span,
            final String rowTitle,
            final String colTitle
    ) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(renderCells(cellGroup, rowTitle, colTitle));
        if (cellGroup.size() == span) {
            return stringBuilder.toString();
        }
        stringBuilder.append(cellGroup.isEmpty() ? renderTableCell("0") : renderTableCell());
        for (int i = 1; i < span - cellGroup.size(); i++) {
            stringBuilder.append(renderTableCell());
        }
        return stringBuilder.toString();
    }

    @Override
    String cell(final String cellTitle, final String content) {
        return renderTableCell(content);
    }

    @Override
    public String cells(final CellGroup cellGroup, int maxInColumn, final String rowTitle, final String colTitle) {
        return renderSpanningCells(cellGroup, maxInColumn, rowTitle, colTitle);
    }
}
