package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.Arrays;

public class GetBuildList implements XmlRpcRequestParams {

    private final String projectName;
    private final String[] buildVariants;
    private final boolean isBuilt;

    public GetBuildList(
            String projectName,
            String[] buildVariants,
            boolean isBuilt
    ) {
        this.projectName = projectName;
        this.buildVariants = buildVariants;
        this.isBuilt = isBuilt;
    }

    @Override
    public String toString() {
        return projectName + "; " + Arrays.toString(buildVariants) + "; " + isBuilt;
    }

    @Override
    public Object toObject() {
        return this;
    }

    @Override
    public String getMethodName() {
        return Constants.getBuildList;
    }

    public String getProjectName() {
        return projectName;
    }

    public String[] getBuildVariants() {
        return buildVariants;
    }

    /**
     * true = if the built is already finished
     * false = the built is about to be build
     *
     * @return
     */
    public boolean isBuilt() {
        return isBuilt;
    }

    public boolean isSupposedToGetBuild() {
        return !isBuilt;
    }

    public static GetBuildList create(Object object) {
        return (GetBuildList) object;
    }
}
