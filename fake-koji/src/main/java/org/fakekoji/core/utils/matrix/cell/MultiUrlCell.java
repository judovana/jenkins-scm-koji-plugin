package org.fakekoji.core.utils.matrix.cell;

import java.util.List;
import java.util.Objects;

public class MultiUrlCell extends TitleCell {

    private final List<String> urls;

    public MultiUrlCell(final String title, final List<String> urls) {
        super(title);
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiUrlCell)) return false;
        MultiUrlCell that = (MultiUrlCell) o;
        return Objects.equals(urls, that.urls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urls);
    }

    @Override
    public String toString() {
        return getTitle() + " " + urls;
    }
}
