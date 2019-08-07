package org.fakekoji.jobmanager.model;

import java.io.IOException;

public interface Job {

    String DELIMITER = "-";

    String generateTemplate() throws IOException;
}
