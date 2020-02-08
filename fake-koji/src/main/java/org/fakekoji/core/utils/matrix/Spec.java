package org.fakekoji.core.utils.matrix;

import org.fakekoji.model.Platform;
import org.fakekoji.model.TaskVariantValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class Spec {

    protected final Platform platform;
    protected final Platform.Provider provider;
    protected final List<String> variants;

    public Platform getPlatform() {
        return platform;
    }

    public Platform.Provider getProvider() {
        return provider;
    }
    public List<String> getVariants(){
        return Collections.unmodifiableList(variants);
    }


    public Spec(Platform platform, Platform.Provider provider) {
        this.platform = platform;
        this.provider = provider;
        this.variants = new ArrayList<>();
    }

    public void addVariant(TaskVariantValue v) {
        variants.add(v.getId());
    }

    protected String getVariantsString() {
        if (variants.size() > 0) {
            return "-" + String.join(".", variants);
        } else {
            return "";
        }
    }

    public abstract String toString();

    public boolean matchOs(String os) {
        return (platform.getOs()+platform.getVersion()).equals(os);
    }

    public boolean matchArch(String a) {
        return platform.getArchitecture().equals(a);
    }

    public boolean matchVars(Collection<String> buildVars) {
        if (buildVars.size()!=variants.size()){
            return false;
        }
        for(String v: buildVars){
            if (!variants.contains(v)){
                return false;
            }
        }
        return true;
    }

    public boolean matchProvider(String p) {
        if (p == null){
            //provider is ignored for test only jobs
            return true;
        }
        return provider.getId().equals(p);
    }
}
