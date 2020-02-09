package org.fakekoji.core.utils.matrix;

public class BuildEqualityFilter extends EqualityFilter {

    public final boolean project;
    public final boolean jdk;


    public BuildEqualityFilter(boolean os, boolean arch, boolean provider, boolean project, boolean jdk, boolean variants) {
        super(os, arch, provider, variants);
        this.project = project;
        this.jdk = jdk;
    }
}
