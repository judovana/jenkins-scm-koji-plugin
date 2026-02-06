package hudson.plugins.scm.koji;

import hudson.plugins.scm.koji.model.Build;
import hudson.scm.SCMRevisionState;

public class KojiRevisionState extends SCMRevisionState {

    private final Build build;

    public KojiRevisionState(Build build) {
        this.build = build;
    }

    public Build getBuild() {
        return build;
    }

    @Override
    public String toString() {
        return "KojiRevisionState[nvr=" + build + "]";
    }

}
