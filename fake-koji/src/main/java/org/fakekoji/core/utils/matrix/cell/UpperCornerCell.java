package org.fakekoji.core.utils.matrix.cell;

import java.util.List;
import java.util.Objects;

public class UpperCornerCell implements Cell {

    private final List<UrlCell> projectCells;

    public UpperCornerCell(final List<UrlCell> projectCells) {
        this.projectCells = projectCells;
    }

    public List<UrlCell> projectCells() {
        return this.projectCells;
    }

    @Override
    public int getSpan() {
        return projectCells.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpperCornerCell)) return false;
        UpperCornerCell that = (UpperCornerCell) o;
        return Objects.equals(projectCells, that.projectCells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectCells);
    }
}
