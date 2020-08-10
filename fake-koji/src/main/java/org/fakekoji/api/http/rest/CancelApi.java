package org.fakekoji.api.http.rest;

import hudson.plugins.scm.koji.Constants;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.fakekoji.api.http.rest.utils.RedeployApiWorkerBase;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;

public class CancelApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String NO = "no";
    public static final String NO_ENABLE = "enable";
    public static final String NO_DISABLE = "disable";
    public static final String NO_STOP = "stop";
    public static final String NO_ADD = "scratch";
    //without do, jsut listing
    public static final String NO_DO = "do";
    //for scratching only
    public static final String NO_NVR = "nvr";


    private final JDKProjectParser parser;
    private final JDKProjectManager jdkProjectManager;
    private final JDKTestProjectManager jdkTestProjectManager;
    private final AccessibleSettings settings;
    private final PlatformManager platformManager;
    private final JDKVersionManager jdkVersionManager;
    private final TaskVariantManager taskVariantManager;

    CancelApi(final AccessibleSettings settings) {
        this.parser = settings.getJdkProjectParser();
        final ConfigManager configManager = settings.getConfigManager();
        this.jdkProjectManager = configManager.jdkProjectManager;
        this.jdkTestProjectManager = configManager.jdkTestProjectManager;
        this.platformManager = configManager.platformManager;
        this.jdkVersionManager = configManager.jdkVersionManager;
        this.taskVariantManager = configManager.taskVariantManager;
        this.settings = settings;
    }


    public static String getHelp() {
        return "\n"
                + MISC + '/' + NO + "/[enable/disable/stop/scratch] where scratch is tkaing nvr=value argumenr" + "\n"
                + "  enable - enable selected jobs, disable - disables selcted jobs, stop - will stop selected jobs, scratch will insert nvr into jobs processed.txt" + "\n"
                + "  without do=true will just list as usually\n"
                + RedeployApiWorkerBase.getHelp();
    }

    private enum DirectOp {
        enable, disable, stop, nvr;

    }

    private void directOp(DirectOp op, Context context) throws StorageException, IOException, ManagementException {
        List<String> jobNames = new RedeployApiWorkerBase.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
        String doAndHow = context.queryParam(NO_DO);
        String nvr = context.queryParam(NO_NVR);
        if (op.equals(DirectOp.nvr) && nvr == null) {
            context.status(500).result("for "+NO_ADD+" "+NO_NVR+" is mandatory\n");
            return;
        }
        if ("true".equals(doAndHow)) {
            List<String> results = new ArrayList<>(jobNames.size());
            for (String job : jobNames) {
                JenkinsCliWrapper.ClientResponseBase r = null;
                String opString = "unknown";
                if (op.equals(DirectOp.enable)) {
                    r = JenkinsCliWrapper.getCli().enableJob(job);
                    opString = "enabled";
                } else if (op.equals(DirectOp.disable)) {
                    r = JenkinsCliWrapper.getCli().disableJob(job);
                    opString = "disabled";
                } else if (op.equals(DirectOp.stop)) {
                    r = JenkinsCliWrapper.getCli().stopJob(job);
                    opString = "stopped";
                } else if (op.equals(DirectOp.nvr)) {
                    r = addNvrToProcessedTct(nvr, job);
                    opString = "excluded " + nvr;
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

    private JenkinsCliWrapper.ClientResponseBase addNvrToProcessedTct(String nvr, String job) {
        try {
            File processed = new File(new File(settings.getJenkinsJobsRoot(), job), Constants.PROCESSED_BUILDS_HISTORY);
            try (BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(processed, true), "utf-8"))) {
                bf.write(nvr + " # mass, at " + new Date().toString());
                bf.newLine();
            }
            return new JenkinsCliWrapper.ClientResponseBase(0, "so?", "se?", null, NO_ADD + "?nvr=" + nvr + "&do=true");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return new JenkinsCliWrapper.ClientResponseBase(0, "so?", "se?", e, NO_ADD + "?nvr=" + nvr + "&do=true");
        }

    }

    @Override
    public void addEndpoints() {
        get(NO_ENABLE, context -> {
            directOp(DirectOp.enable, context);
        });
        get(NO_DISABLE, context -> {
            directOp(DirectOp.disable, context);
        });
        get(NO_STOP, context -> {
            directOp(DirectOp.stop, context);
        });
        get(NO_ADD, context -> {
            directOp(DirectOp.nvr, context);
        });
    }
}
