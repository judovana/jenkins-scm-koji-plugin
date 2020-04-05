package org.fakekoji.api.http.rest;

public class OToolError {
    final String message;
    final int code;

    public OToolError(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
