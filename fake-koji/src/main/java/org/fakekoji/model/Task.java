package org.fakekoji.model;

import org.fakekoji.functional.Result;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Task implements  Comparable<Task> {

    private final String id;
    private final String script;
    private final Type type;
    private final String scmPollSchedule;
    private final MachinePreference machinePreference;
    private final Limitation<String> productLimitation;
    private final Limitation<String> platformLimitation;
    private final FileRequirements fileRequirements;
    private final String xmlTemplate;
    private final String xmlViewTemplate;
    private final RpmLimitation rpmLimitation;
    private final List<OToolVariable> variables;
    private final Integer timeoutInHours;

    public Task() {
        id = null;
        script = null;
        type = null;
        scmPollSchedule = null;
        machinePreference = null;
        productLimitation = null;
        platformLimitation = null;
        fileRequirements = null;
        xmlTemplate = null;
        xmlViewTemplate = null;
        rpmLimitation = null;
        variables = null;
        timeoutInHours = null;
    }

    public Task(
            String id,
            String script,
            Type type,
            String scmPollSchedule,
            MachinePreference machinePreference,
            Limitation<String> productLimitation,
            Limitation<String> platformLimitation,
            FileRequirements fileRequirements,
            String xmlTemplate,
            String xmlViewTemplate,
            RpmLimitation rpmLimitation,
            List<OToolVariable> variables,
            Integer timeoutInHours
    ) {
        this.id = id;
        this.script = script;
        this.type = type;
        this.scmPollSchedule = scmPollSchedule;
        this.machinePreference = machinePreference;
        this.productLimitation = productLimitation;
        this.platformLimitation = platformLimitation;
        this.fileRequirements = fileRequirements;
        this.xmlTemplate = xmlTemplate;
        this.xmlViewTemplate = xmlViewTemplate;
        this.rpmLimitation = rpmLimitation;
        this.variables = variables;
        this.timeoutInHours = timeoutInHours;
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

    public String getScmPollSchedule() {
        return scmPollSchedule;
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

    public String getXmlViewTemplate() {
        return xmlViewTemplate;
    }

    public RpmLimitation getRpmLimitation() {
        return rpmLimitation;
    }

    public List<OToolVariable> getVariables() {
        return variables;
    }

    public static Optional<String> getViewColumnsAsOptional(Task t) {
        if (t.xmlViewTemplate == null || t.xmlViewTemplate.isBlank()){
            return Optional.empty();
        } else {
            return Optional.of(t.xmlViewTemplate);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
                Objects.equals(script, task.script) &&
                type == task.type &&
                Objects.equals(scmPollSchedule, task.scmPollSchedule) &&
                machinePreference == task.machinePreference &&
                Objects.equals(productLimitation, task.productLimitation) &&
                Objects.equals(platformLimitation, task.platformLimitation) &&
                Objects.equals(fileRequirements, task.fileRequirements) &&
                Objects.equals(xmlTemplate, task.xmlTemplate) &&
                Objects.equals(xmlViewTemplate, task.xmlViewTemplate) &&
                Objects.equals(rpmLimitation, task.rpmLimitation) &&
                Objects.equals(variables, task.variables) &&
                Objects.equals(timeoutInHours, task.timeoutInHours);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                script,
                type,
                scmPollSchedule,
                machinePreference,
                productLimitation,
                platformLimitation,
                fileRequirements,
                xmlTemplate,
                xmlViewTemplate,
                rpmLimitation,
                variables,
                timeoutInHours
        );
    }

    @Override
    public int compareTo(@NotNull Task task) {
        return this.getId().compareTo(task.getId());
    }

    public int getTimeoutInHours() {
        if (timeoutInHours == null) {
            return 0;
        } else {
            return timeoutInHours;
        }
    }

    public static class FileRequirements {

        private final boolean source;
        private final boolean noarch;
        private final BinaryRequirements binary;

        public FileRequirements() {
            source = false;
            noarch = false;
            binary = null;
        }

        public FileRequirements(boolean source, boolean noarch, BinaryRequirements binary
        ) {
            this.source = source;
            this.noarch = noarch;
            this.binary = binary;
        }

        public boolean isSource() {
            return source;
        }

        public boolean isNoarch() {
            return noarch;
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
                    noarch == that.noarch &&
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

        private final List<String> denylist;
        private final List<String> allowlist;

        public RpmLimitation() {
            denylist = null;
            allowlist = null;
        }

        public RpmLimitation(List<String> denylist, List<String> allowlist) {
            this.denylist = denylist;
            this.allowlist = allowlist;
        }

        public List<String> getDenylist() {
            return denylist;
        }

        public List<String> getAllowlist() {
            return allowlist;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RpmLimitation)) return false;
            RpmLimitation that = (RpmLimitation) o;
            return Objects.equals(denylist, that.denylist) &&
                    Objects.equals(allowlist, that.allowlist);
        }

        @Override
        public int hashCode() {
            return Objects.hash(denylist, allowlist);
        }

        @Override
        public String toString() {
            return "RpmLimitation{" +
                    "subpackageDenylist=" + denylist +
                    ", subpackageAllowlist=" + allowlist +
                    '}';
        }
    }

    public enum LimitFlag {
        NONE(""),
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
        NONE(""),
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

        public static Result<Type, String> parse(final String value) {
            try {
                return Result.ok(Type.valueOf(value));
            } catch (IllegalArgumentException e) {
                return Result.err("Invalid task type: " + value + ". Valid options are: " + BUILD + " and " + TEST + ".");
            }
        }
    }
}
