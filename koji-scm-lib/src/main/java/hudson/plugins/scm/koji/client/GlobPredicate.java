package hudson.plugins.scm.koji.client;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class GlobPredicate implements Predicate<CharSequence>, java.io.Serializable {

    private final List<Pattern> globPatterns;
    private final transient TaskListenerLogTransporter log;
    private static final long serialVersionUID = -402287815488043482L;

    public GlobPredicate(String globExpr, TaskListenerLogTransporter logger) {
        if (globExpr == null) {
            globExpr = "";
        }

        log = logger;

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
            logMessage("[KojiSCM]matched: " + input + " because of globPattern is empty");
            return true;
        }
        for (Pattern globPattern : globPatterns) {
            if (globPattern.matcher(input).matches()) {
                logMessage("[KojiSCM]matched: " + input + " because of globPattern: " + globPattern);
                return true;
            }
        }
        logMessage("[KojiSCM]not matched: " + input + "because this globPattern didn't match anything.");
        return false;
    }

    private void logMessage(String message) {
        if(log != null) {
           log.println(message);
        }
    }
}
