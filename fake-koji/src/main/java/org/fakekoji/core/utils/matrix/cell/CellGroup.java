package org.fakekoji.core.utils.matrix.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CellGroup implements Cell {

    private final List<TitleCell> cells;

    public CellGroup() {
        cells = new ArrayList<>();
    }

    public CellGroup(final List<TitleCell> cells) {
        this.cells = cells;
    }

    public void add(final TitleCell cell) {
        cells.add(cell);
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    @Override
    public int getSpan() {
        return cells.stream().map(Cell::getSpan).reduce(0, Integer::sum);
    }

    public int size() {
        return cells.size();
    }

    public List<TitleCell> getCells() {
        return cells;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellGroup)) return false;
        CellGroup cellGroup = (CellGroup) o;
        return Objects.equals(cells, cellGroup.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells);
    }
}
