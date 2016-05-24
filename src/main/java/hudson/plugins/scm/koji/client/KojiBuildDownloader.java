package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.scm.koji.BuildsSerializer;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiBuildDownloadResult;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.plugins.scm.koji.model.RPM;
import hudson.remoting.VirtualChannel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jenkinsci.remoting.RoleChecker;

import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.plugins.scm.koji.LoggerHelp;
import java.net.InetAddress;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiBuildDownloader implements FilePath.FileCallable<KojiBuildDownloadResult>, LoggerHelp{

    private static final Logger LOG = LoggerFactory.getLogger(KojiSCM.class);
    private final KojiScmConfig config;
    private final Predicate<String> notProcessedNvrPredicate;
    private TaskListener currentListener;
    private final boolean verbose = true;

    public KojiBuildDownloader(KojiScmConfig config, Predicate<String> notProcessedNvrPredicate) {
        this.config = config;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
    }

    @Override
    public KojiBuildDownloadResult invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        File checkoutBuildFile = new File(workspace, BUILD_XML);
        Build build = new BuildsSerializer().read(checkoutBuildFile);
        if (build == null) {
            // if we are here - it is the first build ever,
            // have to pull the koji and download whatever we'll find:
            build = new KojiListBuilds(config, notProcessedNvrPredicate).invoke(workspace, channel);
            if (build == null) {
                // if we are here - no remote changes on first build, exiting:
                return null;
            }
        }
        // we got the build info in workspace, downloading:
        File targetDir = workspace;
        if (config.getDownloadDir() != null && config.getDownloadDir().length() > 0) {
            // target dir was specified,
            targetDir = new File(targetDir, config.getDownloadDir());
            // checkbox for dir per nvr was specified:
            if (config.isDirPerNvr()) {
                targetDir = new File(targetDir, build.getNvr());
            }
            // do not delete the workspace dir if user specified '.' :
            if (!targetDir.getAbsoluteFile().equals(workspace.getAbsoluteFile()) && targetDir.exists() && config.isCleanDownloadDir()) {
                cleanDirRecursively(targetDir);
            }
            targetDir.mkdirs();
        }
        List<String> rpmFiles = downloadRPMs(targetDir, build);
        return new KojiBuildDownloadResult(build, targetDir.getAbsolutePath(), rpmFiles);
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

    private List<String> downloadRPMs(File targetDir, Build build) {
        Predicate<RPM> nvrPredicate = i -> true;
        if (config.getExcludeNvr() != null && !config.getExcludeNvr().isEmpty()) {
            GlobPredicate glob = new GlobPredicate(config.getExcludeNvr());
            nvrPredicate = rpm -> !glob.test(rpm.getNvr());
        }
        return build.getRpms()
                .stream()
                .parallel()
                .filter(nvrPredicate)
                .map(r -> downloadRPM(targetDir, build, r))
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
    }

    private File downloadRPM(File targetDir, Build build, RPM rpm) {
        try {
            String urlString = composeUrl(build, rpm);
            log(InetAddress.getLocalHost().getHostName());
            log(new Date().toString());
            log("Downloading: ",urlString);
            File targetFile = new File(targetDir, rpm.getNvr() + '.' + rpm.getArch() + ".rpm");
            log("To: ",targetFile);
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                    InputStream in = httpDownloadStream(urlString)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return targetFile;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while downloading RPM", ex);
        }
    }

    private InputStream httpDownloadStream(String urlString) {
        HttpURLConnection httpConn = null;
        boolean keepConnection = false;
        for (int i = 0; i < 50; i++) {
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
                    case 301: {
                        String location = httpConn.getHeaderField("Location");
                        if (location == null || location.isEmpty()) {
                            throw new Exception("Invalid Location header for response 301");
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

    private String composeUrl(Build build, RPM rpm) {
        String kojiDownloadUrl = config.getKojiDownloadUrl();
        StringBuilder sb = new StringBuilder(255);
        sb.append(kojiDownloadUrl);
        if (kojiDownloadUrl.charAt(kojiDownloadUrl.length() - 1) != '/') {
            sb.append('/');
        }
        sb.append(build.getName()).append('/');
        sb.append(build.getVersion()).append('/');
        sb.append(build.getRelease()).append('/');
        sb.append(rpm.getArch()).append('/');
        sb.append(rpm.getNvr()).append('.');
        sb.append(rpm.getArch()).append(".rpm");
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
}
