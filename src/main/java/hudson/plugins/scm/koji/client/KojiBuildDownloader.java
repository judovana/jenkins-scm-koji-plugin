package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.BuildsSerializer;
import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.plugins.scm.koji.model.Build;
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
import java.util.Optional;
import java.util.stream.Collectors;
import org.jenkinsci.remoting.RoleChecker;
import hudson.plugins.scm.koji.WebLog;
import hudson.plugins.scm.koji.model.KojiBuildDownloadResult;
import java.util.function.Predicate;

public class KojiBuildDownloader extends AbstractLoggingWorker implements FilePath.FileCallable<Optional<KojiBuildDownloadResult>> {

    private final KojiScmConfig config;
    private final Predicate<String> notProcessedNvrPredicate;
    private final Predicate<RPM> excludeNvrPredicate;

    public KojiBuildDownloader(WebLog log, KojiScmConfig config, Predicate<String> notProcessedNvrPredicate) {
        super(log);
        this.config = config;
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
        if (config.getExcludeNvr() == null || config.getExcludeNvr().isEmpty()) {
            excludeNvrPredicate = i -> true;
        } else {
            GlobPredicate nvrPredicate = new GlobPredicate(config.getExcludeNvr());
            excludeNvrPredicate = rpm -> nvrPredicate.test(rpm.getNvr());
        }
    }

    @Override
    public Optional<KojiBuildDownloadResult> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        File checkoutBuildFile = new File(workspace, BUILD_XML);
        Optional<Build> buildOpt = new BuildsSerializer().read(checkoutBuildFile);
        if (!buildOpt.isPresent()) {
            // if we are here - it is the first build ever,
            // have to pull the koji and download whatever we'll find:
            buildOpt = new KojiListBuilds(getLog(), config, notProcessedNvrPredicate).invoke(workspace, channel);
            if (!buildOpt.isPresent()) {
                // if we are here - no remote changes on first build, exiting:
                return Optional.empty();
            }
        }
        // we got the build info in workspace, downloading:
        File targetDir = workspace;
        if (config.getDownloadDir() != null && config.getDownloadDir().length() > 0) {
            // target dir was specified,
            targetDir = new File(targetDir, config.getDownloadDir());
            // do not delete the workspace dir if user specified '.' :
            if (!targetDir.getAbsoluteFile().equals(workspace.getAbsoluteFile()) && targetDir.exists()) {
                cleanDirRecursively(targetDir);
            }
            targetDir.mkdirs();
        }
        Build build = buildOpt.get();
        List<File> rpmFiles = downloadRPMs(targetDir, build);
        return Optional.of(new KojiBuildDownloadResult(build, rpmFiles));
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

    private List<File> downloadRPMs(File targetDir, Build build) {
        return build.getRpms()
                .stream()
                .parallel()
                .filter(excludeNvrPredicate.negate())
                .map((r) -> downloadRPM(targetDir, build, r))
                .collect(Collectors.toList());
    }

    private File downloadRPM(File targetDir, Build build, RPM rpm) {
        HttpURLConnection httpConn = null;
        try {
            String urlString = composeUrl(build, rpm);
            log("Downloading RPM '" + rpm.getNvr() + "' from URL: " + urlString);
            URL url = new URL(urlString);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            int response = httpConn.getResponseCode();
            if (response < 200 || response > 299) {
                log("Cancelling because of server response: " + response);
                throw new Exception("Error downloading RPM, got response code: " + response);
            }

            File targetFile = new File(targetDir, rpm.getNvr() + '.' + rpm.getArch() + ".rpm");
            log("Saving RPM '" + rpm.getNvr() + "' to file: " + targetFile.getAbsolutePath());
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                    InputStream in = httpConn.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            log("Finished downloading of RPM '" + rpm.getNvr() + "'");
            return targetFile;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while downloading RPM", ex);
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
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
}
