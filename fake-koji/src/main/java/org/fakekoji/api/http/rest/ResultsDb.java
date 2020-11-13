package org.fakekoji.api.http.rest;

import static org.fakekoji.api.http.rest.OToolService.MISC;
import static org.fakekoji.api.http.rest.OToolService.RESULTS_DB;

import static io.javalin.apibuilder.ApiBuilder.get;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

public class ResultsDb implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static class ScoreWithTimeStamp {
        final int score;
        final long timestamp;

        public ScoreWithTimeStamp(int score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }
    }

    private class DB {
        private final Map<String/*job*/, Map<String/*nvr*/,Map<Integer/*jobId*/,ScoreWithTimeStamp>>> db = Collections.synchronizedMap(new HashMap<>());

        private synchronized void load(){
            System.out.println(settings.getResultsFile());
        }

        private synchronized void save(){
            System.out.println(settings.getResultsFile());

        }

        private synchronized void set(String job,  String nvr, Integer jobId, Integer score){

        }

        private synchronized Map<String, Map<String, Map<Integer, ScoreWithTimeStamp>>> et(){
            return db;
        }


    }

    public static final String SET = "set";
    public static final String GET = "get";
    private final AccessibleSettings settings;


    ResultsDb(final AccessibleSettings settings) {
        this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + RESULTS_DB + "/get will return the score of job of nvr of buildId" + "\n"
                + MISC + '/' + RESULTS_DB + "/set will set the result for job,nvr,buildId,score" + "\n"
                + " Negative jobId is manual touch, negative score is manual action, time is automated\n"
                + " for set job, nvr, buildId and score are mandatory, For get not, but you will get all matching results\n";
    }


    @Override
    public void addEndpoints() {
        get(SET, context -> {
            try{
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

    private String  addScore(Context context) {
        String job = context.queryParam("job");
        String nvr = context.queryParam("nvr");
        String buildId = context.queryParam("buildId");
        String score = context.queryParam("score");
        if (job == null || nvr == null || buildId == null || score == null) {
            throw new RuntimeException("SET job, nvr buildId, and score are mandatory");
        }
        return "db ok";
    }

    private String getScore(Context context) {
        String job = context.queryParam("job");
        String nvr = context.queryParam("nvr");
        String buildId = context.queryParam("buildId");
        return "db ok";

    }
}
