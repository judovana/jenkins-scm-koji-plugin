package hudson.plugins.scm.koji;

import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;
import java.net.URL;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class KojiScmDescriptor extends SCMDescriptor<KojiSCM> {

    public KojiScmDescriptor() {
        super(KojiSCM.class, KojiRepositoryBrowser.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "Koji";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        // TODO implement
        save();
        return true;
    }

    public FormValidation doCheckKojiTopUrl(@QueryParameter String value) {
        try {
            new URL(value);
            return FormValidation.ok();
        } catch (Exception ignore) {
        }
        return FormValidation.error("Invalid URL");
    }

    public FormValidation doCheckKojiDownloadUrl(@QueryParameter String value) {
        try {
            URL url = new URL(value);
            if (!"http".equals(url.getProtocol())) {
                return FormValidation.error("Only http protocol is supported");
            }
            return FormValidation.ok();
        } catch (Exception ignore) {
        }
        return FormValidation.error("Invalid URL");
    }

}
