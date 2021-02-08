package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.MultiUrlCell;
import org.fakekoji.core.utils.matrix.cell.TitleCell;
import org.fakekoji.core.utils.matrix.cell.UpperCornerCell;
import org.fakekoji.core.utils.matrix.cell.UrlCell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HtmlSpanningFormatter extends HtmlFormatter {

    public HtmlSpanningFormatter(boolean expandNames, String[] projects) {
        super(expandNames, projects);
    }

    @Override
    public String upperCorner(final UpperCornerCell cell) {
        final List<UrlCell> projectCells = cell.projectCells();
        if (projectCells.isEmpty()) {
            return renderHtmlCell("-", "#");
        }
        if (projectCells.size() == 1) {
            final UrlCell projectCell = projectCells.get(0);
            return renderHtmlCell(projectCell.getTitle(), projectCell.getUrl().orElse("#"));
        }
        return projectCells.stream()
                .map(projectCell -> renderHtmlCell(projectCell.getTitle(), projectCell.getUrl().orElse("#")))
                .collect(Collectors.joining());
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
        final int size = cellGroup.getSpan();
        if (size == span) {
            return stringBuilder.toString();
        }
        stringBuilder.append(cellGroup.isEmpty() ? renderTableCell("0") : renderTableCell(""));
        for (int i = 1; i < span - size; i++) {
            stringBuilder.append(renderTableCell(""));
        }
        return stringBuilder.toString();
    }

    @Override
    String renderMultiUrlCell(final String title, final MultiUrlCell cell) {
        return cell.getUrls()
                .stream()
                .map(url -> cell(title, renderHtmlAnchor(title, url)))
                .collect(Collectors.joining());
    }

    @Override
    String cell(final String cellTitle, final String content) {
        return renderTableCell(content);
    }

    @Override
    public String cells(final CellGroup cellGroup, int maxInColumn, final String rowTitle, final String colTitle) {
        return renderSpanningCells(cellGroup, maxInColumn, rowTitle, colTitle);
    }

    @Override
    public String lowerCorner(int found, int all, int span) {
        return renderTableCell(found + "/" + all, span);
    }
}
