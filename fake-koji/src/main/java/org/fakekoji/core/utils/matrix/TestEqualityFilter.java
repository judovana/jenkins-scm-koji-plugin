package org.fakekoji.core.utils.matrix;

public class TestEqualityFilter extends  EqualityFilter{

    public final boolean suite;

    public TestEqualityFilter(boolean os, boolean arch, boolean provider, boolean suite, boolean variants) {
        super(os, arch, provider, variants);
        this.suite = suite;
    }
}
