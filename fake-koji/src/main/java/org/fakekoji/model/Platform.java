package org.fakekoji.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Platform implements  Comparable<Platform> {

    private final String id;
    private final String os;
    private final String version;
    private final String versionNumber;
    private final String architecture;
    private final String kojiArch;
    private final List<Provider> providers;
    private final String vmName;
    private final TestStableYZupdates testingYstream;
    private final TestStableYZupdates stableZstream;
    private final List<String> tags;
    private final List<OToolVariable> variables;

    public Platform() {
        id = null;
        vmName = null;
        os = null;
        version = null;
        versionNumber = null;
        architecture = null;
        kojiArch = null;
        providers = null;
        tags = null;
        variables = null;
        testingYstream = null;
        stableZstream = null;
    }

    public Platform(
            String id,
            String os,
            String version,
            String versionNumber,
            String architecture,
            String kojiArch,
            List<Provider> providers,
            String vmName,
            TestStableYZupdates testingYstream,
            TestStableYZupdates stableZstream,
            List<String> tags,
            List<OToolVariable> variables
    ) {
        this.id = id;
        this.os = os;
        this.version = version;
        this.versionNumber = versionNumber;
        this.architecture = architecture;
        this.kojiArch = kojiArch;
        this.providers = providers;
        this.vmName = vmName;
        this.tags = tags;
        this.testingYstream = testingYstream;
        this.stableZstream = stableZstream;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getArchitecture() {
        return architecture;
    }

    public Optional<String> getKojiArch() {
        return Optional.ofNullable(kojiArch);
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public String getVmName() {
        return vmName;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<OToolVariable> getVariables() {
        return variables;
    }

    public TestStableYZupdates getStableZstream() {
        return stableZstream;
    }

    public TestStableYZupdates getTestingYstream() {
        return testingYstream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Platform)) return false;
        Platform platform = (Platform) o;
        return Objects.equals(id, platform.id) &&
                Objects.equals(os, platform.os) &&
                Objects.equals(version, platform.version) &&
                Objects.equals(versionNumber, platform.versionNumber) &&
                Objects.equals(architecture, platform.architecture) &&
                Objects.equals(kojiArch, platform.kojiArch) &&
                Objects.equals(providers, platform.providers) &&
                Objects.equals(vmName, platform.vmName) &&
                Objects.equals(tags, platform.tags) &&
                Objects.equals(stableZstream, platform.stableZstream) &&
                Objects.equals(testingYstream, platform.testingYstream) &&
                Objects.equals(variables, platform.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                os,
                version,
                versionNumber,
                architecture,
                kojiArch,
                providers,
                vmName,
                tags,
                stableZstream,
                testingYstream,
                variables
        );
    }

    @Override
    public String toString() {
        return toString("");
    }
    public String toString(String tail) {
        return "Platform{" + tail +
                "os='" + os + '\'' + tail +
                ", version='" + version + '\'' + tail +
                ", versionNumber='" + versionNumber + '\'' + tail +
                ", architecture='" + architecture + '\'' + tail +
                ", kojiArch='" + getKojiArch() + '\'' + tail +
                ", providers='" + providers.stream().map(provider -> provider.toString(tail)).collect(Collectors.joining("\n")) + '\'' + tail +
                ", vmName='" + vmName + '\'' + tail +
                ", tags=" + tags + tail +
                ", stableZstream=" + stableZstream + tail +
                ", testingYstream=" + testingYstream + tail +
                ", variables=" + variables + tail +
                '}';
    }

    public String toOsVar() {
        return getOs() + '.' + getVersion();
    }

    @Override
    public int compareTo(@NotNull Platform platform) {
        return this.getId().compareTo(platform.getId());
    }

    public void addZstreamVar(List<OToolVariable> defaultVariables) {
        if (getStableZstream() != null && !getStableZstream().isNaN()){
            defaultVariables.add(getStableZstream().toVar("zstream"));
        }
    }

    public void addYstreamVar(List<OToolVariable> defaultVariables) {
        if (getTestingYstream() != null && !getTestingYstream().isNaN()){
            defaultVariables.add(getTestingYstream().toVar("ystream"));
        }
    }

    public static class Provider implements Comparable<Provider>{


        private final String id;
        private final List<String> hwNodes;
        private final List<String> vmNodes;

        public Provider() {
            id = null;
            hwNodes = null;
            vmNodes = null;
        }

        public Provider(
                String id,
                List<String> hwNodes,
                List<String> vmNodes
        ) {
            this.id = id;
            this.hwNodes = hwNodes;
            this.vmNodes = vmNodes;
        }

        public String getId() {
            return id;
        }

        public List<String> getHwNodes() {
            return hwNodes;
        }

        public List<String> getVmNodes() {
            return vmNodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Provider)) return false;
            Provider provider = (Provider) o;
            return Objects.equals(id, provider.id) &&
                    Objects.equals(hwNodes, provider.hwNodes) &&
                    Objects.equals(vmNodes, provider.vmNodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, hwNodes, vmNodes);
        }

        @Override
        public String toString() {
            return toString("");
        }

        public String toString(String tail) {
            return "Provider{" + tail +
                    "id='" + id + '\'' + tail +
                    ", hwNodes=" + hwNodes + tail +
                    ", vmNodes=" + vmNodes + tail +
                    '}';
        }

        @Override
        public int compareTo(@NotNull Provider provider) {
            return id.compareTo(provider.id);
        }
    }

    public enum TestStableYZupdates {
        NaN("Nan"),
        True("True"),
        False("False");

        private final String value;

        TestStableYZupdates(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isNaN(){
            return this.equals(NaN);
        }

        public OToolVariable toVar(String name) {
            switch (this){
                case True:
                    return new OToolVariable(name, "true");
                case False:
                    return new OToolVariable(name, "false");
            }
            throw new RuntimeException("Unexportable value: "+this);
        }
    }

    public static Platform create(Platform platform) {
        return new Platform(
                platform.os + platform.version + '.' + platform.getArchitecture(),
                platform.os,
                platform.version,
                platform.versionNumber,
                platform.architecture,
                platform.kojiArch,
                platform.providers,
                platform.vmName,
                platform.testingYstream,
                platform.stableZstream,
                platform.tags,
                platform.variables
        );
    }
}

