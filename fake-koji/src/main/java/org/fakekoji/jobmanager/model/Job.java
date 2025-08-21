package org.fakekoji.jobmanager.model;

import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.OJDK_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PACKAGE_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_NAME_VAR;

public abstract class Job implements NamesProvider {

    public static final String DELIMITER = "-";
    public static final String VARIANTS_DELIMITER = ".";
    public static final int MAX_JOBNAME_LENGTH = 59;

    private final String projectName;
    private final List<OToolVariable> projectVariables;
    private final Product product;
    private final JDKVersion jdkVersion;

    protected Job(
            final String projectName,
            final List<OToolVariable> projectVariables,
            final Product product,
            final JDKVersion jdkVersion
    ) {
        this.projectName = projectName;
        this.projectVariables = projectVariables;
        this.product = product;
        this.jdkVersion = jdkVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<OToolVariable> getProjectVariables() {
        if (projectVariables == null) {
            return Collections.emptyList();
        } else {
            return projectVariables;
        }
    }

    public Product getProduct() {
        return product;
    }

    public JDKVersion getJdkVersion() {
        return jdkVersion;
    }

    public abstract String generateTemplate() throws IOException;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Job job = (Job) o;
        return Objects.equals(projectName, job.projectName) &&
                Objects.equals(projectVariables, job.projectVariables) &&
                Objects.equals(product, job.product) &&
                Objects.equals(jdkVersion, job.jdkVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, projectVariables, product, jdkVersion);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public abstract String getName();

    @Override
    public abstract String getShortName();


    public static String truncatedSha(String source, int maxLength) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            String fullOutput = byteArrayToHexString(hash);
            if (fullOutput.length() <= maxLength) {
                return fullOutput;
            } else {
                return fullOutput.substring(fullOutput.length() - maxLength);
            }
        } catch (NoSuchAlgorithmException ex) {
            //impossible
            throw new RuntimeException(ex);
        }
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static String firstLetter(String n) {
        if (n == null || n.trim().isEmpty()) {
            return "";
        } else {
            return n.trim().substring(0, 1);
        }
    }

    /**
     * In case of test-only jobs, there can appear double delimtier
     *
     * @param n
     * @return
     */
    public static String sanitizeNames(String n) {
        while (n.contains(DELIMITER+DELIMITER) || n.contains(VARIANTS_DELIMITER+VARIANTS_DELIMITER)) {
            if (n.contains(VARIANTS_DELIMITER + VARIANTS_DELIMITER)) {
                n = n.replace(VARIANTS_DELIMITER + VARIANTS_DELIMITER, VARIANTS_DELIMITER);
            }
            if (n.contains(DELIMITER + DELIMITER)) {
                n = n.replace(DELIMITER + DELIMITER, DELIMITER);
            }
        }
        return n;

    }

    List<OToolVariable> getExportedVariables() {
        return Arrays.asList(
                new OToolVariable(PROJECT_NAME_VAR, projectName),
                new OToolVariable(OJDK_VAR, product.getJdk()),
                new OToolVariable(JDK_VERSION_VAR, jdkVersion.getVersion()),
                new OToolVariable(PACKAGE_NAME_VAR, product.getPackageName())
        );
    }
}
