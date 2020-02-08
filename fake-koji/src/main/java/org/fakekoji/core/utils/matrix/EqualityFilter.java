package org.fakekoji.core.utils.matrix;

public class EqualityFilter {

    public final boolean os;
    public final boolean arch;
    public final boolean provider;
    public final boolean suiteOrProject;
    public final boolean variants;

    public EqualityFilter(boolean os, boolean arch, boolean provider, boolean suiteOrProject, boolean variants) {
        this.os = os;
        this.arch = arch;
        this.provider = provider;
        this.suiteOrProject = suiteOrProject;
        this.variants = variants;
    }
}
