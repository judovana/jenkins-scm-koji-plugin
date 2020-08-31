package org.fakekoji.core.utils.matrix.cell;

import java.util.Objects;
import java.util.Optional;

public class UrlCell extends TitleCell {
    private final String url;

    public UrlCell(final String title) {
        super(title);
        url = null;
    }

    public UrlCell(final String title, final String url) {
        super(title);
        this.url = url;
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlCell)) return false;
        if (!super.equals(o)) return false;
        UrlCell urlCell = (UrlCell) o;
        return Objects.equals(url, urlCell.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url);
    }

    @Override
    public String toString() {
        return getUrl().orElse(getTitle());
    }
}
