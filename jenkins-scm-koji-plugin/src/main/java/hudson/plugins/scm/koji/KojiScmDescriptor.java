package hudson.plugins.scm.koji;

import hudson.scm.SCMDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiScmDescriptor extends SCMDescriptor<KojiSCM> {

    private static final Logger LOG = LoggerFactory.getLogger(SCMDescriptor.class);
    private boolean KojiSCMConfig = true;

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
        this.KojiSCMConfig = json.getBoolean("KojiSCMConfig");
        LOG.info("KojiSCMConfig configured to " + KojiSCMConfig);
        save();
        return true;
    }

    public boolean getKojiSCMConfig() {
        LOG.info("KojiSCMConfig returning " + KojiSCMConfig);
        return KojiSCMConfig;
    }

    @DataBoundSetter
    public void setKojiSCMConfig(boolean kojiSCMConfig) {
        // TODO implement complex refreshing logic
        LOG.info("KojiSCMConfig set from" + KojiSCMConfig + " to " + kojiSCMConfig);
        this.KojiSCMConfig = kojiSCMConfig;
    }
}
