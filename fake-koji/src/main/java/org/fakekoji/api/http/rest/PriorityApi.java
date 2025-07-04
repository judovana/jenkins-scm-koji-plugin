package org.fakekoji.api.http.rest;

import static org.fakekoji.api.http.rest.KojiEndpointGroup.MISC;

import static io.javalin.apibuilder.ApiBuilder.get;

import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Run;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

public class PriorityApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String PRIORITY = "priority";
    //without do, just listing
    public static final String PRIORITY_DO = "do";
    private static final String PRIORITY_TYPE = "moveType";
    private static final String PRIORITY_TYPE_DEFAULT = "DOWN_FAST";
    private static final String PRIORITY_VIEW = "viewName";
    private static final String PRIORITY_DELAY = "delay";
    private static final int PRIORITY_DELAY_DEFAULT = 5; //seconds
    private static final String PRIORITY_ITEM = "itemId";
    private static final String PRIORITY_URLSTUB = /*jenkins*/"simpleMoveUnsafe/move?";


    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;

    PriorityApi(final AccessibleSettings settings) {
        this.parser = settings.getJdkProjectParser();
        final ConfigManager configManager = settings.getConfigManager();
        this.jdkProjectManager = configManager.jdkProjectManager;
        this.jdkTestProjectManager = configManager.jdkTestProjectManager;
        this.settings = settings;
    }

    private String getUrlString(String moveType, String itemId, String view) {
        return settings.getJenkinsUrl() + PRIORITY_URLSTUB + PRIORITY_TYPE + "=" + moveType + "&" + PRIORITY_ITEM + "=" + itemId + (view == null ? "" : "&" + PRIORITY_VIEW + "=" + view);
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + PRIORITY + " can move items in jenkins queue if simple-queue-plugin is installed. It filters as usually\n"
                + RedeployApiWorkerBase.THIS_API_IS_USING_SHARED_FILTER
                + " Extended by  " + PRIORITY_VIEW + " which allows to manipulate items only inside selected view and main. Defautls to nothing (equals as all)\n"
                + "  " + PRIORITY_TYPE + " which defaults to " + PRIORITY_TYPE_DEFAULT + ". \n"
                + "  value source of truth is: https://github.com/jenkinsci/simple-queue-plugin/blob/master/src/main/java/cz/mendelu/xotradov/MoveType.java#L7 \n"
                + "  Values are: UP_FAST will move to the top of queue, so executed last; DOWN_FAST wil move to the bottom of queue so executed first \n"
                + "  UP/DOWN will move one item up/down (its actual imapct varies with view/no view; TOP/BOTTOM will move to the top/botom of the items in view\n"
                + "  " + PRIORITY_DELAY + " which delays individual URL.connections to jenkins. Defaults to " + PRIORITY_DELAY_DEFAULT + "\n";
    }


    private void doWork(Context context) throws StorageException, IOException, ManagementException {
        List<String> jobNames = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
        String doAndHow = context.queryParam(PRIORITY_DO);
        String moveType = context.queryParam(PRIORITY_TYPE);
        String view = context.queryParam(PRIORITY_VIEW);
        String delayString = context.queryParam(PRIORITY_DELAY);
        int delay = PRIORITY_DELAY_DEFAULT * 1000;
        if (delayString != null) {
            delay = Integer.parseInt(delayString) * 1000;
        }
        if (moveType == null) {
            moveType = PRIORITY_TYPE_DEFAULT;
        }
        int failures = 0;
        if ("true".equals(doAndHow)) {
            List<String> results = new ArrayList<>(jobNames.size());
            for (String job : jobNames) {
                String url = job;
                try {
                    url = getUrlString(moveType, job, view);
                    LOGGER.log(Level.INFO, "priority: " + url);
                    URL u = new URL(url);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setInstanceFollowRedirects(false);// this is crucial, e are mssuing api, which is supopsed to redirect back, where it come from. Here, we come from nowhere, and redirect would stop the world.
                    c.connect();
                    System.out.println("" + c.getResponseCode());
                    c.disconnect();
                    if (c.getResponseCode() > 399 ) {
                        throw new IOException("server returned " + c.getResponseCode());
                    }
                    results.add("moved " + moveType + " " + job + " in " + view + "; waiting " + (delay / 1000) + "s");
                    Thread.sleep(delay);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, url + " priority failed: " + ex.toString(), ex);
                    failures++;
                    results.add("failed: " + url + " " + ex.toString());
                }
            }
            context.status(OToolService.OK).result(String.join("\n", results) + "\n");
        } else {
            int rr = OToolService.OK;
            if (failures > 0) {
                rr = OToolService.BAD;
            }
            context.status(rr).result(String.join("\n", jobNames) + "\n");
        }
    }


    @Override
    public void addEndpoints() {
        get(context -> {
            doWork(context);
        });
    }
}
