/*
 * The MIT License
 *
 * Copyright 2016 user.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.scm.koji;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import static hudson.plugins.scm.koji.Constants.BUILD_ENV_NVR;
import static hudson.plugins.scm.koji.Constants.BUILD_ENV_RPMS_DIR;
import static hudson.plugins.scm.koji.Constants.BUILD_ENV_RPM_FILES;

public class KojiEnvVarsAction implements EnvironmentContributingAction {

    private final String nvr;
    private final String rpmsDir;
    private final String rpmFiles;

    public KojiEnvVarsAction(String nvr, String rpmsDir, String rpmFiles) {
        this.nvr = nvr;
        this.rpmsDir = rpmsDir;
        this.rpmFiles = rpmFiles;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.put(BUILD_ENV_NVR, nvr);
        env.put(BUILD_ENV_RPMS_DIR, rpmsDir);
        env.put(BUILD_ENV_RPM_FILES, rpmFiles);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

}
