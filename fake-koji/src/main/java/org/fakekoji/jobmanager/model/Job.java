package org.fakekoji.jobmanager.model;

import java.io.IOException;
import java.util.Objects;

public abstract class Job {

    public static final String DELIMITER = "-";

    public abstract String generateTemplate() throws IOException;

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }
}
