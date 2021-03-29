package org.fakekoji.jobmanager.views;

import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VariantsMultiplier {


    public static List<List<List<String>>> splitAndExpand(List<TaskVariant> taskVariants) {
        List<TaskVariant> builds = new ArrayList<>(taskVariants.size());
        List<TaskVariant> tests = new ArrayList<>(taskVariants.size());
        for (TaskVariant taskvariant : taskVariants) {
            if (taskvariant.getType() == Task.Type.BUILD) {
                builds.add(taskvariant);
            } else if (taskvariant.getType() == Task.Type.TEST) {
                tests.add(taskvariant);
            } else {
                throw new RuntimeException("New variant added - " + taskvariant);
            }
        }
        Collections.sort(builds);
        Collections.sort(taskVariants);
        List<List<List<String>>> result = new ArrayList<>();
        result.add(expand(builds));
        result.add(expand(tests));
        return result;
    }

    public static List<List<String>> expand(List<TaskVariant> builds) {
        List<List<String>> r = new ArrayList();
        for (TaskVariant taskvariant : builds) {
            r.add(taskvariant.getVariants().values().stream().map(t -> t.getId()).collect(Collectors.toList()));
            r.get(r.size() - 1).add(0, ""); //adding nothing to each set, to allow subsets of not all variants
        }
        return r;
    }


    public static List<JenkinsViewTemplateBuilder> getAllCombinedVariantsAsFlatView(List<String> taskVariants) throws IOException {
        List<JenkinsViewTemplateBuilder> jvt = new ArrayList<>();
        for (String taskVariant : taskVariants) {
            // this serves for include only tohse views, which had skipped many parts
            //it is disabled now, see tbal ebelow
            int c = 0;
            String unifiedTaskVariant=taskVariant.replace("|", ".");//its metter of taste
            for (int i = 0; i < unifiedTaskVariant.length()-1; i++) {
                if (unifiedTaskVariant.substring(i,i+2).equals("..") ) {
                    c++;
                }
            }
            /** when run above test generator's variants
             *           | and .  just .      .*.*.*.*    with squeezed .*
             * c>-1 (all) 36000   36000       uncounted   seconds
             * c>0        26000   31000       uncounted   seconds
             * c>1        10000   18000       uncounted   seconds
             * c>2        2333    7000        hours       seconds
             * c>3        200     1600        hours       seconds
             * c>4        0       200         minutes     seconds
             * c>5        0       10          seconds     seconds
             */
            if (c > -1) {
                jvt.add(JenkinsViewTemplateBuilderFactory.getVariantTempalte(taskVariant));
            }
        }
        return jvt;
    }

    private static class NestedVariantHelper {
        private final List<NestedVariantHelper> children = new ArrayList<>(0);
    }
}
