package org.fakekoji.api.http.rest;

import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.RESULTS_DB;

import static io.javalin.apibuilder.ApiBuilder.get;

import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

public class ResultsDb implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    public static final String MAIN_DELIMITER = ":";
    private static final String SECONDARY_DELIMITER = " ";


    private static class ScoreWithTimeStamp {

        private static final String SCORE_DELIMITER = ";";

        final int score;
        final long timestamp;

        public ScoreWithTimeStamp(int score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }

        public ScoreWithTimeStamp(String from) {
            String[] srcs = from.trim().split(SCORE_DELIMITER);
            this.score = Integer.valueOf(srcs[0]);
            this.timestamp = Long.valueOf(srcs[1]);
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
            return score + SCORE_DELIMITER + timestamp;

        }
    }

    private class DB {
        private final Map<String/*nvr*/, Map<String/*job*/, Map<Integer/*jobId*/, List<ScoreWithTimeStamp>>>> nvras = Collections.synchronizedMap(new HashMap<>());
        int added = 0;

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

        private synchronized ScoreWithTimeStamp set(String nvr, String job, Integer buildId, Integer score) {
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId, true);
            ScoreWithTimeStamp newOne = new ScoreWithTimeStamp(score, new Date().getTime());
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
            ScoreWithTimeStamp newOne = new ScoreWithTimeStamp(score, new Date().getTime());
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
            added++;
            if (added > 50) {
                save();
                added = 0;
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
                + MISC + '/' + RESULTS_DB + "/nvrs will return set of nvrs in results db" + "\n"
                + MISC + '/' + RESULTS_DB + "/get will return the score of job of nvr of buildId" + "\n"
                + MISC + '/' + RESULTS_DB + "/del will removethe result for job,nvr,buildId,score, ba careful" + "\n"
                + MISC + '/' + RESULTS_DB + "/set will set the result for job,nvr,buildId,score" + "\n"
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

    }

    private String getNvrs() {
        List l = new ArrayList<>(db.get().keySet());
        return l.stream().sorted().collect(Collectors.joining("\n"))+"\n";
    }

    private String addDelScore(final Context context, final String finalAction) {
        String job = context.queryParam("job");
        String nvr = context.queryParam("nvr");
        String buildId = context.queryParam("buildId");
        String score = context.queryParam("score");
        if (job == null || nvr == null || buildId == null || score == null) {
            throw new RuntimeException("SET job, nvr buildId, and score are mandatory");
        }
        if (finalAction.equals(SET)) {
            return setHelper(job, nvr, buildId, score);
        } else if (finalAction.equals(DEL)) {
            return delHelper(job, nvr, buildId, score);
        } else {
            String s = "unknown action " + finalAction;
            //throw  new RuntimeException(s);
            return s;
        }
    }

    @NotNull
    private String setHelper(String job, String nvr, String buildId, String score) {
        ScoreWithTimeStamp original = db.set(nvr, job, Integer.valueOf(buildId), Integer.valueOf(score));
        if (original == null) {
            return "inserted";
        } else {
            String s = "Not replacing " + original.score + " from " + new Date(original.timestamp).toString();
            //throw  new RuntimeException(s);
            return s;
        }
    }

    private String delHelper(String job, String nvr, String buildId, String score) {
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
            throw new RuntimeException(id + " not found");
        } else {
            return "";
        }
    }

    private String getScore(Context context) {
        String nvr = context.queryParam("nvr");
        String job = context.queryParam("job");
        String buildId = context.queryParam("buildId");
        return getScore(nvr, job, buildId);
    }

    private String getScore(String nvr, String job, String buildId) {

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

    private String iterateJobs(String nvr, Set<Map.Entry<String, Map<Integer, List<ScoreWithTimeStamp>>>> jobs, String job, String buildId) {
        StringBuilder r = new StringBuilder();
        for (Map.Entry<String, Map<Integer, List<ScoreWithTimeStamp>>> jobEntry : jobs) {
            if (job == null || job.equals(jobEntry.getKey())) {
                Set<Map.Entry<Integer, List<ScoreWithTimeStamp>>> buildIds = jobEntry.getValue().entrySet();
                r.append(iterateBuildIds(buildId, buildIds, nvr, jobEntry.getKey()));
            }
        }
        return r.toString();
    }

    private String iterateBuildIds(String buildId, Set<Map.Entry<Integer, List<ScoreWithTimeStamp>>> buildIds, String nvr, String job) {
        StringBuilder r = new StringBuilder();
        for (Map.Entry<Integer, List<ScoreWithTimeStamp>> buildIdEntry : buildIds) {
            if (buildId == null || buildId.equals(buildIdEntry.getKey().toString())) {
                List<ScoreWithTimeStamp> scores = buildIdEntry.getValue();
                r.append(nvr + MAIN_DELIMITER + job + MAIN_DELIMITER + buildIdEntry.getKey() + MAIN_DELIMITER + scoresOut(scores)).append("\n");
            }
        }
        return r.toString();
    }

    private String scoresOut(List<ScoreWithTimeStamp> scores) {
        return scores.stream().map(ScoreWithTimeStamp::toString).collect(Collectors.joining(SECONDARY_DELIMITER));
    }
}
