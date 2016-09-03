package hudson.plugins.scm.koji;

import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;
import java.net.URL;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiScmDescriptor extends SCMDescriptor<KojiSCM> {

    private static final Logger LOG = LoggerFactory.getLogger(SCMDescriptor.class);
    private boolean KojiSCMConfigExists = true;

    public KojiScmDescriptor() {
        super(KojiSCM.class, KojiRepositoryBrowser.class);
        try {
            //this may be killing tests
            load();
        } catch (Exception ex) {
            LOG.error(ex.toString());
            ex.printStackTrace();
        }
    }

    @Override
    public String getDisplayName() {
        return "Koji";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        this.KojiSCMConfigExists = json.getBoolean("KojiSCMConfig");
        LOG.info("KojiSCMConfig configured to " + KojiSCMConfigExists);
        save();
        return true;
    }

    public boolean getKojiSCMConfig() {
        LOG.info("KojiSCMConfig returning " + KojiSCMConfigExists);
        return KojiSCMConfigExists;
    }

    @DataBoundSetter
    public void setKojiSCMConfig(boolean kojiSCMConfig) {
        // TODO implement complex refreshing logic
        LOG.info("KojiSCMConfig set from" + KojiSCMConfigExists + " to " + kojiSCMConfig);
        this.KojiSCMConfigExists = kojiSCMConfig;
    }

    private static final String InvalidUrl = "At least one of the urls is invalid URL";

    public FormValidation doCheckKojiTopUrl(@QueryParameter String values) {
        return testableKojiTopUrl(values);
    }

    public FormValidation doCheckKojiDownloadUrl(@QueryParameter String values) {
        return testableKojiDownloadUrl(values);
    }

    public static FormValidation testableKojiDownloadUrl(String values) {
        try {
            for (String value : KojiScmConfig.testableSplitter(values)) {
                URL url = new URL(KojiScmConfig.replaceDPORT(value));
                if (!"http".equals(url.getProtocol())) {
                    return FormValidation.error("Only http protocol is supported");
                }
            }
            return FormValidation.ok();
        } catch (Exception ignore) {
        }
        return FormValidation.error(InvalidUrl);
    }

    public static FormValidation testableKojiTopUrl(String values) {
        try {
            for (String value : KojiScmConfig.testableSplitter(values)) {
                new URL(KojiScmConfig.replaceXPORT(value));
            }
            return FormValidation.ok();
        } catch (Exception ignore) {
        }
        return FormValidation.error(InvalidUrl);
    }

}
