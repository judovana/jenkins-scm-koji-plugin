package org.fakekoji.jobmanager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.util.io.input.NoCloseInputStream;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.views.JenkinsViewTemplateBuilder;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.jetbrains.annotations.NotNull;

public class JenkinsCliWrapper {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static AccessibleSettings originalSettings;

    public static void setMainSettings(AccessibleSettings accessibleSettings) {
        originalSettings = accessibleSettings;
    }

    private final String host;
    private final int port;
    private final UserWithShhKey user;

    private static final class UserWithShhKey {
        String user = "unused_now";
        String pathToKey = null;

        public UserWithShhKey(String user, String pathToKey) {
            if (user == null) {
                this.user = "unused_now";
            } else {
                this.user = user;
            }
            this.pathToKey = pathToKey;
        }

        public String toSubString() {
            if (pathToKey == null){
                return user;
            } else {
                return "-i " + pathToKey + " " + user;
            }
        }
    }

    private static class Singleton {

        private static JenkinsCliWrapper client;

        public static JenkinsCliWrapper getClient() {
            if (client == null) {
                client = new JenkinsCliWrapper(originalSettings.getJenkinsSshHost(), originalSettings.getJenkinsSshPort(), originalSettings.getJenkinsSshUser(), originalSettings.getJenkinsSshPathToPrivateKey());
            }
            return client;
        }

    }

    public class ClientResponse extends  ClientResponseBase {

        public ClientResponse(Integer res, String so, String se, Throwable ex, String origCommand) {
            super(res, so, se, ex, "ssh -p " + port + " " + user.toSubString() + "@" + host + " " + origCommand);
        }

    }
    public static class ClientResponseBase {

        public final int remoteCommandreturnValue;
        public final String sout;
        public final String serr;
        public final Throwable sshEngineExeption;
        public final String cmd;
        public final String plainCmd;

        public ClientResponseBase(Integer res, String so, String se, Throwable ex, String origCommand) {
            this.cmd = origCommand;
            this.plainCmd = origCommand;
            this.sshEngineExeption = ex;
            this.sout = so;
            this.serr = se;
            if (res == null) {
                this.remoteCommandreturnValue = -1;
            } else {
                this.remoteCommandreturnValue = res;
            }
        }

        public void throwIfNecessary() throws IOException {
            String so = getNiceOutput(sout, "(no stdout)", "stdout: ");
            String se = getNiceOutput(serr, "(no stderr)", "stderr: ");
            if (sshEngineExeption != null) {
                throw new IOException("Probable ssh engine fail in `" + cmd + "`" + se + ", " + so, sshEngineExeption);
            } else {
                if (remoteCommandreturnValue != 0) {
                    throw new IOException("ssh command `" + cmd + "` returned non zero: " + remoteCommandreturnValue + "; " + se + ", " + so);
                }
            }
        }

        @NotNull
        private String getNiceOutput(String sout, String alternative, String prefix) {
            String r = sout;
            if (r == null) {
                r = alternative;
            } else {
                r = prefix + alternative;
            }
            r = r.substring(0, Math.min(r.length(), 50));
            return r;
        }

        @Override
        public String toString() {
            if (sshEngineExeption != null) {
                return "`" + cmd + "` failed, because " + sshEngineExeption.toString();
            }
            if (remoteCommandreturnValue != 0) {
                if (serr != null) {
                    return "`" + cmd + "` returned non zero: " + remoteCommandreturnValue + " serr = `" + serr + "`";
                } else {
                    return "`" + cmd + "` returned non zero: " + remoteCommandreturnValue + " and serr is null";
                }
            }
            if (serr != null) {
                return "`" + cmd + "` seems ok: " + remoteCommandreturnValue + " serr = `" + serr + "`";
            } else {
                return "`" + cmd + "` seems ok: " + remoteCommandreturnValue + " and serr is null";
            }
        }

        public boolean simpleVerdict() {
            if (sshEngineExeption != null) {
                return false;
            }
            if (remoteCommandreturnValue != 0) {
                return false;
            }
            return true;
        }
    }


    public static JenkinsCliWrapper getCli() {
        return Singleton.getClient();
    }

    public static void setCli(JenkinsCliWrapper c) {
        Singleton.client = c;
    }

    public static void killCli() {
        Singleton.client = new NoOpWrapper();
    }

    public static void reinitCli() {
        if (originalSettings == null) {
            //LOGGER.log(Level.SEVERE, "No settings set, reinit would be futile");
            throw new NullPointerException("No settings set, reinit would be futile");
        } else {
            Singleton.client = new JenkinsCliWrapper(originalSettings.getJenkinsSshHost(), originalSettings.getJenkinsSshPort(), originalSettings.getJenkinsSshUser(), originalSettings.getJenkinsSshPathToPrivateKey());
        }
    }

    private ClientResponse syncSshExec(String cmd) throws IOException, InterruptedException {
        return syncSshExec(cmd, null);
    }

    private JenkinsCliWrapper(String host, Integer port, String user, String key) {
        if (host == null) {
            this.host = "localhost";
        } else {
            this.host = host;
        }
        if (port == null) {
            this.port=999;
        } else {
            this.port = port;
        }
        this.user = new UserWithShhKey(user, key);
    }

    public static class NoOpWrapper extends JenkinsCliWrapper {

        public NoOpWrapper() {
            super("nothing", 666, "noOne", null);

        }

        @Override
        ClientResponse syncSshExec(String cmd, InputStream is) throws IOException, InterruptedException {
            return new ClientResponse(0, "no-sout", "no-ser", null, cmd);
        }

    }



    ClientResponse syncSshExec(String cmd, InputStream is) throws IOException, InterruptedException {
        LOGGER.log(Level.FINE, toString(cmd));
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            HostConfigEntry hce = new HostConfigEntry(".*", host, port, user.user);
            if (user.pathToKey != null) {
                hce.addIdentity(user.pathToKey);
            }
            ConnectFuture cu = client.connect(hce);
            cu.await();
            try (ClientSession session = cu.getSession()) {
                session.auth().verify();

                try (ChannelExec channel = session.createExecChannel(cmd)) {
                    if (is == null) {
                        channel.setIn(new NoCloseInputStream(System.in));
                    } else {
                        channel.setIn(is);
                    }
                    ByteArrayOutputStream boos = new ByteArrayOutputStream();
                    channel.setOut(boos);
                    ByteArrayOutputStream boes = new ByteArrayOutputStream();
                    channel.setErr(boes);
                    OpenFuture of = channel.open();
                    of.await();
                    Throwable ex = of.getException();
                    Set<ClientChannelEvent> r = channel.waitFor(Arrays.asList(ClientChannelEvent.CLOSED), -1);
                    String so = new String(boos.toByteArray(), "utf8");
                    String se = new String(boes.toByteArray(), "utf8");
                    Integer res = channel.getExitStatus();
                    return new ClientResponse(res, so, se, ex, cmd);
                } finally {
                    CloseFuture fc = session.close(false);
                    fc.await();
                }
            } finally {
                client.stop();
            }
        }
    }

    public String toString(String cmd) {
        return "Executing: ssh -p " + port + " " + user.toSubString() + "@" + host + " " + cmd;
    }

    public String toString() {
        return "Will be " + toString("future_cmd");
    }

    public ClientResponse help() {
        String cmd = "help";
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse listJobs() {
        String cmd = "list-jobs";
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public String[] listJobsToArray() throws Throwable {
        ClientResponse r = syncSshExec("list-jobs");
        if (r.sshEngineExeption != null) {
            throw r.sshEngineExeption;
        }
        if (r.remoteCommandreturnValue != 0) {
            throw new IOException("ssh returned " + r.remoteCommandreturnValue);
        }
        String[] resultListOfJobs = r.sout.split("\\s+");
        for (int i = 0; i < resultListOfJobs.length; i++) {
            resultListOfJobs[i] = resultListOfJobs[i].trim();
        }
        return resultListOfJobs;
    }

    public ClientResponse createJob(String name, InputStream config) {
        String cmd = "create-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd, config);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    /**
     * Signature of this method ensures job is really refreshed by itself
     *
     * @param dirWithJobs
     * @param name
     * @return
     */
    public ClientResponse createManuallyUploadedJob(File dirWithJobs, String name) {
        try {
            try(final FileInputStream fis = new FileInputStream(new File(new File(dirWithJobs, name), JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE))) {
                ClientResponse r = createJob(name, fis);
                return r;
            }
        } catch (IOException ex) {
            return new ClientResponse(-1, "", "", ex, /*copypasted*/ "create-job " + name);
        }
    }
    
    /**
     * Signature of this method ensures job is really refreshed by itself
     *
     * @param dirWithJobs
     * @param name
     * @return
     */
    public ClientResponse updateManuallyUpdatedJob(File dirWithJobs, String name) {
        try {
            try(final FileInputStream fis = new FileInputStream(new File(new File(dirWithJobs, name), JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE))) {
                ClientResponse r = updateJob(name, fis);
                return r;
            }
        } catch (IOException ex) {
            return new ClientResponse(-1, "", "", ex, /*copypasted*/ "update-job " + name);
        }
    }

    public ClientResponse deleteJobs(String... name) {
        String names = String.join(" ", name);
        String cmd = "delete-job " + names;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse relaodAll() {
        String cmd = "reload-configuration";
        try {
            ClientResponse r = syncSshExec(cmd);
            //this seems to block while it relaods (good!)
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse updateJob(String name, InputStream config) {
        String cmd = "update-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd, config);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse reloadJob(String name) {
        String cmd = "reload-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse getJob(String name) {
        String cmd = "get-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse buildAndWait(String name) {
        String cmd = "build -s " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse scheduleBuild(String name) {
        String cmd = "build " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse enableJob(String name) {
        String cmd = "enable-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse disableJob(String name) {
        String cmd = "disable-job " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse stopJob(String name) {
        String cmd = "stop-builds " + name;
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse createView(JenkinsViewTemplateBuilder j) {
        String cmd = "create-view";
        try {
            ClientResponse r = syncSshExec(cmd, j.expandToStream());
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse deleteView(JenkinsViewTemplateBuilder j) {
        String cmd = "delete-view "+j.getName();
        try {
            ClientResponse r = syncSshExec(cmd);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse updateView(JenkinsViewTemplateBuilder j) {
        String cmd = "update-view "+j.getName();
        try {
            ClientResponse r = syncSshExec(cmd, j.expandToStream());
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex, cmd);
        }
    }

    public ClientResponse createUpdateJob(JenkinsUpdateVmTemplateBuilder j) throws IOException {
        return createJob(j.getName(), j.expandToStream());
    }

    public ClientResponse deleteUpdateJob(JenkinsUpdateVmTemplateBuilder j) {
        return deleteJobs(j.getName());
    }

    public ClientResponse updateUpdateJob(JenkinsUpdateVmTemplateBuilder j) throws IOException {
        return updateJob(j.getName(), j.expandToStream());
    }
}
