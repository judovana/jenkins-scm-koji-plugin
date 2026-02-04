package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class OToolServiceRun {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static void main(String[] args) throws IOException {
        if ("keep".equals(System.getProperty("otool.test.cli"))) {
            LOGGER.severe("otool.test.cli=keep, cli kept running. This is dangerous!");
        } else {
            LOGGER.info("otool.test.cli!=keep, cli will be correctly repalced by no-op instance.");
            JenkinsCliWrapper.killCli();
            LOGGER.info("Done");
        }
        final File oTool = Files.createTempDirectory("oTool").toFile();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersOnFileRoot(oTool.toPath());
        if (System.getProperty("otool.testdb.dir") != null) {
            Files.copy(new File(System.getProperty("otool.testdb.dir") + "/results.db").toPath(), new File(folderHolder.configsRoot, "results.db").toPath());
        }
        OToolService oToolService = new OToolService(DataGenerator.getSettings(folderHolder));
        LOGGER.severe(JenkinsCliWrapper.getCli().toString());
        oToolService.start();
    }

    Map<String, List<String>> winZips = new HashMap<>();
    Map<String, List<String>> rpms = new HashMap<>();
    Map<String, List<String>> containers = new HashMap<>();
    Map<String, List<String>> portable = new HashMap<>();

    List<String> container = Arrays.asList("docker-image-sha256:084490c9716c6e661eabeb78208dfd8124f031f8d7fce066b7f7cca736b22ff6.x86_64.tar.gz");
    List<String> jdk8F30src = Arrays.asList("java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.src.rpm");
    List<String> jdk8F30aarch64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.aarch64.rpm");
    List<String> jdk8F30armv7hl = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.armv7hl.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.armv7hl.rpm");
    List<String> jdk8F30i686 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-openjfx-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-openjfx-devel-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-openjfx-devel-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-openjfx-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.i686.rpm");
    List<String> jdk8F30noarch = Arrays.asList(
            "java-1.8.0-openjdk-javadoc-1.8.0.242.b08-0.fc30.noarch.rpm",
            "java-1.8.0-openjdk-javadoc-zip-1.8.0.242.b08-0.fc30.noarch.rpm");
    List<String> jdk8F30ppc64le = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.ppc64le.rpm");
    List<String> jdk8F30s390x = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.s390x.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.s390x.rpm");
    List<String> jdk8F30x86_64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-openjfx-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-openjfx-devel-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-openjfx-devel-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-openjfx-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-0.fc30.x86_64.rpm");
    List<String> jdk11F31src = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.src.rpm");
    List<String> jdk11F31aarch64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-0.fc31.aarch64.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-0.fc31.aarch64.rpm");
    List<String> jdk11F31armv7hl = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.armv7hl.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.armv7hl.rpm");
    List<String> jdk11F31i686 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-0.fc31.i686.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-0.fc31.i686.rpm");
    List<String> jdk11F31ppc64le = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-0.fc31.ppc64le.rpm");
    List<String> jdk11F31s390x = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-0.fc31.s390x.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-0.fc31.s390x.rpm");
    List<String> jdk11F31x86_64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-demo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-devel-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-headless-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-src-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-0.fc31.x86_64.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-0.fc31.x86_64.rpm");

    List<String> jdk13F31src = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.src.rpm");
    List<String> jdk13F31aarch64 = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-demo-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-devel-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-headless-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-jmods-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-src-slowdebug-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-devel-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-headless-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm",
            "java-latest-openjdk-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.aarch64.rpm");
    List<String> jdk13F31armv7hl = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.armv7hl.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.armv7hl.rpm");
    List<String> jdk13F31i686 = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-demo-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-devel-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-headless-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-jmods-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-src-slowdebug-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-devel-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-headless-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm",
            "java-latest-openjdk-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.i686.rpm");
    List<String> jdk13F31ppc64le = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-demo-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-devel-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-headless-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-jmods-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-src-slowdebug-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-devel-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-headless-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm",
            "java-latest-openjdk-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.ppc64le.rpm");
    List<String> jdk13F31s390x = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-demo-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-devel-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-headless-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-jmods-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-src-slowdebug-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-devel-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-headless-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm",
            "java-latest-openjdk-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.s390x.rpm");
    List<String> jdk13F31x86_64 = Arrays.asList(
            "java-latest-openjdk-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-demo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-demo-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-devel-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-devel-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-headless-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-headless-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-javadoc-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-javadoc-zip-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-jmods-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-jmods-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-src-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-src-slowdebug-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-debugsource-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-devel-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-devel-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-headless-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-headless-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm",
            "java-latest-openjdk-slowdebug-debuginfo-13.0.2.8-1.rolling.fc31.x86_64.rpm");
    List<String> jdk11WinZips = Arrays.asList(
            "java-11-openjdk-11.0.26.0.4-1.win.debuginfo.x86_64.zip",
            "java-11-openjdk-11.0.26.0.4-1.win.jdk.x86_64.zip",
            "java-11-openjdk-11.0.26.0.4-1.win.jre.x86_64.zip",
            "java-11-openjdk-11.0.26.0.4-1.win.redhat.src.zip",
            "java-11-openjdk-11.0.26.0.4-1.win-sources.zip",
            "java-11-openjdk-11.0.26.0.4-1.win.src.zip",
            "java-11-openjdk-11.0.26.0.4-1.win.test.x86_64.zip",
            "java-11-openjdk-fastdebug-11.0.26.0.4-1.win.jdk.x86_64.zip",
            "signing-logs.zip",
            "unsigned_java-11-openjdk-11.0.26.0.4-1.win.jdk.x86_64.zip",
            "unsigned_java-11-openjdk-11.0.26.0.4-1.win.jre.x86_64.zip",
            "unsigned_java-11-openjdk-fastdebug-11.0.26.0.4-1.win.jdk.x86_64.zip");
    List<String> jdk8WinZips = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.debuginfo.x86_64.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.jdk.x86_64.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.jre.x86_64.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.redhat.src.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win-sources.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.src.zip",
            "java-1.8.0-openjdk-1.8.0.442.b06-2.win.testsrc.zip",
            "signing-logs.zip",
            "unsigned_java-1.8.0-openjdk-1.8.0.442.b06-2.win.jdk.x86_64.zip",
            "unsigned_java-1.8.0-openjdk-1.8.0.442.b06-2.win.jre.x86_64.zip",
            "unsigned_java-1.8.0-openjdk-1.8.0.442.b06-2.win.x86_64.installer_bundle.zip",
            "java-1.8.0-openjdk-jre-1.8.0.242-3.b08.redhat.windows.x86.zip");
    List<String> jdk17WinZips = Arrays.asList(
            "java-17-openjdk-17.0.14.0.7-1.win.debuginfo.x86_64.zip",
            "java-17-openjdk-17.0.14.0.7-1.win.jdk.x86_64.zip",
            "java-17-openjdk-17.0.14.0.7-1.win.jre.x86_64.zip",
            "java-17-openjdk-17.0.14.0.7-1.win.redhat.src.zip",
            "java-17-openjdk-17.0.14.0.7-1.win-sources.zip",
            "java-17-openjdk-17.0.14.0.7-1.win.src.zip",
            "java-17-openjdk-17.0.14.0.7-1.win.test.x86_64.zip",
            "java-17-openjdk-fastdebug-17.0.14.0.7-1.win.jdk.x86_64.zip",
            "signing-logs.zip",
            "unsigned_java-17-openjdk-17.0.14.0.7-1.win.jdk.x86_64.zip",
            "unsigned_java-17-openjdk-17.0.14.0.7-1.win.jre.x86_64.zip",
            "unsigned_java-17-openjdk-fastdebug-17.0.14.0.7-1.win.jdk.x86_64.zip");
    List<String> jdk21WinZips = Arrays.asList(
            "java-21-openjdk-21.0.6.0.7-1.win.debuginfo.x86_64.zip",
            "java-21-openjdk-21.0.6.0.7-1.win.jdk.x86_64.zip",
            "java-21-openjdk-21.0.6.0.7-1.win.jre.x86_64.zip",
            "java-21-openjdk-21.0.6.0.7-1.win.patches.zip",
            "java-21-openjdk-21.0.6.0.7-1.win-sources.zip",
            "java-21-openjdk-21.0.6.0.7-1.win.src.zip",
            "java-21-openjdk-21.0.6.0.7-1.win.test.x86_64.zip",
            "java-21-openjdk-fastdebug-21.0.6.0.7-1.win.jdk.x86_64.zip",
            "signing-logs.zip",
            "unsigned_java-21-openjdk-21.0.6.0.7-1.win.jdk.x86_64.zip",
            "unsigned_java-21-openjdk-21.0.6.0.7-1.win.jre.x86_64.zip",
            "unsigned_java-21-openjdk-fastdebug-21.0.6.0.7-1.win.jdk.x86_64.zip"
    );

    List<String> jdk11El7src = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.src.rpm");
    List<String> jdk11El7aarch64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.aarch64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.aarch64.rpm");
    List<String> jdk11El7i686 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.i686.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.i686.rpm");
    List<String> jdk11El7ppc = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.ppc.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.ppc.rpm");
    List<String> jdk11El7ppc64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.ppc64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.ppc64.rpm");
    List<String> jdk11El7ppc64le = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.ppc64le.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.ppc64le.rpm");
    List<String> jdk11El7s390 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.s390.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.s390.rpm");
    List<String> jdk11El7s390x = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.s390x.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.s390x.rpm");
    List<String> jdk11El7x86_64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-demo-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-demo-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-devel-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-devel-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-headless-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-headless-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-javadoc-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-javadoc-zip-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-jmods-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-src-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-src-debug-11.0.6.10-3.el7.x86_64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-3.el7.x86_64.rpm");

    List<String> jdk11El8src = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.src.rpm");
    List<String> jdk11El8aarch64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-demo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-devel-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-headless-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-src-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-2.el8.aarch64.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-2.el8.aarch64.rpm");
    List<String> jdk11El8i686 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-demo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-devel-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-headless-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-jmods-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-src-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-2.el8.i686.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-2.el8.i686.rpm");
    List<String> jdk11El8ppc64le = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-demo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-devel-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-headless-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-jmods-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-src-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-2.el8.ppc64le.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-2.el8.ppc64le.rpm");
    List<String> jdk11El8s390x = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-demo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-devel-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-headless-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-jmods-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-src-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-2.el8.s390x.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-2.el8.s390x.rpm");
    List<String> jdk11El8x86_64 = Arrays.asList(
            "java-11-openjdk-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-demo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-demo-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-devel-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-devel-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-headless-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-headless-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-javadoc-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-javadoc-zip-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-jmods-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-jmods-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-src-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-src-slowdebug-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-debuginfo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-debugsource-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-devel-debuginfo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-devel-slowdebug-debuginfo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-headless-debuginfo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-headless-slowdebug-debuginfo-11.0.6.10-2.el8.x86_64.rpm",
            "java-11-openjdk-slowdebug-debuginfo-11.0.6.10-2.el8.x86_64.rpm");

    List<String> jdk7El7src = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.src.rpm");
    List<String> jdk7El7aarch64 = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.aarch64.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.aarch64.rpm");
    List<String> jdk7El7i686 = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.i686.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.i686.rpm");
    List<String> jdk7El7noarch = Arrays.asList(
            "java-1.7.0-openjdk-javadoc-1.7.0.251-2.6.21.1.el7.noarch.rpm");
    List<String> jdk7El7ppc = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.ppc.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.ppc.rpm");
    List<String> jdk7El7ppc64 = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.ppc64.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.ppc64.rpm");
    List<String> jdk7El7ppc64le = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.ppc64le.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.ppc64le.rpm");
    List<String> jdk7El7s390 = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.s390.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.s390.rpm");
    List<String> jdk7El7s390x = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.s390x.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.s390x.rpm");
    List<String> jdk7El7x86_64 = Arrays.asList(
            "java-1.7.0-openjdk-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-accessibility-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-demo-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-devel-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-headless-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-src-1.7.0.251-2.6.21.1.el7.x86_64.rpm",
            "java-1.7.0-openjdk-debuginfo-1.7.0.251-2.6.21.1.el7.x86_64.rpm");
    List<String> jdk8El8src = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.src.rpm");
    List<String> jdk8El8aarch64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-1.el8.aarch64.rpm");
    List<String> jdk8El8i686 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-1.el8.i686.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-1.el8.i686.rpm");
    List<String> jdk8El8noarch = Arrays.asList(
            "java-1.8.0-openjdk-javadoc-1.8.0.242.b08-1.el8.noarch.rpm",
            "java-1.8.0-openjdk-javadoc-zip-1.8.0.242.b08-1.el8.noarch.rpm");
    List<String> jdk8El8ppc64le = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-1.el8.ppc64le.rpm");
    List<String> jdk8El8s390x = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-1.el8.s390x.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-1.el8.s390x.rpm");
    List<String> jdk8El8x86_64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-src-slowdebug-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-debugsource-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-demo-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-demo-slowdebug-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-devel-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-devel-slowdebug-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-headless-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-headless-slowdebug-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm",
            "java-1.8.0-openjdk-slowdebug-debuginfo-1.8.0.242.b08-1.el8.x86_64.rpm");
    List<String> jdk8El7src = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.src.rpm");
    List<String> jdk8El7aarch64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-accessibility-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-demo-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-devel-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-headless-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-src-debug-1.8.0.242.b08-1.el7.aarch64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.aarch64.rpm");
    List<String> jdk8El7i686 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-accessibility-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-demo-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-devel-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-headless-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-src-debug-1.8.0.242.b08-1.el7.i686.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.i686.rpm");
    List<String> jdk8El7noarch = Arrays.asList(
            "java-1.8.0-openjdk-javadoc-1.8.0.242.b08-1.el7.noarch.rpm",
            "java-1.8.0-openjdk-javadoc-debug-1.8.0.242.b08-1.el7.noarch.rpm",
            "java-1.8.0-openjdk-javadoc-zip-1.8.0.242.b08-1.el7.noarch.rpm",
            "java-1.8.0-openjdk-javadoc-zip-debug-1.8.0.242.b08-1.el7.noarch.rpm");
    List<String> jdk8El7ppc = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.ppc.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.ppc.rpm");
    List<String> jdk8El7ppc64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-accessibility-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-demo-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-devel-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-headless-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-src-debug-1.8.0.242.b08-1.el7.ppc64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.ppc64.rpm");
    List<String> jdk8El7ppc64le = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-accessibility-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-demo-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-devel-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-headless-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-src-debug-1.8.0.242.b08-1.el7.ppc64le.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.ppc64le.rpm");
    List<String> jdk8El7s390 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.s390.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.s390.rpm");
    List<String> jdk8El7s390x = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.s390x.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.s390x.rpm");
    List<String> jdk8El7x86_64 = Arrays.asList(
            "java-1.8.0-openjdk-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-accessibility-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-demo-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-demo-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-devel-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-devel-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-headless-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-headless-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-src-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-src-debug-1.8.0.242.b08-1.el7.x86_64.rpm",
            "java-1.8.0-openjdk-debuginfo-1.8.0.242.b08-1.el7.x86_64.rpm");

    List<String> portable11src = Arrays.asList("java-11-openjdk-11.0.7.10-0.el6openjdkportable.src.rpm");

    List<String> portable11x64 = Arrays.asList("java-11-openjdk-11.0.7.10-0.el6openjdkportable.x86_64.rpm",
            "java-11-openjdk-portable-11.0.7.10-0.el6openjdkportable.x86_64.rpm",
            "java-11-openjdk-portable-debug-11.0.7.10-0.el6openjdkportable.x86_64.rpm",
            "java-11-openjdk-portable-devel-11.0.7.10-0.el6openjdkportable.x86_64.rpm",
            "java-11-openjdk-portable-devel-debug-11.0.7.10-0.el6openjdkportable.x86_64.rpm");


    List<String> portable8src = Arrays.asList("java-1.8.0-openjdk-1.8.0.252.b09-0.el6openjdkportable.src.rpm");

    List<String> portable8x64 = Arrays.asList("java-1.8.0-openjdk-1.8.0.252.b09-0.el6openjdkportable.x86_64.rpm",
            "java-1.8.0-openjdk-portable-1.8.0.252.b09-0.el6openjdkportable.x86_64.rpm",
            "java-1.8.0-openjdk-portable-debug-1.8.0.252.b09-0.el6openjdkportable.x86_64.rpm",
            "java-1.8.0-openjdk-portable-devel-1.8.0.252.b09-0.el6openjdkportable.x86_64.rpm",
            "java-1.8.0-openjdk-portable-devel-debug-1.8.0.252.b09-0.el6openjdkportable.x86_64.rpm");

    List<String> portableEl8src = Arrays.asList("java-21-openjdk-portable-21.0.0.0.35-2.el8.src.rpm");
    List<String> portableEl8x64 = Arrays.asList("java-21-openjdk-portable-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-devel-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-devel-fastdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-devel-slowdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-docs-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-fastdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-misc-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-slowdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-static-libs-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-static-libs-fastdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-static-libs-slowdebug-21.0.0.0.35-2.el8.x86_64.rpm",
            "java-21-openjdk-portable-unstripped-21.0.0.0.35-2.el8.x86_64.rpm");

    private static class BlackWhiteLister {

        private final List<String> blackList;
        private final List<String> whiteList;
        private final String name;
        public StringBuilder looger;

        public String getName() {
            return name;
        }

        public static BlackWhiteLister build(BlackWhiteLister... toJoin) {
            String name = "";
            final List<String> blackList = new ArrayList<>();
            final List<String> whiteList = new ArrayList<>();
            for (BlackWhiteLister blackWhiteLister : toJoin) {
                name = name + "-" + blackWhiteLister.getName();
                if (!isSkipped(blackWhiteLister.blackList)) {
                    blackList.addAll(blackWhiteLister.blackList);
                }
                if (!isSkipped(blackWhiteLister.whiteList)) {
                    whiteList.addAll(blackWhiteLister.whiteList);
                }
            }
            name = name.replaceFirst("-", "");
            return new BlackWhiteLister(name, new ArrayList<>(new HashSet<>(blackList)), new ArrayList<>(new HashSet<>(whiteList)));
        }

        public BlackWhiteLister(String name, String[] blackList, String[] whiteList) {
            this.blackList = Collections.unmodifiableList(Arrays.asList(blackList));
            this.whiteList = Collections.unmodifiableList(Arrays.asList(whiteList));
            this.name = name;
        }

        public BlackWhiteLister(String name, List<String> blackList, List<String> whiteList) {
            this.blackList = Collections.unmodifiableList(blackList);
            this.whiteList = Collections.unmodifiableList(whiteList);
            this.name = name;
        }

        private static boolean isSkipped(List<String> l) {
            return l == null || l.isEmpty() || (l.size() == 1 && l.get(0).trim().isEmpty());
        }

        public List<String> removeBlacklisted(String[] s) {
            return removeBlacklisted(Arrays.asList(s));
        }

        public List<String> removeBlacklisted(List<String> s) {
            List<String> r = new ArrayList<>(s);
            if (isSkipped(blackList)) {
                if (looger != null) {
                    looger.append(s.size() + " allowed by missing balcklist \n");
                }
                return r;
            }
            for (String w : blackList) {
                for (int i = 0; i < r.size(); i++) {
                    if (r.get(i).matches(w)) {
                        if (looger != null) {
                            looger.append(r.get(i) + " removed by " + w + "\n");
                        }
                        r.remove(i);
                        i--;
                    }
                }
            }
            return r;
        }

        public List<String> allowJustWhitelisted(String[] s) {
            return allowJustWhitelisted(Arrays.asList(s));
        }

        public List<String> allowJustWhitelisted(List<String> s) {
            if (isSkipped(whiteList)) {
                if (looger != null) {
                    looger.append(s.size() + " allowed by missing whitelist \n");
                }
                return new ArrayList<>(s);
            }
            Set<String> r = new HashSet<>(s.size());
            for (int i = 0; i < s.size(); i++) {
                for (String w : whiteList) {
                    if (s.get(i).matches(w)) {
                        r.add(s.get(i));
                        if (looger != null) {
                            looger.append(s.get(i) + " allowed by " + w + "\n");
                        }
                    }
                }
            }
            return new ArrayList<>(r);
        }

        public List<String> match(String[] s) {
            return match(Arrays.asList(s));
        }

        public List<String> match(List<String> s) {
            //first balcklist, and then whitelist
            return allowJustWhitelisted(removeBlacklisted(s));
        }

        public String listsToString() {
            return "  B: [" + String.join(" ", blackList) + "]\n"
                    + "  W: [" + String.join(" ", whiteList) + "]";
        }
    }

    @Test
    public void bwlWorks() {
        BlackWhiteLister bwl;
        bwl = new BlackWhiteLister("test", new String[]{"aa", "bb"}, new String[]{"zz", "yy"});
        List<String> e = bwl.removeBlacklisted(new String[]{"aa", "bb", "aa"});
        System.out.println(Arrays.toString(e.toArray()));
        Assertions.assertArrayEquals(new String[]{}, e.toArray());

        bwl = new BlackWhiteLister("test", new String[]{"aa", "bb"}, new String[]{"zz", "yy"});
        List<String> o = bwl.removeBlacklisted(new String[]{"kkk", "aa", "bb", "ooo", "aa", "zz", "yy", "mmm"});
        System.out.println(Arrays.toString(o.toArray()));
        Assertions.assertEquals(new String[]{"kkk", "ooo", "zz", "yy", "mmm"}, o.toArray());

        List<String> oo = bwl.allowJustWhitelisted(new String[]{"kkk", "aa", "bb", "ooo", "aa", "zz", "yy", "mmm", "zz"});
        System.out.println(Arrays.toString(oo.toArray()));
        Assertions.assertEquals(new String[]{"zz", "yy"}, oo.toArray());

        BlackWhiteLister onlyW = new BlackWhiteLister("test", new String[]{}, new String[]{"aa"});
        List<String> a = onlyW.match(new String[]{"aa"});
        System.out.println(Arrays.toString(a.toArray()));
        Assertions.assertEquals(new String[]{"aa"}, a.toArray());

        BlackWhiteLister onlyB = new BlackWhiteLister("test", new String[]{"aa"}, new String[]{""});
        List<String> b = onlyB.match(new String[]{"aa", "bb"});
        System.out.println(Arrays.toString(b.toArray()));
        Assertions.assertEquals(new String[]{"bb"}, b.toArray());
    }

    public OToolServiceRun() {
        winZips.put("jdk21WinZips", jdk21WinZips);
        winZips.put("jdk17WinZips", jdk17WinZips);
        winZips.put("jdk11WinZips", jdk11WinZips);
        winZips.put("jdk8WinZips", jdk8WinZips);
        rpms.put("F30aarch64", jdk8F30aarch64);
        rpms.put("jdk8F30armv7hl", jdk8F30armv7hl);
        rpms.put("jdk8F30i686", jdk8F30i686);
        rpms.put("jdk8F30ppc64le", jdk8F30ppc64le);
        rpms.put("jdk8F30s390x", jdk8F30s390x);
        rpms.put("jdk8F30x86_64", jdk8F30x86_64);
        rpms.put("jdk11F31aarch64", jdk11F31aarch64);
        rpms.put("jdk11F31armv7hl", jdk11F31armv7hl);
        rpms.put("jdk11F31i686", jdk11F31i686);
        rpms.put("jdk11F31s390x", jdk11F31s390x);
        rpms.put("jdk11F31x86_64", jdk11F31x86_64);
        rpms.put("jdk11F31ppc64le", jdk11F31ppc64le);
        rpms.put("jdk13F31aarch64", jdk13F31aarch64);
        rpms.put("jdk13F31armv7hl", jdk13F31armv7hl);
        rpms.put("jdk13F31i686", jdk13F31i686);
        rpms.put("jdk13F31s390x", jdk13F31s390x);
        rpms.put("jdk13F31x86_64", jdk13F31x86_64);
        rpms.put("jdk13F31ppc64le", jdk13F31ppc64le);
        rpms.put("jdk8El7i686", jdk8El7i686);
        rpms.put("jdk8El7ppc64le", jdk8El7ppc64le);
        rpms.put("jdk8El7s390x", jdk8El7s390x);
        rpms.put("jdk8El7x86_64", jdk8El7x86_64);
        rpms.put("jdk11El7aarch64", jdk11El7aarch64);
        rpms.put("jdk11El7i686", jdk11El7i686);
        rpms.put("jdk11El7s390x", jdk11El7s390x);
        rpms.put("jdk11El7x86_64", jdk11El7x86_64);
        rpms.put("jdk11El7ppc64le", jdk11El7ppc64le);
        rpms.put("jdk7El7i686", jdk7El7i686);
        rpms.put("jdk7El7ppc64le", jdk7El7ppc64le);
        rpms.put("jdk7El7s390x", jdk7El7s390x);
        rpms.put("jdk7El7x86_64", jdk7El7x86_64);
        rpms.put("jdk8El8i686", jdk8El8i686);
        rpms.put("jdk8El8ppc64le", jdk8El8ppc64le);
        rpms.put("jdk8El8s390x", jdk8El8s390x);
        rpms.put("jdk8El8x86_64", jdk8El8x86_64);
        rpms.put("jdk11El8aarch64", jdk11El8aarch64);
        rpms.put("jdk11El8i686", jdk11El8i686);
        rpms.put("jdk11El8s390x", jdk11El8s390x);
        rpms.put("jdk11El8x86_64", jdk11El8x86_64);
        rpms.put("jdk11El8ppc64le", jdk11El8ppc64le);

        containers.put("container", container);

        portable.put("portable11x64", portable11x64);
        portable.put("portable8x64", portable8x64);
        portable.put("portableEl8x64", portableEl8x64);
    }

    BlackWhiteLister sdk = new BlackWhiteLister("sdk",
            new String[]{".*-jre-.*windows.*", ".*jre.win.*", ".*-portable-[b\\d\\.\\-ea]{3,}el.*", ".*-portable-fastdebug-[b\\d\\.\\-ea]{3,}el.*", ".*-portable-slowdebug-[b\\d\\.\\-ea]{3,}el.*"},
            new String[]{});
    BlackWhiteLister jre = new BlackWhiteLister("jre",
            new String[]{".*-devel-.*", ".*-jmods-.*", ".*-static-libs-.*", ".*-openjdk[b\\d\\.\\-]{3,}(ea.windows.redhat|ea.redhat.windows).*", ".*\.jdk\..*"},
            new String[]{});
    BlackWhiteLister jreHeadless = new BlackWhiteLister("jreHeadless",
            new String[]{".*windows.*", ".*debuginfo.*"},
            new String[]{".*-headless.*"});
    BlackWhiteLister allDebugRelease = new BlackWhiteLister("allDebugRelease", new String[]{}, new String[]{});
    BlackWhiteLister release = new BlackWhiteLister("release",
            new String[]{".*-fastdebug-.*", ".*-slowdebug-.*", ".*-debug-.*", ".*-unstripped-.*"},
            new String[]{""});
    BlackWhiteLister all = new BlackWhiteLister("all",
            new String[]{},
            new String[]{});
    BlackWhiteLister fasdebug = new BlackWhiteLister("fastdebug",
            new String[]{".*-slowdebug-.*", ".*-debug-.*", ".*-openjdk[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*", ".*-openjdk-jre[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*", "(?!.*(-fastdebug-|-debuginfo-).*).*", ".*-unstripped-.*"},
            new String[]{});
    BlackWhiteLister slowdebug = new BlackWhiteLister("slowdebug",
            new String[]{".*-fastdebug-.*", ".*-openjdk[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*", ".*-openjdk-jre[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*","(?!.*(-slowdebug-|-debug-|-debuginfo-).*).*", ".*-unstripped-.*"},
            new String[]{});
    BlackWhiteLister unstripped = new BlackWhiteLister("unstripped",
            new String[]{".*-fastdebug-.*", ".*-slowdebug-.*", ".*-debug-.*", ".*-openjdk[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*", ".*-openjdk-jre[b\\d\\.\\-]{3,}(windows.redhat|redhat.windows).*", ".*-openjdk-portable[b\\d\\.\\-]{3,}.*", ".*-openjdk-portable-devel[b\\d\\.\\-]{3,}.*"},
            new String[]{});
    BlackWhiteLister containersLists = new BlackWhiteLister("containers", new String[]{}, new String[]{});
    BlackWhiteLister portableLists = new BlackWhiteLister("portable", new String[]{"java-[\\d\\.]{2,5}-openjdk-[b\\d\\.\\-ea]{3,}el6openjdkportable.*"}, new String[]{});
    BlackWhiteLister rpmsLists = new BlackWhiteLister("rpms", new String[]{".*accessibility.*", ".*src.*", ".*demo.*", ".*openjfx.*"}, new String[]{});
    BlackWhiteLister win64ZipsLists = new BlackWhiteLister("win64zips",
            new String[]{".*txt.*", ".*openjfx.*", ".*\\.msi$", ".*\\.json$", ".*(redhat.windows|windows.redhat).x86.zip$"},
            new String[]{".*x86_64.zip$", ".*static.*"});
    BlackWhiteLister nonDebuginfoSuite = new BlackWhiteLister("nonDebuginfoSuite",
            new String[]{".*-debuginfo-.*", ".*-debugsource-.*"},
            new String[]{});
    BlackWhiteLister debuginfoSuite = new BlackWhiteLister("debuginfoSuite", new String[]{}, new String[]{});

    BlackWhiteLister[] jresdk = new BlackWhiteLister[]{jreHeadless, jre, sdk};
    BlackWhiteLister[] debugMode = new BlackWhiteLister[]{all, release, unstripped, fasdebug, slowdebug};
    BlackWhiteLister[] suites = new BlackWhiteLister[]{nonDebuginfoSuite, debuginfoSuite};

    @Test
    //for paste to json file, you can copy the declarations itself
    public void printListsFrOtoolGui() {
        for (BlackWhiteLister j : jresdk) {
            System.out.println(" ** " + j.getName());
            System.out.println(j.listsToString());
        }
        for (BlackWhiteLister d : debugMode) {
            System.out.println(" ** " + d.getName());
            System.out.println(d.listsToString());
        }
        for (BlackWhiteLister s : suites) {
            System.out.println(" ** " + s.getName());
            System.out.println(s.listsToString());
        }
        System.out.println(" ** " + containersLists.getName());
        System.out.println(containersLists.listsToString());
        System.out.println(" ** " + win64ZipsLists.getName());
        System.out.println(win64ZipsLists.listsToString());
        System.out.println(" ** " + rpmsLists.getName());
        System.out.println(rpmsLists.listsToString());
        System.out.println(" ** " + portableLists.getName());
        System.out.println(portableLists.listsToString());

    }

    @Test
    public void regexgame() {
        for (Map.Entry<String, List<String>> e : containers.entrySet()) {
            for (BlackWhiteLister j : jresdk) {
                for (BlackWhiteLister d : debugMode) {
                    for (BlackWhiteLister s : suites) {
                        BlackWhiteLister bl = BlackWhiteLister.build(j, d, s, containersLists);
                        List<String> r = checkMatch(bl, e.getValue(), e.getKey());
                        Assertions.assertTrue(r.size() >= 0 && r.size() <= 1);
                    }
                }
            }
        }
        for (Map.Entry<String, List<String>> e : winZips.entrySet()) {
            for (BlackWhiteLister j : jresdk) {
                for (BlackWhiteLister d : debugMode) {
                    for (BlackWhiteLister s : suites) {
                        BlackWhiteLister bl = BlackWhiteLister.build(j, d, s, win64ZipsLists);
                        List<String> r = checkMatch(bl, e.getValue(), e.getKey());
                        if (d == all){
                            //currenlty nothing intereesting
                        } else {
                            Assertions.assertTrue(r.size() >= 0 && r.size() <= 1);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, List<String>> e : rpms.entrySet()) {
            for (BlackWhiteLister j : jresdk) {
                for (BlackWhiteLister d : debugMode) {
                    for (BlackWhiteLister s : suites) {
                        BlackWhiteLister bl = BlackWhiteLister.build(j, d, s, rpmsLists);
                        checkMatch(bl, e.getValue(), e.getKey());
                    }
                }
            }
        }
    }

    @Test
    public void regexgamePortable() {
        for (Map.Entry<String, List<String>> e : portable.entrySet()) {
            for (BlackWhiteLister j : jresdk) {
                for (BlackWhiteLister d : debugMode) {
                    for (BlackWhiteLister s : suites) {
                        BlackWhiteLister bl = BlackWhiteLister.build(j, d, s, portableLists);
                        checkMatch(bl, e.getValue(), e.getKey());
                    }
                }
            }
        }


    }

    private List<String> checkMatch(BlackWhiteLister bl, List<String> orig, String id) {
        bl.looger = new StringBuilder();
        List<String> r = bl.match(orig);
        System.out.println(" ** " + bl.getName() + " x " + id);
//        System.out.println(bl.listsToString());
//        System.out.print(bl.looger.toString());
        System.out.println(" =>  (" + r.size() + ")" + r.toString());
        bl.looger = null;
        return r;
    }
}
