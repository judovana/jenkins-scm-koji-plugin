package org.fakekoji.jobmanager.views;

import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private static boolean haveMatch(JenkinsViewTemplateBuilder view, List<String> jobs){
        for (String job : jobs) {
            if (view.getRegex().matcher(job).matches()) {
                return true;
            }
        }
        return false;
    }
    public static List<NestedVariantHelper> combinedVariantsToTree(List<String> views, List<TaskVariant> maxVariants, Optional<List<String>> jobs) throws IOException {
        List<NestedVariantHelper>[] futureTree = new List[maxVariants.size()+1];
        for (int i = 0; i < futureTree.length; i++) {
            futureTree[i] = new ArrayList<>(views.size() / maxVariants.size());
        }
        for (String view : views) {
            NestedVariantHelper v = new NestedVariantHelper(view);
            if (jobs.isPresent()) {
                if (haveMatch(v.view, jobs.get())) {
                    futureTree[v.getSegments()].add(v);
                }
            } else {
                futureTree[v.getSegments()].add(v);
            }
        }
        for (int i = futureTree.length - 1; i > 1/*really,must NOT fold into [0]*/; i--) {
            for (NestedVariantHelper viewToFold : futureTree[i]) {
                for (NestedVariantHelper viewToMatch : futureTree[i - 1]) {
                    if (viewToFold.belongsBelow(viewToMatch)) {
                        viewToMatch.add(viewToFold);
                    }
                }
            }
        }
        return futureTree[1];
    }

    public static Collection<JenkinsViewTemplateBuilder> getAllCombinedVariantsAsTree(List<NestedVariantHelper> tree) throws IOException {
        Set<JenkinsViewTemplateBuilder> r = new HashSet<>(tree.size()*2);//folders+views=>*2
        for(NestedVariantHelper leaf: tree){
            r.add(leaf.view);
            if (leaf.children.size()==1) {//do not create folder for view with one child (whch is actually after removal of empty most...)
                r.addAll(getAllCombinedVariantsAsTree(leaf.children));
            } else if (leaf.children.size()>1) {
                JenkinsViewTemplateBuilder.JenkinsViewTemplateBuilderFolder folder = JenkinsViewTemplateBuilderFactory.getJenkinsViewTemplateBuilderFolder(leaf.name);
                folder.addAll(getAllCombinedVariantsAsTree(leaf.children));
                r.add(folder);
            }
        }
        List<JenkinsViewTemplateBuilder> lr = new ArrayList<>(r);
        Collections.sort(lr, (j1, j2) -> j1.getName().compareTo(j2.getName()));
        return lr;
    }


    /**
     * This class is serving to change to view or nested view later
     * Reason we are using it as middle man is that duing creation some classes would otherwise change from nested to direct or oposite
     */
    private static class NestedVariantHelper {
        private final List<NestedVariantHelper> children = new ArrayList<>(0);
        private final String name;
        private final List<String> split;
        private final JenkinsViewTemplateBuilder view;

        public NestedVariantHelper(String name) throws IOException {
            this.name = name;
            split = Arrays.stream(name.split("[\\.\\|]+")).sequential().filter(s -> !s.isEmpty()).collect(Collectors.toList());
            //this is surprsingly memory optimisation - each view is hundrd times copied,
            //and in 30k x 30k variants it already is necessary.
            //So better to send refferences, then to create to much of them
            //of coourse the folders, have to be created uniq
            //and tbh, also compilation of pattern is pretty expenisve under those numbers
            view = JenkinsViewTemplateBuilderFactory.getVariantTempalte(name);
        }

        @Override
        public String toString() {
            return name + " - " + children.size();

        }

        public int getSegments() {
            return split.size();
        }

        public void add(NestedVariantHelper viewToFold) {
            children.add(viewToFold);
        }

        public boolean belongsBelow(NestedVariantHelper viewToMatch) {
            int matches = 0;
            for (String thatKeys : viewToMatch.split) {
                for (String thisKeys : this.split) {
                    if (thisKeys.equals(thatKeys)) {
                        matches++;
                        break;
                    }
                }
            }
            return matches == viewToMatch.getSegments();
        }
    }
}
