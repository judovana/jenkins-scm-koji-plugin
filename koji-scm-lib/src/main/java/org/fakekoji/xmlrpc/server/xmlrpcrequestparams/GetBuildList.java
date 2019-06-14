package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

public class GetBuildList implements XmlRpcRequestParams {

    private final String projectName;
    private final String jvm;
    private final String buildVariant;
    private final boolean isBuilt;

    public GetBuildList(
            String projectName,
            String jvm,
            String buildVariant,
            boolean isBuilt
    ) {
        this.projectName = projectName;
        this.jvm = jvm;
        this.buildVariant = buildVariant;
        this.isBuilt = isBuilt;
    }

    @Override
    public String toString() {
        return projectName + "; " + jvm + "; " + buildVariant + "; " + isBuilt;
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

    public String getJvm() {
        return jvm;
    }

    public String getBuildVariant() {
        return buildVariant;
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
