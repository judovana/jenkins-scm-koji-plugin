package org.fakekoji.jobmanager;

public class ManagementException extends Exception {

    public ManagementException(String message) {
        super(message);
    }

    public ManagementException(Exception exception) {
        super(exception);
    }

    public ManagementException(String message, Exception exception) {
        super(message, exception);
    }
}
