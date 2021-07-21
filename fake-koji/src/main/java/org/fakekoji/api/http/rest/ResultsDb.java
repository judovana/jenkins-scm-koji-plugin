package org.fakekoji.api.http.rest;

import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.RESULTS_DB;

import static io.javalin.apibuilder.ApiBuilder.get;

import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;


import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

public class ResultsDb implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    public static final String MAIN_DELIMITER = ":";
    private static final String SECONDARY_DELIMITER = " ";
    public static final int LIMIT_TO_SAVE = 50;

    private static class ScoreWithTimeStamp {

        private static final String SCORE_DELIMITER = ";";

        final int score;
        final long timestamp;
        final Optional<String> message;

        public ScoreWithTimeStamp(int score, long timestamp, Optional<String> message) {
            this.score = score;
            this.timestamp = timestamp;
            this.message = message;
            if (message.isPresent()) {
                if (message.get().contains(SECONDARY_DELIMITER) ||
                        message.get().contains(SCORE_DELIMITER) ||
                        message.get().contains(MAIN_DELIMITER)) {
                    throw new RuntimeException("Forbidden chars of '" + SECONDARY_DELIMITER + "', '" + SCORE_DELIMITER + "', '" + MAIN_DELIMITER + "' present in " + message.get());
                }
            }
        }

        public ScoreWithTimeStamp(String from) {
            String[] srcs = from.trim().split(SCORE_DELIMITER);
            this.score = Integer.valueOf(srcs[0]);
            this.timestamp = Long.valueOf(srcs[1]);
            if (srcs.length > 2) {
                message = Optional.of(srcs[2].replace(SCORE_DELIMITER, "%3B").replace(MAIN_DELIMITER, "%3A").replace(SECONDARY_DELIMITER, "%20"));
            } else {
                message = Optional.empty();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScoreWithTimeStamp that = (ScoreWithTimeStamp) o;
            return score == that.score;
        }

        @Override
        public int hashCode() {
            return score;
        }

        @Override
        public String toString() {
            return score + SCORE_DELIMITER + timestamp + SCORE_DELIMITER + message.orElse("");

        }
    }

    private class DB {
        private final Map<String/*nvr*/, Map<String/*job*/, Map<Integer/*jobId*/, List<ScoreWithTimeStamp>>>> nvras = Collections.synchronizedMap(new HashMap<>());
        AtomicInteger added = new AtomicInteger(0);

        public DB() {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    DB.this.save();
                }
            });
            load();
        }

        private synchronized void load() {
            List<String> lines = new ArrayList<>();
            try {
                lines = Utils.readFileToLines(settings.getResultsFile(), null);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "error loading results db: " + settings.getResultsFile().getAbsoluteFile(), ex);
                return;
            }
            nvras.clear();
            for (String line : lines) {
                try {
                    String[] main = line.split(MAIN_DELIMITER);
                    String[] second = main[3].split(SECONDARY_DELIMITER);
                    for(String result: second) {
                        set(main[0], main[1], Integer.valueOf(main[2]), new ScoreWithTimeStamp(result));
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "error results db: " + settings.getResultsFile().getAbsoluteFile() + " line of " + line, ex);
                    return;
                }
            }
        }

        private synchronized void save() {
            try {
                Utils.writeToFile(settings.getResultsFile(), getScore(null, null, null));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "error saving results db: " + settings.getResultsFile().getAbsoluteFile(), ex);
            }

        }

        private synchronized void set(String nvr, String job, Integer buildId, ScoreWithTimeStamp result) {
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId, true);
            scores.add(result);
        }

        private synchronized ScoreWithTimeStamp set(String nvr, String job, Integer buildId, Integer score, Optional<String> messgae) {
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId, true);
            ScoreWithTimeStamp newOne = new ScoreWithTimeStamp(score, new Date().getTime(), messgae);
            for (ScoreWithTimeStamp oldOne : scores) {
                if (oldOne.equals(newOne)) {
                    //we do not overwrite
                    return oldOne;
                }
            }
            scores.add(newOne);
            saveOnCount();
            return null;
        }

        private synchronized ScoreWithTimeStamp del(String nvr, String job, Integer buildId, Integer score) {
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId, false);
            ScoreWithTimeStamp newOne = new ScoreWithTimeStamp(score, new Date().getTime(), Optional.empty());
            for (ScoreWithTimeStamp oldOne : scores) {
                if (oldOne.equals(newOne)) {
                    int deleteions = delChain(nvr, job, buildId, oldOne);
                    if (deleteions == 0){
                        LOGGER.log(Level.WARNING, "deletion of " + nvr +", "+job+", "+buildId+", "+score+" is bad, deleted: "+deleteions);
                    }
                    saveOnCount();
                    return oldOne;
                }
            }
            return null;
        }

        private void saveOnCount() {
            added.incrementAndGet();
            if (added.get() > LIMIT_TO_SAVE) {
                save();
                added.set(0);
            }
        }

        @NotNull
        private synchronized List<ScoreWithTimeStamp> getChain(String nvr, String job, Integer buildId, boolean putIfNeeded) {
            Map<String, Map<Integer, List<ScoreWithTimeStamp>>> jobs = nvras.get(nvr);
            if (jobs == null) {
                jobs = Collections.synchronizedMap(new HashMap<>());
                 if (putIfNeeded) {
                     nvras.put(nvr, jobs);
                 }
            }
            Map<Integer, List<ScoreWithTimeStamp>> jobIds = jobs.get(job);
            if (jobIds == null) {
                jobIds = Collections.synchronizedMap(new HashMap<>());
                if (putIfNeeded) {
                    jobs.put(job, jobIds);
                }
            }
            List<ScoreWithTimeStamp> scores = jobIds.get(buildId);
            if (scores == null) {
                scores = Collections.synchronizedList(new ArrayList<>());
                if (putIfNeeded) {
                    jobIds.put(buildId, scores);
                }
            }
            return scores;
        }

        @NotNull
        private int delChain(String nvr, String job, Integer buildId, ScoreWithTimeStamp oldOne) {
            int deletions = 0;
            Map<String, Map<Integer, List<ScoreWithTimeStamp>>> jobs = nvras.get(nvr);
            if (jobs == null){
                return -1;
            }
            Map<Integer, List<ScoreWithTimeStamp>> jobIds = jobs.get(job);
            if (jobIds == null){
                return -1;
            }
            List<ScoreWithTimeStamp> scores = jobIds.get(buildId);
            if (scores == null){
                return -1;
            }
            if(scores.remove(oldOne)){
                deletions++;
            }
            if (scores.isEmpty()){
                if (jobIds.remove(buildId, scores)){
                    deletions++;
                }
            }
            if (jobIds.isEmpty()){
                if (jobs.remove(job, jobIds)){
                    deletions ++;
                }
            }
            if (jobs.isEmpty()){
                if (nvras.remove(nvr, jobs)){
                    deletions ++;
                }
            }
            return deletions;
        }

        private synchronized Map<String, Map<String, Map<Integer, List<ScoreWithTimeStamp>>>> get() {
            return nvras;
        }


    }

    public static final String SET = "set";
    public static final String GET = "get";
    public static final String REPORT = "report";
    public static final String DEL = "del";
    public static final String NVRS = "nvrs";
    private final AccessibleSettings settings;
    private final DB db;


    ResultsDb(final AccessibleSettings settings) {
        this.settings = settings;
        this.db = new DB();
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + RESULTS_DB + "/" + NVRS + " will return set of nvrs in results db" + "\n"
                + MISC + '/' + RESULTS_DB + "/" + GET + " will return the score of job of nvr of buildId" + "\n"
                + MISC + '/' + RESULTS_DB + "/" + DEL + " will removethe result for job,nvr,buildId,score, ba careful" + "\n"
                + MISC + '/' + RESULTS_DB + "/" + SET + " will set the result for job,nvr,buildId,score" + "\n"
                + MISC + '/' + RESULTS_DB + "/" + REPORT + " will generate post-mortem texts (nvr and job may be regexes here)" + "\n"
                + " Negative jobId is manual touch, time is automated\n"
                + " for set nvr, job, buildId and score are mandatory, For get not, but you will get all matching results\n";
    }


    @Override
    public void addEndpoints() {
        get(NVRS, context -> {
            try {
                context.status(OToolService.OK).result(getNvrs());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(SET, context -> {
            try {
                String s = addDelScore(context, SET);
                context.status(OToolService.OK).result(s);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(DEL, context -> {
            try {
                String s = addDelScore(context, DEL);
                context.status(OToolService.OK).result(s);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        get(GET, context -> {
            try {
                String s = getScore(context);
                context.status(OToolService.OK).result(s);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });
        get(REPORT, context -> {
            try {
                String s = getReport(context);
                context.status(OToolService.OK).result(s);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                context.status(500).result(e.getClass().getName() + ": " + e.getMessage());
            }

        });

    }


    String getSet(String job, String nvr, String buildId, String score, Optional<String> message) {
        return setHelper(job, nvr, buildId, score, message);
    }

    String getDel(String job, String nvr, String buildId, String score) {
        return delHelper(job, nvr, buildId, score);
    }

    synchronized String getNvrs() {
        List l = new ArrayList<>(db.get().keySet());
        return l.stream().sorted().collect(Collectors.joining("\n")) + "\n";
    }

    private String addDelScore(final Context context, final String finalAction) throws UnsupportedEncodingException {
        final String job = context.queryParam("job");
        final String nvr = context.queryParam("nvr");
        final String buildId = context.queryParam("buildId");
        final String score = context.queryParam("score");
        //unluckily for us, javalin decodes soemthing what it should not - parameter
        final String messagevalue = context.queryParam("message");
        final Optional<String> message = messagevalue == null || messagevalue.trim() == "" ? Optional.empty() : Optional.of(URLEncoder.encode(messagevalue, StandardCharsets.UTF_8.toString()));
        if (job == null || nvr == null || buildId == null || score == null) {
            throw new RuntimeException("SET job, nvr buildId, and score are mandatory");
        }
        if (finalAction.equals(SET)) {
            return setHelper(job, nvr, buildId, score, message);
        } else if (finalAction.equals(DEL)) {
            return delHelper(job, nvr, buildId, score);
        } else {
            String s = "unknown action " + finalAction;
            //throw  new RuntimeException(s);
            return s;
        }
    }

    @NotNull
    synchronized private String setHelper(String job, String nvr, String buildId, String score, Optional<String> message) {
        ScoreWithTimeStamp original = db.set(nvr, job, Integer.valueOf(buildId), Integer.valueOf(score), message);
        if (original == null) {
            return "inserted";
        } else {
            String s = "Not replacing " + original.score + " from " + new Date(original.timestamp).toString();
            //throw  new RuntimeException(s);
            return s;
        }
    }

    private synchronized  String delHelper(String job, String nvr, String buildId, String score) {
        ScoreWithTimeStamp original = db.del(nvr, job, Integer.valueOf(buildId), Integer.valueOf(score));
        if (original == null) {
            String s = "Not deleted " + nvr +", "+job+", "+buildId+", "+score;
            //throw  new RuntimeException(s);
            return s;
        } else {
            return "deleted";
        }
    }

    String throwOrReturn(String id) {
        boolean ratherThrow = true;
        if (ratherThrow) {
            throw new ItemNotFoundException(id + " not found");
        } else {
            return "";
        }
    }

    private synchronized String getReport(Context context) {
        String nvr = context.queryParam("nvr", ".*");
        String job = context.queryParam("job", ".*");
        return getReport(Pattern.compile(nvr), Pattern.compile(job));
    }

    synchronized String getReport(Pattern nvrEx, Pattern jobEx) {
        StringBuilder r = new StringBuilder();
        List<String> nvrs = new ArrayList<>(db.get().keySet());
        Collections.sort(nvrs);
        for (String nvr : nvrs) {
            if (nvrEx.matcher(nvr).matches()) {
                r.append(nvr).append("\n");
                r.append(getReportForNvr(db.get().get(nvr), jobEx));
            }
        }
        return r.toString();
    }

    synchronized String getReportForNvr(Map<String, Map<Integer, List<ScoreWithTimeStamp>>> nvr, Pattern jobEx) {
        StringBuilder r = new StringBuilder();
        List<String> jobs = new ArrayList<>(nvr.keySet());
        Collections.sort(jobs);
        for (int mark = 4; mark >= 0; mark--) {
            r.append("  ** " + markToNiceString(mark)).append(" **\n");
            for (String job : jobs) {
                if (jobEx.matcher(job).matches()) {
                    String content = getReportForJob(nvr.get(job), job, mark);
                    if (!content.trim().isEmpty()) {
                        r.append("    " + job).append("\n");
                        r.append(content);
                    }
                }
            }
        }
        return r.toString();
    }

    private String markToNiceString(int mark) {
        switch (mark) {
            case (JobBuildScoreStamp.VERIFIED_GOOD):
                return "waived - ok";
            case (JobBuildScoreStamp.GOOD):
                return "ignored";
            case (JobBuildScoreStamp.NEEDS_INSPECTION):
                return "reruned";
            case (JobBuildScoreStamp.FAILED):
                return "incorrect";
            case (JobBuildScoreStamp.VERIFIED_FAILED):
                return "failed";
            default:
                throw new RuntimeException("Invalid wight resolution: " + mark);
        }
    }

    private static final class JobBuildScoreStamp implements Comparable<JobBuildScoreStamp> {
        //job is here only for link generation
        final String job;
        final int id;
        final int score;
        final long timestamp;
        final Optional<String> message;

        public JobBuildScoreStamp(String job, int id, int score, long timestamp, Optional<String> message) {
            this.job = job;
            this.id = id;
            this.score = score;
            this.timestamp = timestamp;
            this.message = message;
        }

        boolean haveMessage(){
            if (message.isPresent()){
                return !message.get().trim().isEmpty();
            } else {
                return false;
            }
        }


        @Override
        public int compareTo(@NotNull JobBuildScoreStamp o) {
            if (o.timestamp == this.timestamp) {
                return 0;
            } else if (this.timestamp > o.timestamp) {
                return -1;
            } else {
                return 1;
            }
        }

        public String toPlain() {
            try {
                return getSeriousnessToColorFromHealth(getMark(), isManual()) + " " + getDate() + " (" + getDaysAgo() + " days ago): " + URLDecoder.decode(message.orElse(""), "utf-8");
            }catch(UnsupportedEncodingException ex){
                LOGGER.log(Level.INFO, ex, null);
                return ex.toString();
            }
        }

        String getDaysAgo() {
            double diff = new Date().getTime() - timestamp;
            double toDay = 24 * 60 * 60 * 1000;
            double days = diff / toDay;
            BigDecimal toString = BigDecimal.valueOf(days);
            return toString.setScale(2, RoundingMode.HALF_EVEN).toString();
        }

        String getDate() {
            return new Date(timestamp).toString();
        }

        int getMark() {
            return getSeriousnessFromScore(score);
        }


        /**
         * WARNING!!!!
         * Following is copypasted from HydraDailyReport.java
         * and also is duplicated in the JS table (ajax.html)!!!
         * (from which are the manual runs collors)
         *
         * If the score distribution ever changes, this must be fixed everywhere!!
         *
         */

        public boolean isManual(){
            return id < 0;
        }

        private static final int GREEN = 0;
        private static final int WHITE = 40;
        private static final int YELLOW = 100;
        private static final int MANUAL_FAIL=100000;

        private static final int VERIFIED_GOOD=0;
        private static final int GOOD=1;
        private static final int NEEDS_INSPECTION=2;
        private static final int FAILED=3;
        private static final int VERIFIED_FAILED=4;

        private static String getSeriousnessToStyleFromHealth(int health, boolean manual) {
            return "style=\" background-color: "+getSeriousnessToColorFromHealth(health, manual)+" \"";
        }
        private static String getSeriousnessToColorFromHealth(int health, boolean manual) {
            if (manual){
                switch (health) {
                    case (VERIFIED_GOOD):
                        return "darkgreen";
                    case (GOOD):
                        return "purple";
                    case (NEEDS_INSPECTION):
                        return "magenta";
                    case (FAILED):
                        return "black";
                    case (VERIFIED_FAILED):
                        return "orange";
                    default:
                        throw new RuntimeException("Invalid wight resolution: " + health);
                }
            } else {
                switch (health) {
                    case (VERIFIED_GOOD):
                        return "green";
                    case (GOOD):
                        return "lightgreen";
                    case (NEEDS_INSPECTION):
                        return "yellow";
                    case (FAILED):
                        return "red";
                    case (VERIFIED_FAILED):
                        return "red";
                    default:
                        throw new RuntimeException("Invalid auto-wight resolution: " + health);
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        @SuppressFBWarnings(value = "UC_USELESS_CONDITION", justification = "The redundant 'greater than' conditions improve human readability of the code.")
        private static int getSeriousnessFromScore(int weight) {
            if (weight <= GREEN) {
                return VERIFIED_GOOD;
                /*>green && <= white, no op:)*/
            } else if (weight > GREEN && weight <= WHITE) {
                return GOOD;
            } else if (weight > WHITE && weight < YELLOW) {
                return NEEDS_INSPECTION;
            } else if (weight >= YELLOW  && weight < MANUAL_FAIL) {
                return FAILED;
            } else if (weight >= MANUAL_FAIL) {
                return VERIFIED_FAILED;
            }
            throw new RuntimeException("Unresolved weight: " + weight);
        }
    }

    private String getReportForJob(Map<Integer, List<ScoreWithTimeStamp>> buildsWithScoreAndStamp, String job, int mark) {
        StringBuilder sb = new StringBuilder();
        List<JobBuildScoreStamp> all = unpackJobsResults(buildsWithScoreAndStamp, job);
        boolean okToInclude = false;
        for (JobBuildScoreStamp result : all) {
            if (result.getMark() == mark && result.haveMessage()) {
                okToInclude = true;
                break;
            }
        }
        if (okToInclude) {
            for (JobBuildScoreStamp item : all) {
                if (item.getMark() == mark && item.haveMessage()) {
                    sb.append("----->" + item.toPlain() + "\n");
                } else {
                    sb.append("      " + item.toPlain() + "\n");
                }
            }
        }
        return sb.toString();
    }

    @NotNull
    private List<JobBuildScoreStamp> unpackJobsResults(Map<Integer, List<ScoreWithTimeStamp>> buildsWithScoreAndStamp, String job) {
        List<JobBuildScoreStamp> all = new ArrayList<>();
        Set<Map.Entry<Integer, List<ScoreWithTimeStamp>>> builds = buildsWithScoreAndStamp.entrySet();
        for (Map.Entry<Integer, List<ScoreWithTimeStamp>> build : builds) {
            for (ScoreWithTimeStamp inBuild : build.getValue()) {
                all.add(new JobBuildScoreStamp(
                        job,
                        build.getKey(),
                        inBuild.score,
                        inBuild.timestamp,
                        inBuild.message
                ));
            }
        }
        Collections.sort(all);
        return all;
    }


    private synchronized String getScore(Context context) {
        String nvr = context.queryParam("nvr");
        String job = context.queryParam("job");
        String buildId = context.queryParam("buildId");
        return getScore(nvr, job, buildId);
    }

    synchronized String getScore(String nvr, String job, String buildId) {
        StringBuilder r = new StringBuilder();
        if (nvr != null) {
            Map<String, Map<Integer, List<ScoreWithTimeStamp>>> foundNvr = db.get().get(nvr);
            if (foundNvr == null) {
                return throwOrReturn(nvr);
            } else {
                if (job != null) {
                    Map<Integer, List<ScoreWithTimeStamp>> foundJob = foundNvr.get(job);
                    if (foundJob == null) {
                        return throwOrReturn(job);
                    } else {
                        if (buildId != null) {
                            List<ScoreWithTimeStamp> foundBuildId = foundJob.get(Integer.valueOf(buildId));
                            if (foundBuildId == null) {
                                return throwOrReturn(buildId);
                            } else {
                                return scoresOut(foundBuildId);
                            }
                        } else {
                            return iterateBuildIds(buildId, foundJob.entrySet(), nvr, job);
                        }
                    }
                } else {
                    return iterateJobs(nvr, foundNvr.entrySet(), job, buildId);
                }
            }
        } else {
            Set<Map.Entry<String, Map<String, Map<Integer, List<ScoreWithTimeStamp>>>>> nvrs = db.get().entrySet();
            for (Map.Entry<String, Map<String, Map<Integer, List<ScoreWithTimeStamp>>>> nvrEntry : nvrs) {
                if (nvr == null || nvr.equals(nvrEntry.getKey())) {
                    r.append(iterateJobs(nvrEntry.getKey(), nvrEntry.getValue().entrySet(), job, buildId));
                }
            }
        }
        return r.toString();
    }

    private synchronized String iterateJobs(String nvr, Set<Map.Entry<String, Map<Integer, List<ScoreWithTimeStamp>>>> jobs, String job, String buildId) {
        StringBuilder r = new StringBuilder();
        for (Map.Entry<String, Map<Integer, List<ScoreWithTimeStamp>>> jobEntry : jobs) {
            if (job == null || job.equals(jobEntry.getKey())) {
                Set<Map.Entry<Integer, List<ScoreWithTimeStamp>>> buildIds = jobEntry.getValue().entrySet();
                r.append(iterateBuildIds(buildId, buildIds, nvr, jobEntry.getKey()));
            }
        }
        return r.toString();
    }

    private synchronized String iterateBuildIds(String buildId, Set<Map.Entry<Integer, List<ScoreWithTimeStamp>>> buildIds, String nvr, String job) {
        StringBuilder r = new StringBuilder();
        for (Map.Entry<Integer, List<ScoreWithTimeStamp>> buildIdEntry : buildIds) {
            if (buildId == null || buildId.equals(buildIdEntry.getKey().toString())) {
                List<ScoreWithTimeStamp> scores = buildIdEntry.getValue();
                r.append(nvr + MAIN_DELIMITER + job + MAIN_DELIMITER + buildIdEntry.getKey() + MAIN_DELIMITER + scoresOut(scores)).append("\n");
            }
        }
        return r.toString();
    }

    private synchronized String scoresOut(List<ScoreWithTimeStamp> scores) {
        return scores.stream().map(ScoreWithTimeStamp::toString).collect(Collectors.joining(SECONDARY_DELIMITER));
    }

    class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(String s) {
            super(s);
        }
    }
}
