package org.fakekoji.jobmanager.model;

import java.io.IOException;
import java.util.Objects;

public abstract class Job implements NamesProvider{

    public static final String DELIMITER = "-";

    public abstract String generateTemplate() throws IOException;

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }

    @Override
    public String toString(){
        return getName();
    }

    @Override
    public abstract String getName();

    @Override
    public abstract String getShortName();


}
