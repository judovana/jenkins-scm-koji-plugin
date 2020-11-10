package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.core.utils.matrix.cell.TitleCell;
import org.fakekoji.core.utils.matrix.cell.UpperCornerCell;

public interface Formatter {
    String rowStart();

    String rowEnd();

    String tableStart();

    String tableEnd();

    String edge(final TitleCell titleCell, int span);

    String cells(final CellGroup cellGroup, int maxInColumn, final String rowTitle, final String colTitle);

    String upperCorner(final UpperCornerCell cell);

    String lowerCorner(int total, int i, int span);
}
