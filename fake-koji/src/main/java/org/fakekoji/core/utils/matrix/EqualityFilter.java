package org.fakekoji.core.utils.matrix;

abstract class EqualityFilter {

    public final boolean os;
    public final boolean arch;
    public final boolean provider;
    public final boolean variants;

    public EqualityFilter(boolean os, boolean arch, boolean provider, boolean variants) {
        this.os = os;
        this.arch = arch;
        this.provider = provider;
        this.variants = variants;
    }
}
