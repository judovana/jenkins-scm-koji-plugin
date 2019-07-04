package hudson.plugins.scm.koji;

import java.io.Serializable;

public enum KojiXmlRpcApiType implements Serializable {

    REAL_KOJI("realKoji"),
    FAKE_KOJI("fakeKoji");

    private final String name;

    KojiXmlRpcApiType(final String xmlRpcApiName) {
        name = xmlRpcApiName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public static KojiXmlRpcApiType getType(final String value) {
        for (KojiXmlRpcApiType type : KojiXmlRpcApiType.values()) {
            if (type.getName().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type (" + value + ") requested for "
                + KojiXmlRpcApiType.class.getName());
    }
}
