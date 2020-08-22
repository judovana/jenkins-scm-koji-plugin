package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.util.Objects;

import hudson.plugins.scm.koji.Constants;

public class GetBuildList implements XmlRpcRequestParams {

    private final String projectName;
    private final String buildVariants;
    private final String platforms;
    private final boolean isBuilt;

    public GetBuildList(
            String projectName,
            String buildVariants,
            String platforms,
            boolean isBuilt
    ) {
        this.projectName = projectName;
        this.buildVariants = buildVariants;
        this.platforms = platforms;
        this.isBuilt = isBuilt;
    }

    @Override
    public String toString() {
        return projectName + "; " + buildVariants + "; " + isBuilt;
    }

    @Override
    public Object[] toXmlRpcParams() {
        return new Object[]{this};
    }

    @Override
    public String getMethodName() {
        return Constants.getBuildList;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getBuildVariants() {
        return buildVariants;
    }

    public String getPlatforms() {
        return platforms;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildList that = (GetBuildList) o;
        return isBuilt == that.isBuilt &&
                Objects.equals(projectName, that.projectName) &&
                Objects.equals(buildVariants, that.buildVariants) &&
                Objects.equals(platforms, that.platforms) &&
                Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), projectName, buildVariants, platforms, isBuilt);
    }
}
