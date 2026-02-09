package hudson.plugins.scm.koji;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.scm.koji.model.BuildProvider;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class KojiBuildProvider implements Describable<KojiBuildProvider>, Serializable {

    private final BuildProvider buildProvider;

    @DataBoundConstructor
    public KojiBuildProvider(String topUrl, String downloadUrl) {
        buildProvider = new BuildProvider(topUrl, downloadUrl);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<KojiBuildProvider> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    public BuildProvider getBuildProvider() {
        return buildProvider;
    }

    public String getTopUrl() {
        return buildProvider.getTopUrl();
    }

    public String getDownloadUrl() {
        return buildProvider.getDownloadUrl();
    }


    @Extension
    public static final class KojiBuildProviderDescriptor extends Descriptor<KojiBuildProvider> {

        private static final String SUPPORTED_PROTOCOL = "HTTP";
        private static final String URL_INVALID = "The URL is invalid";
        private static final String PROTOCOL_NOT_SUPPORTED = "Only " + SUPPORTED_PROTOCOL + " protocol is supported";

        @NonNull
        @Override
        public String getDisplayName() {
            return "Koji build provider";
        }

        FormValidation doCheckTopUrl(@QueryParameter String value) {
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error(URL_INVALID);
            }
            return FormValidation.ok();
        }

        FormValidation doCheckDownloadUrl(@QueryParameter String value) {
            try {
                final URL url = new URL(value);
                if (!url.getProtocol().equals(SUPPORTED_PROTOCOL.toLowerCase())) {
                    return FormValidation.error(PROTOCOL_NOT_SUPPORTED);
                }
            } catch (MalformedURLException e) {
                return FormValidation.error(URL_INVALID);
            }
            return FormValidation.ok();
        }
    }

    @Override
    public String toString() {
        return
                "topUrl: " + getTopUrl() + '\n' +
                "downloadUrl: " + getDownloadUrl() + '\n';
    }
}
