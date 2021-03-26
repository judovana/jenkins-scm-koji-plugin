package org.fakekoji.jobmanager.views;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VersionlessPlatform implements CharSequence, Comparable<VersionlessPlatform> {
    private final String os;
    private final String arch;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionlessPlatform that = (VersionlessPlatform) o;
        return Objects.equals(os, that.os) &&
                Objects.equals(arch, that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(os, arch);
    }

    public VersionlessPlatform(String os, String arch) {
        this.os = os;
        this.arch = arch;
    }

    public String getOs() {
        return os;
    }

    public String getArch() {
        return arch;
    }

    public String getId() {
        return os + JenkinsViewTemplateBuilderFactory.getMinorDelimiter() + arch;
    }

    @Override
    public int length() {
        return getId().length();
    }

    @Override
    public char charAt(int index) {
        return getId().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getId().subSequence(start, end);
    }

    @Override
    public int compareTo(@NotNull VersionlessPlatform o) {
        return getId().compareTo(o.getId());
    }
}
