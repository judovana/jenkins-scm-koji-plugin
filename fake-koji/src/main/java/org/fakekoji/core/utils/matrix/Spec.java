package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.model.Platform;
import org.fakekoji.model.TaskVariantValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class Spec {

    protected final Platform platform;
    protected final Platform.Provider provider;
    protected final List<String> variants;
    protected final EqualityFilter viewFilter;

    public Platform getPlatform() {
        return platform;
    }

    public Platform.Provider getProvider() {
        return provider;
    }

    public List<String> getVariants() {
        return Collections.unmodifiableList(variants);
    }


    public Spec(Platform platform, Platform.Provider provider, EqualityFilter viewFilter) {
        this.platform = platform;
        this.provider = provider;
        this.viewFilter = viewFilter;
        this.variants = new ArrayList<>();
    }

    public void addVariant(TaskVariantValue v) {
        variants.add(v.getId());
    }

    protected String getVariantsString() {
        if (variants.size() > 0) {
            if (!viewFilter.variants) {
                return "-?";
            } else {
                return "-" + String.join(".", variants);
            }
        } else {
            return "";
        }
    }

    public abstract String toString();

    public boolean matchOs(String os) {
        return !viewFilter.os || (platform.getOs() + platform.getVersion()).equals(os);
    }

    public boolean matchArch(String a) {
        return !viewFilter.arch || platform.getArchitecture().equals(a);
    }

    public boolean matchVars(Collection<String> vars) {
        return !viewFilter.variants || matchVarsImpl(vars);
    }

    private boolean matchVarsImpl(Collection<String> vars) {
        //we are ok with subset, so iterating throughs horter list and checking its presence in longer one
        //reason is, test only jobs now have subset of variants only
        Collection<String> varsLess = vars;
        Collection<String> varsMore = variants;
        if (vars.size() > varsMore.size()) {
            varsLess = variants;
            varsMore = vars;
        }
        for (String v : varsLess) {
            if (!varsMore.contains(v)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchProvider(String p) {
        if (p == null) {
            //provider is ignored for test only jobs
            return true;
        }
        return !viewFilter.provider || provider.getId().equals(p);
    }

    protected String getPlatformString() {
        String pr = provider.getId();
        if (!viewFilter.provider) {
            pr = "?";
        }
        Platform pl = platform;
        if (!viewFilter.arch && !viewFilter.os) {
            pl = new Platform("??.?", "?","?", "?", "?", "", null, null, null, null, null, null);
        } else if (!viewFilter.arch) {
            pl = new Platform(platform.getOs()+platform.getVersion()+".?", platform.getOs(), platform.getVersion(),platform.getVersionNumber(), "?", "", null, null,null, null,  null, null);
        } else if (!viewFilter.os) {
            pl = new Platform("??."+platform.getArchitecture(), "?", "?","?",  platform.getArchitecture(), "", null, null, null, null,  null, null);
        }
        return TaskJob.getPlatformAndProviderString(pl, pr);
    }


}
