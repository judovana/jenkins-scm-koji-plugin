package org.fakekoji.model;

import java.util.List;
import java.util.Objects;

public class Task {

    private final String id;
    private final String script;
    private final Type type;
    private final MachinePreference machinePreference;
    private final Limitation<String> productLimitation;
    private final Limitation<String> platformLimitation;
    private final FileRequirements fileRequirements;
    private final String xmlTemplate;
    private final RpmLimitation rpmLimitation;

    public Task() {
        id = null;
        script = null;
        type = null;
        machinePreference = null;
        productLimitation = null;
        platformLimitation = null;
        fileRequirements = null;
        xmlTemplate = null;
        rpmLimitation = null;
    }

    public Task(
            String id,
            String script,
            Type type,
            MachinePreference machinePreference,
            Limitation<String> productLimitation,
            Limitation<String> platformLimitation,
            FileRequirements fileRequirements,
            String xmlTemplate,
            RpmLimitation rpmLimitation
    ) {
        this.id = id;
        this.script = script;
        this.type = type;
        this.machinePreference = machinePreference;
        this.productLimitation = productLimitation;
        this.platformLimitation = platformLimitation;
        this.fileRequirements = fileRequirements;
        this.xmlTemplate = xmlTemplate;
        this.rpmLimitation = rpmLimitation;
    }

    public String getId() {
        return id;
    }

    public String getScript() {
        return script;
    }

    public Type getType() {
        return type;
    }

    public MachinePreference getMachinePreference() {
        return machinePreference;
    }

    public Limitation<String> getProductLimitation() {
        return productLimitation;
    }

    public Limitation<String> getPlatformLimitation() {
        return platformLimitation;
    }

    public FileRequirements getFileRequirements() {
        return fileRequirements;
    }

    public String getXmlTemplate() {
        return xmlTemplate;
    }

    public RpmLimitation getRpmLimitation() {
        return rpmLimitation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
                Objects.equals(script, task.script) &&
                Objects.equals(type, task.type) &&
                machinePreference == task.machinePreference &&
                Objects.equals(productLimitation, task.productLimitation) &&
                Objects.equals(platformLimitation, task.platformLimitation) &&
                Objects.equals(fileRequirements, task.fileRequirements) &&
                Objects.equals(xmlTemplate, task.xmlTemplate) &&
                Objects.equals(rpmLimitation, task.rpmLimitation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, script, type, machinePreference, productLimitation, platformLimitation, fileRequirements, xmlTemplate, rpmLimitation);
    }

    public static class FileRequirements {

        private final boolean source;
        private final BinaryRequirements binary;

        public FileRequirements() {
            source = false;
            binary = null;
        }

        public FileRequirements(
                boolean source,
                BinaryRequirements binary
        ) {
            this.source = source;
            this.binary = binary;
        }

        public boolean isSource() {
            return source;
        }

        public BinaryRequirements getBinary() {
            return binary;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FileRequirements)) return false;
            FileRequirements that = (FileRequirements) o;
            return source == that.source &&
                    binary == that.binary;
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, binary);
        }
    }

    public static class Limitation<T> {

        private final List<T> list;
        private final LimitFlag flag;

        public Limitation() {
            list = null;
            flag = null;
        }

        public Limitation(List<T> list, LimitFlag flag) {
            this.list = list;
            this.flag = flag;
        }

        public List<T> getList() {
            return list;
        }

        public LimitFlag getFlag() {
            return flag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Limitation)) return false;
            Limitation<?> limitation = (Limitation<?>) o;
            return Objects.equals(list, limitation.getList()) &&
                    flag == limitation.getFlag();
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, flag);
        }
    }

    public static class RpmLimitation {

        private final String glob;
        private final LimitFlag flag;

        public RpmLimitation() {
            glob = null;
            flag = null;
        }

        public RpmLimitation(String glob, LimitFlag flag) {
            this.glob = glob;
            this.flag = flag;
        }

        public String getGlob() {
            return glob;
        }

        public LimitFlag getFlag() {
            return flag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RpmLimitation)) return false;
            RpmLimitation rpmLimitation = (RpmLimitation) o;
            return Objects.equals(glob, rpmLimitation.glob) &&
                    flag == rpmLimitation.flag;
        }

        @Override
        public int hashCode() {
            return Objects.hash(glob, flag);
        }
    }

    public enum LimitFlag {
        NONE("NONE"),
        WHITELIST("WHITELIST"),
        BLACKLIST("BLACKLIST");

        private final String value;

        LimitFlag(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum MachinePreference {
        VM("VM"),
        VM_ONLY("VM_ONLY"),
        HW("HW"),
        HW_ONLY("HW_ONLY");

        private final String value;

        MachinePreference(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum BinaryRequirements {
        NONE("NONE"),
        BINARY("BINARY"),
        BINARIES("BINARIES");

        private final String value;

        BinaryRequirements(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Type {
        BUILD("BUILD", 0),
        TEST("TEST", 1);

        private final String value;
        private final int order;

        Type(String value, int order) {
            this.value = value;
            this.order = order;
        }

        public String getValue() {
            return value;
        }

        public int getOrder() {
            return order;
        }
    }
}
