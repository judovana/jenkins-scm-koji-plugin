package org.fakekoji.api.http.rest.args;

import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.manager.PlatformManager;
import org.fakekoji.jobmanager.model.PlatformBumpVariant;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.fakekoji.api.http.rest.BumperAPI.BUMP_FROM;
import static org.fakekoji.api.http.rest.BumperAPI.BUMP_TO;
import static org.fakekoji.api.http.rest.BumperAPI.FILTER;
import static org.fakekoji.api.http.rest.BumperAPI.PLATFORM_BUMP_VARIANT;
import static org.fakekoji.api.http.rest.BumperAPI.PROJECTS;
import static org.fakekoji.api.http.rest.RestUtils.extractMandatoryParamValue;
import static org.fakekoji.api.http.rest.RestUtils.extractParamValue;

public class BumpPlatformArgs extends BumpArgs {
    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    final public Platform from;
    final public Platform to;
    final public Optional<Pattern> filter;
    final public PlatformBumpVariant variant;
    final public List<Project> projects;

    BumpPlatformArgs(
            BumpArgs bumpArgs,
            final Platform from,
            final Platform to,
            final Optional<Pattern> filter,
            final PlatformBumpVariant variant,
            final List<Project> projects
    ) {
        super(bumpArgs);
        this.from = from;
        this.to = to;
        this.filter = filter;
        this.variant = variant;
        this.projects = projects;
    }

    public static Result<BumpPlatformArgs, OToolError> parse(
            final ConfigManager configManger,
            final Map<String, List<String>> paramsMap
    ) {
        return extractParams(paramsMap).flatMap(params -> {
            final Platform from;
            final Platform to;
            try {
                final PlatformManager platformManager = configManger.platformManager;
                from = platformManager.read(params.from);
                to = platformManager.read(params.to);
            } catch (StorageException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return Result.err(new OToolError(e.getMessage(), 500));
            } catch (ManagementException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return Result.err(new OToolError(e.getMessage(), 400));
            }
            final Result<PlatformBumpVariant, String> variantParseResult
                    = PlatformBumpVariant.parse(params.variant);
            if (variantParseResult.isError()) {
                return Result.err(new OToolError(variantParseResult.getError(), 400));
            }
            final PlatformBumpVariant variant = variantParseResult.getValue();
            final List<String> projectsList = Arrays.asList(params.projects.split(","));
            final Optional<Pattern> filter =  Optional.ofNullable(compileFilter(params.filter));
            return BumpArgs.parseBumpArgs(paramsMap).flatMap(bumpArgs ->
                    configManger.getProjects(projectsList).flatMap(projects ->
                            Result.ok(new BumpPlatformArgs(bumpArgs, from, to, filter, variant, projects))
                    )
            );
        });
    }

    private static Pattern compileFilter(Optional<String> filter) {
        if  (filter == null || !filter.isPresent()){
            return null;
        }
        return Pattern.compile(filter.get());
    }

    static Result<ExtractedParams, OToolError> extractParams(final Map<String, List<String>> params) {
        final Optional<String> filter = extractParamValue(params, FILTER);
        return extractMandatoryParamValue(params, BUMP_FROM).flatMap(from ->
                extractMandatoryParamValue(params, BUMP_TO).flatMap(to ->
                        extractMandatoryParamValue(params, PROJECTS).flatMap(projects ->
                                extractMandatoryParamValue(params, PLATFORM_BUMP_VARIANT).flatMap(variant ->
                                        Result.ok(new ExtractedParams(from, to, variant, projects, filter))
                                )
                        )
                )
        );
    }

    private static class ExtractedParams {
        private final String from;
        private final String to;
        private final String variant;
        private final String projects;
        private final Optional<String> filter;

        ExtractedParams(final String from, final String to, final String variant, final String projects, Optional<String> filter) {
            this.from = from;
            this.to = to;
            this.variant = variant;
            this.projects = projects;
            this.filter = filter;
        }
    }
}
