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
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

public class JenkinsCliWrapper {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final String host;
    private final int port;
    private final String user = "unused_now";

    private static class Singleton {

        private static JenkinsCliWrapper client = new JenkinsCliWrapper("localhost", 9999);

    }

    public class ClientResponse {

        public final int remoteCommandreturnValue;
        public final String sout;
        public final String serr;
        public final Throwable sshEngineExeption;
        public final String cmd;
        public final String plainCmd;

        ClientResponse(Integer res, String so, String se, Throwable ex, String origCommand) {
            this.cmd = "ssh -p " + port + " " + user + "@" + host + " " + origCommand;
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
            if (sshEngineExeption != null) {
                throw new IOException("Probable ssh engine fail in `" + cmd + "`", sshEngineExeption);
            } else {
                if (remoteCommandreturnValue != 0) {
                    throw new IOException("ssh command `" + cmd + "` returned non zero: " + remoteCommandreturnValue);
                }
            }
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
        return Singleton.client;
    }

    public static void setCli(JenkinsCliWrapper c) {
        Singleton.client = c;
    }

    public static void killCli() {
        Singleton.client = new NoOpWrapper();
    }

    public static void reinitCli() {
        Singleton.client = new JenkinsCliWrapper("localhost", 9999);
    }

    private ClientResponse syncSshExec(String cmd) throws IOException, InterruptedException {
        return syncSshExec(cmd, null);
    }

    private JenkinsCliWrapper(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static class NoOpWrapper extends JenkinsCliWrapper {

        public NoOpWrapper() {
            super("nothing", 666);

        }

        @Override
        ClientResponse syncSshExec(String cmd, InputStream is) throws IOException, InterruptedException {
            return new ClientResponse(0, "no-sout", "no-ser", null, cmd);
        }

    }

    ClientResponse syncSshExec(String cmd, InputStream is) throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Executing: ssh -p {0} " + user + "@{1} {2}", new Object[]{port, host, cmd});
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            //todo enable remote, customize-able server from config
            HostConfigEntry hce = new HostConfigEntry(".*", host, port, user);
            ConnectFuture cu = client.connect(hce);
            cu.await();
            try (ClientSession session = cu.getSession()) {
                //althoughg our jenkins is now insecure, veriy is necessary part
                //once it will be secured, set the path to keys here
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
            ClientResponse r = createJob(name, new FileInputStream(new File(new File(dirWithJobs, name), JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE)));
            return r;
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
            ClientResponse r = updateJob(name, new FileInputStream(new File(new File(dirWithJobs, name), JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE)));
            return r;
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

    public ClientResponse createUpdateJob(JenkinsUpdateVmTemplateBuilder j) {
        return createJob(j.getName(), j.expandToStream());
    }

    public ClientResponse deleteUpdateJob(JenkinsUpdateVmTemplateBuilder j) {
        return deleteJobs(j.getName());
    }

    public ClientResponse updateUpdateJob(JenkinsUpdateVmTemplateBuilder j) {
        return updateJob(j.getName(), j.expandToStream());
    }
}
