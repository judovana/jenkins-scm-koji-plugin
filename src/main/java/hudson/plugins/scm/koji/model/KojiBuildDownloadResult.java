package hudson.plugins.scm.koji.model;

import java.io.File;
import java.util.List;

public class KojiBuildDownloadResult {

    private final Build build;
    private final List<File> rpmFiles;

    public KojiBuildDownloadResult(Build build, List<File> rpmFiles) {
        this.build = build;
        this.rpmFiles = rpmFiles;
    }


    public boolean isEmpty() {
        return build == null || rpmFiles == null || rpmFiles.isEmpty();
    }

    public Build getBuild() {
        return build;
    }

    public List<File> getRpmFiles() {
        return rpmFiles;
    }

}
