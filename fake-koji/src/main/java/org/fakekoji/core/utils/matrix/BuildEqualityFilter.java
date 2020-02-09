package org.fakekoji.core.utils.matrix;

public class BuildEqualityFilter extends EqualityFilter {

    public final boolean project;


    public BuildEqualityFilter(boolean os, boolean arch, boolean provider, boolean project, boolean variants) {
        super(os, arch, provider, variants);
        this.project = project;
    }
}
