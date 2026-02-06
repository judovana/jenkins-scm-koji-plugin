package hudson.plugins.scm.koji.model;

import java.util.List;

public class KojiBuildDownloadResult implements java.io.Serializable {

    private final Build build;
    private final String rpmsDirectory;
    private final List<String> rpmFiles;

    public KojiBuildDownloadResult(Build build, String rpmsDirectory, List<String> rpmFiles) {
        this.build = build;
        this.rpmsDirectory = rpmsDirectory;
        this.rpmFiles = rpmFiles;
    }

    public boolean isEmpty() {
        return build == null || rpmFiles == null || rpmFiles.isEmpty();
    }

    public Build getBuild() {
        return build;
    }

    public String getRpmsDirectory() {
        return rpmsDirectory;
    }

    public List<String> getRpmFiles() {
        return rpmFiles;
    }

}
