package hudson.plugins.scm.koji;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.io.Serializable;

public abstract class KojiXmlRpcApi implements Describable<KojiXmlRpcApi>, ExtensionPoint, Serializable {

    private static final long serialVersionUID = -1650617726812887577L;

    private final KojiXmlRpcApiType xmlRpcApiType;

    public KojiXmlRpcApi(final KojiXmlRpcApiType xmlRpcApiType) {
        this.xmlRpcApiType = xmlRpcApiType;
    }

    @Exported
    public KojiXmlRpcApiType getXmlRpcApiType() {
        return xmlRpcApiType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<KojiXmlRpcApi> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    public static class KojiXmlRpcApiDescriptor extends Descriptor<KojiXmlRpcApi> {

        private final String xmlRpcApiName;

        public KojiXmlRpcApiDescriptor(
                final Class<? extends KojiXmlRpcApi> clazz,
                final String xmlRpcApiName
        ) {
            super(clazz);
            this.xmlRpcApiName = xmlRpcApiName;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return xmlRpcApiName;
        }

        public DescriptorExtensionList<KojiXmlRpcApi, KojiXmlRpcApiDescriptor> getKojiXmlRpxApiDescriptors() {
            return Jenkins.getActiveInstance().getDescriptorList(KojiXmlRpcApi.class);
        }
    }
}
