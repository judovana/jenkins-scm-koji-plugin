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
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId);
            scores.add(result);
        }

        private synchronized ScoreWithTimeStamp set(String nvr, String job, Integer buildId, Integer score) {
            List<ScoreWithTimeStamp> scores = getChain(nvr, job, buildId);
            ScoreWithTimeStamp newOne = new ScoreWithTimeStamp(score, new Date().getTime());
            for (ScoreWithTimeStamp oldOne : scores) {
                if (oldOne.equals(newOne)) {
                    //we do not overwrite
                    return oldOne;
                }
            }
            scores.add(newOne);
            added++;
            if (added > 50) {
                save();
                added = 0;
            }
            return null;
        }

        @NotNull
        private List<ScoreWithTimeStamp> getChain(String nvr, String job, Integer buildId) {
            Map<String, Map<Integer, List<ScoreWithTimeStamp>>> jobs = nvras.get(nvr);
            if (jobs == null) {
                jobs = Collections.synchronizedMap(new HashMap<>());
                nvras.put(nvr, jobs);
            }
            Map<Integer, List<ScoreWithTimeStamp>> jobIds = jobs.get(job);
            if (jobIds == null) {
                jobIds = Collections.synchronizedMap(new HashMap<>());
                jobs.put(job, jobIds);
            }
            List<ScoreWithTimeStamp> scores = jobIds.get(buildId);
            if (scores == null) {
                scores = Collections.synchronizedList(new ArrayList<>());
                jobIds.put(buildId, scores);
            }
            return scores;
        }

        private synchronized Map<String, Map<String, Map<Integer, List<ScoreWithTimeStamp>>>> get() {
            return nvras;
        }


    }

    public static final String SET = "set";
    public static final String GET = "get";
    private final AccessibleSettings settings;
    private final DB db;


    ResultsDb(final AccessibleSettings settings) {
        this.settings = settings;
        this.db = new DB();
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + RESULTS_DB + "/get will return the score of job of nvr of buildId" + "\n"
                + MISC + '/' + RESULTS_DB + "/set will set the result for job,nvr,buildId,score" + "\n"
                + " Negative jobId is manual touch, negative score is manual action, time is automated\n"
                + " for set nvr, job, buildId and score are mandatory, For get not, but you will get all matching results\n";
    }


    @Override
    public void addEndpoints() {
        get(SET, context -> {
            try {
                String s = addScore(context);
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

    private String addScore(Context context) {
        String job = context.queryParam("job");
        String nvr = context.queryParam("nvr");
        String buildId = context.queryParam("buildId");
        String score = context.queryParam("score");
        if (job == null || nvr == null || buildId == null || score == null) {
            throw new RuntimeException("SET job, nvr buildId, and score are mandatory");
        }
        ScoreWithTimeStamp original = db.set(nvr, job, Integer.valueOf(buildId), Integer.valueOf(score));
        if (original == null) {
            return "inserted";
        } else {
            String s = "Not replacing " + original.score + " from " + new Date(original.timestamp).toString();
            //throw  new RuntimeException(s);
            return s;
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
