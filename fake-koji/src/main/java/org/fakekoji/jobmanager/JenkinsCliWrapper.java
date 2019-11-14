package org.fakekoji.jobmanager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.util.io.NoCloseInputStream;

public class JenkinsCliWrapper {

    private final String host;
    private final int port;

    private enum Singleton {

        INSTANCE;

        private final JenkinsCliWrapper client;

        private Singleton() {
            //we currently run fake-koji on same machine as jenkins, so localhsot is safe-ground
            client = new JenkinsCliWrapper("localhost", 9999);
        }

    }

    public static class ClientResponse {

        public final int res;
        public final String so;
        public final String se;
        public final Throwable ex;

        ClientResponse(Integer res, String so, String se, Throwable ex) {
            this.ex = ex;
            this.so = so;
            this.se = se;
            if (res == null) {
                this.res = -1;
            } else {
                this.res = res;
            }
        }
    }

    public static JenkinsCliWrapper getCli() {
        return Singleton.INSTANCE.client;
    }

    private ClientResponse syncSshExec(String cmd) throws IOException, InterruptedException {
        return syncSshExec(cmd, null);
    }

    private JenkinsCliWrapper(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private ClientResponse syncSshExec(String cmd, InputStream is) throws IOException, InterruptedException {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            //todo enable remote, customize-able server from config
            HostConfigEntry hce = new HostConfigEntry(".*", host, port, "unused_now");
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
                    return new ClientResponse(res, so, se, ex);
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
        try {
            ClientResponse r = syncSshExec("help");
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse listJobs() {
        try {
            ClientResponse r = syncSshExec("list-jobs");
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public String[] listJobsToArray() throws Throwable {
        ClientResponse r = syncSshExec("list-jobs");
        if (r.ex != null) {
            throw r.ex;
        }
        if (r.res != 0) {
            throw new IOException("ssh returned " + r.res);
        }
        String[] s = r.so.split("\\s+");
        for (int i = 0; i < s.length; i++) {
            s[i] = s[i].trim();
        }
        return s;
    }

    public ClientResponse createJob(String name, InputStream config) {
        try {
            ClientResponse r = syncSshExec("create-job " + name, config);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    /**
     * Signature of this method ensures job is really refreshed by itself
     * @param dirWithJobs
     * @param name
     * @return 
     */
    public ClientResponse reloadOrRegisterManuallyUploadedJob(File dirWithJobs, String name) {
        try {
            ClientResponse r = createJob(name, new FileInputStream(new File(new File(dirWithJobs, name), JenkinsJobUpdater.JENKINS_JOB_CONFIG_FILE)));
            return r;
        } catch (IOException  ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse deleteJobs(String... name) {
        try {
            String names = String.join(" ", name);
            ClientResponse r = syncSshExec("delete-job " + names);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse relaodAll() {
        try {
            ClientResponse r = syncSshExec("reload-configuration");
            //this seems to block while it relaods (good!)
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse updateJob(String name, InputStream config) {
        try {
            ClientResponse r = syncSshExec("update-job " + name, config);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse getJob(String name) {
        try {
            ClientResponse r = syncSshExec("get-job " + name);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse buildAndWait(String name) {
        try {
            ClientResponse r = syncSshExec("build -s " + name);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }

    public ClientResponse scheduleBuild(String name) {
        try {
            ClientResponse r = syncSshExec("build " + name);
            return r;
        } catch (IOException | InterruptedException ex) {
            return new ClientResponse(-1, "", "", ex);
        }
    }
}
