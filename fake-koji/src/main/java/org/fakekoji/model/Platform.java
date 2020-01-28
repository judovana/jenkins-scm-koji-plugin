package org.fakekoji.model;

import java.util.List;
import java.util.Objects;

public class Platform {

    private final String id;
    private final String os;
    private final String version;
    private final String architecture;
    private final String provider;
    private final String vmName;
    private final List<String> vmNodes;
    private final List<String> hwNodes;
    private final List<String> tags;

    public Platform() {
        id = null;
        vmName = null;
        os = null;
        version = null;
        architecture = null;
        provider = null;
        vmNodes = null;
        hwNodes = null;
        tags = null;
    }

    public Platform(
            String os,
            String version,
            String architecture,
            String provider,
            String vmName,
            List<String> vmNodes,
            List<String> hwNodes,
            List<String> tags
    ) {
        this.os = os;
        this.version = version;
        this.architecture = architecture;
        this.provider = provider;
        this.vmName = vmName;
        this.vmNodes = vmNodes;
        this.hwNodes = hwNodes;
        this.tags = tags;
        id = assembleId();
    }

    public Platform(
            String id,
            String os,
            String version,
            String architecture,
            String provider,
            String vmName,
            List<String> vmNodes,
            List<String> hwNodes,
            List<String> tags
    ) {
        this.id = id;
        this.os = os;
        this.version = version;
        this.architecture = architecture;
        this.provider = provider;
        this.vmName = vmName;
        this.vmNodes = vmNodes;
        this.hwNodes = hwNodes;
        this.tags = tags;
    }

    private String assembleId() {
        return assembleString() + '.' + getProvider();
    }

    public String getId() {
        return id;
    }

    public String assembleString() {
        return getOs() + getVersion() + '.' + getArchitecture();
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getProvider() {
        return provider;
    }

    public String getVmName() {
        return vmName;
    }

    public List<String> getVmNodes() {
        return vmNodes;
    }

    public List<String> getHwNodes() {
        return hwNodes;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Platform)) return false;
        Platform platform = (Platform) o;
        return Objects.equals(os, platform.os) &&
                Objects.equals(version, platform.version) &&
                Objects.equals(architecture, platform.architecture) &&
                Objects.equals(provider, platform.provider) &&
                Objects.equals(vmName, platform.vmName) &&
                Objects.equals(vmNodes, platform.vmNodes) &&
                Objects.equals(hwNodes, platform.hwNodes) &&
                Objects.equals(tags, platform.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(os, version, architecture, provider, vmName, vmNodes, hwNodes, tags);
    }

    @Override
    public String toString() {
        return "Platform{" +
                "os='" + os + '\'' +
                ", version='" + version + '\'' +
                ", architecture='" + architecture + '\'' +
                ", provider='" + provider + '\'' +
                ", vmName='" + vmName + '\'' +
                ", vmNodes=" + vmNodes +
                ", hwNodes=" + hwNodes +
                ", tags=" + tags +
                '}';
    }

    public String toOsVar() {
        return getOs() + '.' + getVersion();
    }
}

