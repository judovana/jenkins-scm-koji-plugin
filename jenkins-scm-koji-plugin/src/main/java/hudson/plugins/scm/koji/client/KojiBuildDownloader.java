package hudson.plugins.scm.koji.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.scm.koji.FakeKojiXmlRpcApi;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiXmlRpcApi;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiBuildDownloadResult;
import hudson.plugins.scm.koji.model.RPM;
import hudson.remoting.VirtualChannel;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.plugins.scm.koji.LoggerHelp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Date;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListArchives;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListRPMs;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.jenkinsci.remoting.RoleChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.DatatypeConverter;

public class KojiBuildDownloader implements FilePath.FileCallable<KojiBuildDownloadResult>, LoggerHelp, TaskListenerLogTransporter {

    private static final Logger LOG = LoggerFactory.getLogger(KojiSCM.class);
    private static final int MAX_REDIRECTIONS = 10;
    private static final int BUFFER_SIZE = 8192;

    private final Iterable<KojiBuildProvider> kojiBuildProviders;
    private final KojiXmlRpcApi kojiXmlRpcApi;
    private final Predicate<String> notProcessedNvrPredicate;
    private TaskListener currentListener;
    private final boolean verbose = true;
    private Build build;
    private final String downloadDir;
    private final int maxPreviousBuilds;
    private final boolean cleanDownloadDir;
    private final boolean dirPerNvr;

    public KojiBuildDownloader(
            Iterable<KojiBuildProvider> kojiBuildProviders,
            KojiXmlRpcApi kojiXmlRpcApi,
            Predicate<String> notProcessedNvrPredicate,
            Build build,
            String downloadDir,
            int maxPreviousBuilds,
            boolean cleanDownloadDir,
            boolean dirPerNvr
    ) {
        this.kojiBuildProviders = kojiBuildProviders;
        this.kojiXmlRpcApi = kojiXmlRpcApi;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        this.build = build;
        this.downloadDir = downloadDir;
        this.maxPreviousBuilds = maxPreviousBuilds;
        this.cleanDownloadDir = cleanDownloadDir;
        this.dirPerNvr = dirPerNvr;
    }

    @Override
    public KojiBuildDownloadResult invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        if (build == null) {
            final Build build = new KojiListBuilds(
                    kojiBuildProviders,
                    kojiXmlRpcApi,
                    notProcessedNvrPredicate,
                    maxPreviousBuilds
            ).invoke(workspace, channel);
            if (build == null) {
                // if we are here - no remote changes on first build, exiting:
                return null;
            }
            this.build = build;
        }
        // we got the build info in workspace, downloading:
        File targetDir = workspace;
        if (downloadDir != null && downloadDir.length() > 0) {
            // target dir was specified,
            targetDir = new File(targetDir, downloadDir);
            // checkbox for dir per nvr was specified:
            if (dirPerNvr) {
                targetDir = new File(targetDir, build.getNvr());
            }
            // do not delete the workspace dir if user specified '.' or hardcoded workspace:
            if (!targetDir.getAbsoluteFile().equals(workspace.getAbsoluteFile()) && targetDir.exists() && cleanDownloadDir) {
                if (!build.isManual()) {
                    log("cleaning " + targetDir.toString());
                    cleanDirRecursively(targetDir);
                } else {
                    log("manual tag detected, not cleaning : " + targetDir.toString());
                    String[] l = targetDir.list();
                    if (l == null) {
                        l = new String[]{"Error reading"};
                    }
                    for (String file : l) {
                        log("  " + file);
                    }
                }
            } else {
                log("NOT cleaning " + targetDir.toString() + ":");
                log("" + !targetDir.getAbsoluteFile().equals(workspace.getAbsoluteFile()));
                log("" + targetDir.getAbsoluteFile());
                log("" + workspace.getAbsoluteFile());
                log("" + targetDir.exists());
                log("" + cleanDownloadDir);
            }
            targetDir.mkdirs();
        }
        if (kojiXmlRpcApi instanceof RealKojiXmlRpcApi) {
            final RealKojiXmlRpcApi realKojiXmlRpcApi = (RealKojiXmlRpcApi) kojiXmlRpcApi;
            List<String> rpmFiles = downloadRPMs(targetDir, build, realKojiXmlRpcApi);
            downloadMetadata(new File(targetDir.getAbsolutePath() + "-metadata"), build);
            String srcUrl = "";
            for (String suffix : RPM.Suffix.INSTANCE.getSuffixes()) {
                srcUrl = composeSrcUrl(build.getProvider().getDownloadUrl(), build, suffix);
                if (isUrlReachable(srcUrl)) {
                    build.setSrcUrl(new URL(srcUrl));
                    break;
                }
            }
            // if source file is not found, we try find the directory it might be found in
            if (build.getSrcUrl() == null) {
                URL url = new URL(srcUrl);
                try {
                    // this loop iterates until valid url is found or there is no parent directory anymore
                    // ".." at the end of url indicates there is no parent directory
                    do {
                        URI uri = url.toURI();
                        // https://stackoverflow.com/questions/10159186/how-to-get-parent-url-in-java
                        uri = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
                        url = uri.toURL();
                    } while (!isUrlReachable(url.toString()) && !url.toString().endsWith(".."));
                    build.setSrcUrl(url);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            return new KojiBuildDownloadResult(build, targetDir.getAbsolutePath(), rpmFiles);
        }
        if (kojiXmlRpcApi instanceof FakeKojiXmlRpcApi) {
            final File target = targetDir;
            List<String> rpmPaths = build.getRpms()
                    .stream()
                    .map(rpm -> downloadArchive(target, rpm))
                    .filter(Optional::isPresent)
                    .map(optionalFile -> optionalFile.get().getAbsolutePath())
                    .collect(Collectors.toList());
            log("Downloaded " + rpmPaths.size() + " out of " + build.getRpms().size() + " archives");
            return new KojiBuildDownloadResult(build, target.getAbsolutePath(), rpmPaths);
        }
        return null;
    }

    private Optional<File> downloadArchive(File targetDir, RPM rpm) {
        if (!isUrlReachable(rpm.getUrl())) {
            log("URL " + rpm.getUrl() + " not accessible");
            return Optional.empty();
        }
        File targetFile = new File(targetDir, rpm.getFilename(""));
        log("Starting downloading " + rpm.getUrl());
        try (
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                final InputStream in = httpDownloadStream(rpm.getUrl())
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            log("Exception while downloading " + rpm.getFilename("") + ": ", e);
        }
        log("Download successful");
        rpm.setHashSum(hashSum(targetFile));
        return Optional.of(targetFile);
    }

    private void cleanDirRecursively(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        // if we are still here - we have a directory:
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                cleanDirRecursively(files[i]);
            }
        }
    }


    private void downloadMetadata(File dir, Build build) {
        log("Saving metadata: " + dir.getAbsolutePath());
        if (!dir.exists()) {
            dir.mkdir();
        }
        final String METADATA = "metadata";
        ListRPMs rpmParams = new ListRPMs(build.getId(), null);
        Object[] rpms = saveMetadata(dir, rpmParams);
        ListArchives archivesParams = new ListArchives(build.getId(), null);
        Object[] archives = saveMetadata(dir, archivesParams);
        metaJsonFile(dir, METADATA).delete();
        if (rpms == null || rpms.length == 0) {
            if (archives != null && archives.length > 0) {
                saveMetadata(dir, archives, METADATA);
            }
        } else if (archives == null || archives.length == 0) {
            if (rpms != null && rpms.length > 0) {
                saveMetadata(dir, rpms, METADATA);
            }
        }
        try {
            Files.write(build.getName(), new File(dir, "pkgname.txt"), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log("Exception while saving pkgname metadata" + ex);
        }
        saveMetadata(dir, build, "build");
        log("Saved metadata: " + Arrays.stream(dir.list()).collect(Collectors.joining(",")));
    }

    private Object[] saveMetadata(File dir, XmlRpcRequestParams params) {
        Object allMetadata = null;
        try {
            allMetadata = BuildMatcher.execute(build.getProvider().getTopUrl(), params);
            saveMetadata(dir, allMetadata, params.getMethodName());
        } catch (Exception ex) {
            log("Exception while obtaining " + params.getMethodName() + " metadata" + ex);
        }
        return (Object[]) allMetadata;
    }

    private void saveMetadata(File dir, Object allMetadata, String method) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allMetadata);
            Files.write(json, metaJsonFile(dir, method), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log("Exception while saving " + method + " metadata" + ex);
        }
    }

    private File metaJsonFile(File dir, String s) {
        return new File(dir, s + ".json");
    }

    public List<String> downloadRPMs(File targetDir, Build build, RealKojiXmlRpcApi realKojiXmlRpcApi) {
        Predicate<RPM> nvrPredicate = i -> true;
        final String subpackageBlacklist = realKojiXmlRpcApi.getSubpackageBlacklist();
        if (subpackageBlacklist != null && !subpackageBlacklist.isEmpty()) {
            GlobPredicate glob = new GlobPredicate(subpackageBlacklist, this);
            nvrPredicate = (RPM rpm) -> {
                if (rpm.getArch().equals("src")) {
                    return true;
                } else {
                    log("[KojiSCM] Matching blacklist ...");
                    return !glob.test(rpm.getNvr());
                }
            };
        }

        Predicate<RPM> whitelistPredicate = i -> true;
        final String subpackageWhitelist = realKojiXmlRpcApi.getSubpackageWhitelist();
        if (subpackageWhitelist != null && !subpackageWhitelist.isEmpty()) {
            GlobPredicate glob = new GlobPredicate(subpackageWhitelist, this);
            whitelistPredicate = (RPM rpm) -> {
                if (rpm.getArch().equals("src")){
                    return true;
                } else {
                    log("[KojiSCM] Matching whitelist ...");
                    return glob.test(rpm.getNvr());
                }
            };
        }

        String allPackages = build.getRpms().stream().map(a->a.toString()).collect(Collectors.joining(","));

        List<String> l = build.getRpms()
                .stream()
                .filter(nvrPredicate)
                .filter(whitelistPredicate)
                .map(r -> downloadRPM(targetDir, build, r))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        int rpmsInBuildXml = build.getRpms().size();
        int dwnldedFiles = l.size();
        if (dwnldedFiles == 0) {
            if (rpmsInBuildXml == 0) {
                log("Warning, nothing downloaded, but looks like  nothing should be.");
            } else {
                log("WARNING, nothing downloaded, but should be (" + rpmsInBuildXml + "). Maybe bad exclude packages?");
            }
        }
        return l;
    }

    private File downloadRPM(File targetDir, Build build, RPM rpm) {
        try {
            for (String suffix : RPM.Suffix.INSTANCE.getSuffixes()) {
                String urlString = composeUrl(build.getProvider().getDownloadUrl(), build, rpm, suffix);
                log(InetAddress.getLocalHost().getHostName());
                log(new Date().toString());
                if (build.isManual()) {
                    log("Manual tag provided - skipping download of " + urlString);
                } else {
                    log("Downloading: " + urlString);
                }
                if (!isUrlReachable(urlString)) {
                    log("Not accessible, trying another suffix in: " + rpm.getFilename(suffix));
                    continue;
                }
                rpm.setUrl(urlString);
                File targetFile = new File(targetDir, rpm.getFilename(suffix));
                log("To: " + targetFile);
                if (!build.isManual()) {
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                         InputStream in = httpDownloadStream(urlString)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
                rpm.setHashSum(hashSum(targetFile));
                return targetFile;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while downloading RPM", ex);
        }
        return null;
    }

    private String hashSum(File file) {
        byte[] buffer = new byte[BUFFER_SIZE];
        MessageDigest hashAlgorithm;
        try {
            hashAlgorithm = MessageDigest.getInstance("Md5");
            try (InputStream inputStream = new DigestInputStream(new FileInputStream(file), hashAlgorithm)) {
                while (inputStream.read(buffer) > 0) {
                    ;
                }
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            log("Could not create hash sum of file: " + file.getName(), e);
            return null;
        }
        return DatatypeConverter.printHexBinary(hashAlgorithm.digest()).toLowerCase();
    }

    private InputStream httpDownloadStream(String urlString) {
        HttpURLConnection httpConn = null;
        boolean keepConnection = false;
        for (int i = 0; i < MAX_REDIRECTIONS; i++) {
            try {
                URL url = new URL(urlString);
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("GET");
                int response = httpConn.getResponseCode();
                switch (response) {
                    case 200: {
                        keepConnection = true;
                        return httpConn.getInputStream();
                    }
                    case 301:
                    case 302: {
                        String location = httpConn.getHeaderField("Location");
                        if (location == null || location.isEmpty()) {
                            throw new Exception("Invalid Location header for response " + response);
                        }
                        if (urlString.equals(location)) {
                            throw new Exception("Infinite redirection loop detected for URL: " + urlString);
                        }
                        urlString = location;
                        break;
                    }
                    default:
                        throw new Exception("Unsupported HTTP response " + response + " for URL: " + urlString);
                }
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                if (!keepConnection && httpConn != null) {
                    httpConn.disconnect();
                }
            }
        }
        throw new RuntimeException("Too many redirects for URL: " + urlString);
    }

    private String composeUrl(String kojiDownloadUrl, Build build, RPM rpm, String suffix) {
        StringBuilder sb = new StringBuilder(255);
        sb.append(kojiDownloadUrl);
        if (kojiDownloadUrl.charAt(kojiDownloadUrl.length() - 1) != '/') {
            sb.append('/');
        }
        sb.append(build.getName()).append('/')
        .append(build.getVersion()).append('/')
        .append(build.getRelease()).append('/')
        .append(addArch(rpm)).append('/')
        .append(rpm.getFilename(suffix));
        return sb.toString();
    }

    private String composeSrcUrl(String kojiDownloadUrl, Build build, String suffix) {
        if (kojiDownloadUrl==null || build == null || suffix == null) {
            return "http://unknonw.or/not/found.sorry";
        }
        StringBuilder sb = new StringBuilder(255);
        sb.append(kojiDownloadUrl);
        if (kojiDownloadUrl.charAt(kojiDownloadUrl.length() - 1) != '/') {
            sb.append('/');
        }
        sb.append(build.getName()).append('/');
        sb.append(build.getVersion()).append('/');
        sb.append(build.getRelease()).append('/');
        sb.append("src/");
        sb.append(build.getName()).append('-')
                .append(build.getVersion()).append('-')
                .append(build.getRelease()).append(".src.").append(suffix);
        return sb.toString();
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

    public void setListener(TaskListener listener) {
        this.currentListener = listener;
    }

    private boolean canLog() {
        return (verbose && currentListener != null && currentListener.getLogger() != null);
    }

    private String host() {
        try {
            String h = InetAddress.getLocalHost().getHostName();
            if (h == null) {
                return "null";
            } else {
                return h;
            }
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    void print(String s) {
        try {
            currentListener.getLogger().println(s);
        } catch (Exception ex) {
            LOG.error("During printing of log to TaskListener", ex);
        }
    }

    @Override
    public void log(String s) {
        LOG.info(s);
        if (canLog()) {
            print("[KojiSCM][" + host() + "] " + s);
        }
    }

    @Override
    public void log(String s, Object o) {
        LOG.info(s, o);
        if (canLog()) {
            print("[KojiSCM][" + host() + "] " + s + ": " + o.toString());
        }
    }

    @Override
    public void log(String s, Object... o) {
        LOG.info(s, o);
        if (canLog()) {
            print("[KojiSCM][" + host() + "] " + s);
            for (Object object : o) {
                print("[KojiSCM]   " + object.toString());
            }
        }
    }

    private boolean isUrlReachable(String urlString) {
        try {
            return isUrlReachableImpl(urlString, MAX_REDIRECTIONS);
        } catch (Exception e) {
            LOG.info(e.toString());
            return false;
        }
    }

    private boolean isUrlReachableImpl(String urlString, int redirectionsRemaining) throws MalformedURLException, IOException {
        URL u = new URL(urlString);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        try {
            huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
            huc.connect();
            int code = huc.getResponseCode();
            // http 301=Moved Permanently; 302=Found
            // koji.fedoraproject.org might return both
            if ((code == 301 || code == 302) && redirectionsRemaining > 0) {
                return isUrlReachableImpl(huc.getHeaderField("Location"), redirectionsRemaining - 1);
            }
            return code == 200;
        } finally {
            huc.disconnect();
        }
    }

    private static String addArch(RPM rpm) {
        //it may happen. that this will be necessary to be configurable in koji plugin
        //is container checkbox?
        if (KojiBuildMatcher.isRpmContainer(rpm)) {
            return "images";
        } else {
            return rpm.getArch();
        }
    }

    @Override
    public void println(String s) {
        currentListener.getLogger().println(s);
    }
}
