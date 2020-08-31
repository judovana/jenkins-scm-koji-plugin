package org.fakekoji.core.utils.matrix.cell;

import java.util.Objects;

public class TitleCell implements Cell {

    private final String title;

    public TitleCell(final String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TitleCell)) return false;
        TitleCell titleCell = (TitleCell) o;
        return Objects.equals(title, titleCell.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
