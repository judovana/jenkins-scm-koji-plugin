package hudson.plugins.scm.koji.client;

import hudson.plugins.scm.koji.WebLog;

public class AbstractLoggingWorker {

    private final WebLog log;

    public AbstractLoggingWorker(WebLog log) {
        this.log = log;
    }

    protected void log(String message) {
        log.message(message);
    }

    protected WebLog getLog() {
        return log;
    }

}
