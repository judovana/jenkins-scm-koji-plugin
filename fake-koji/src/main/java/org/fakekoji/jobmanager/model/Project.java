package org.fakekoji.jobmanager.model;

import java.util.Objects;

public class Project {

    private final String id;
    private final ProjectType type;

    public Project() {
        id = null;
        type = null;
    }

    public Project(String id, ProjectType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public ProjectType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) &&
                type == project.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }

    public enum ProjectType {
        JDK_PROJECT("JDK_PROJECT"),
        JDK_TEST_PROJECT("JKD_TEST_PROJECT");

        private final String value;

        ProjectType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
