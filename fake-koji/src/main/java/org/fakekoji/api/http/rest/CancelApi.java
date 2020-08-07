package org.fakekoji.api.http.rest;

import io.javalin.apibuilder.EndpointGroup;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.manager.JDKVersionManager;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.manager.TaskVariantManager;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.List;
import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.fakekoji.api.http.rest.OToolService.MISC;

public class CancelApi implements EndpointGroup {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final String NO = "no";
    //without do, jsut listing
    public static final String NO_ENABLE = "enable";
    public static final String NO_DISABLE = "disable";
    public static final String NO_STOP = "stop";
    public static final String NO_ADD = "scratch";



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
                + RedeployApi.RedeployApiWorkerBase.getHelp();
    }

    @Override
    public void addEndpoints() {
        get(NO_ENABLE, context -> {
            List<String> jobNames = new RedeployApi.RedeployApiStringListing(context).process(jdkProjectManager, jdkTestProjectManager, parser);
            context.status(OToolService.OK).result(String.join("\n", jobNames) + "\n");
        });
    }
}
