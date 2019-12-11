package hudson.plugins.scm.koji.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class GlobPredicate implements Predicate<CharSequence>, java.io.Serializable {

    private final List<Pattern> globPatterns;

    public GlobPredicate(String globExpr) {
        if (globExpr == null) {
            globExpr = "";
        }
        String[] origs = globExpr.split("\\s+");
        List<Pattern> tofinal = new ArrayList<>(origs.length);
        for (int i = 0; i < origs.length; i++) {
            String orig = origs[i];
            if (!orig.trim().isEmpty()) {
                tofinal.add(Pattern.compile(orig));
            }
        }
        this.globPatterns = Collections.unmodifiableList(tofinal);
    }

    @Override
    public boolean test(CharSequence input) {
        if (globPatterns.isEmpty()) {
            return true;
        }
        for (Pattern globPattern : globPatterns) {
            if (globPattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;

    }

}
