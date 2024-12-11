package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;

public class DuplicateCoverageApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String DUPLICATE = "duplicate";
    public static final String DUPLICATE_TASK = "task";
    public static final String DUPLICATE_SOURCE = "source";
    public static final String DUPLICATE_TARGET = "target";
    //without do, jsut listing
    public static final String DUPLICATE_DO = "do";
    //for scratching only



    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;

    DuplicateCoverageApi(final AccessibleSettings settings) {
        this.parser = settings.getJdkProjectParser();
        final ConfigManager configManager = settings.getConfigManager();
        this.jdkProjectManager = configManager.jdkProjectManager;
        this.jdkTestProjectManager = configManager.jdkTestProjectManager;
            this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + DUPLICATE + "/task source taskNameIn target taskNameOut\n"
                + "  Will apply full shared filter, and then select all matching taskNameIn, and create same set of jobs for taskNameOut\n"
                + "  both taskNameIn taskNameOut must beexisting tasks. Note that if you exclude taskNameIn via shared filter, you will get empty output set.\n"
                + "  without do=true will just list as usually\n"
                + RedeployApiWorkerBase.THIS_API_IS_USING_SHARED_FILTER;
    }

    private enum DirectOp {
        task;

    }

    private void directOp(DirectOp op, Context context) throws StorageException, IOException, ManagementException {
        List<String> jobNames = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
        String doAndHow = context.queryParam(DUPLICATE_DO);
        String source = context.queryParam(DUPLICATE_SOURCE);
        String target = context.queryParam(DUPLICATE_TARGET);
        if (source == null || target == null) {
            context.status(500).result(DUPLICATE_SOURCE+" and "+DUPLICATE_TARGET+" are mandatory\n");
            return;
        }
        if ("true".equals(doAndHow)) {
            List<String> results = new ArrayList<>(jobNames.size());
            for (String job : jobNames) {
                JenkinsCliWrapper.ClientResponseBase r = null;
                String opString = "unknown";
                if (op.equals(DirectOp.task)) {
                    r = JenkinsCliWrapper.getCli().enableJob(job);
                    opString = "enabled";
                }

                if (r.simpleVerdict()) {
                    results.add(opString + " " + job);
                } else {
                    results.add("failed " + job + " " + r.toString());
                }
            }
            context.status(OToolService.OK).result(String.join("\n", results) + "\n");
        } else {
            context.status(OToolService.OK).result(String.join("\n", jobNames) + "\n");
        }
    }


    @Override
    public void addEndpoints() {
        get(DUPLICATE_TASK, context -> {
            directOp(DirectOp.task, context);
        });
    }
}
