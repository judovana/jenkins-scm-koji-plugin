package org.fakekoji.jobmanager.model;

import org.apache.sshd.common.digest.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public abstract class Job implements NamesProvider {

    public static final String DELIMITER = "-";
    public static final int MAX_JOBNAME_LENGTH = 50;

    public abstract String generateTemplate() throws IOException;

    @Override
    public int hashCode() {
        return Objects.hash(toString());
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
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
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
        if (n.contains(DELIMITER + DELIMITER)) {
            return n.replaceAll(DELIMITER + "+", DELIMITER);
        } else {
            return n;
        }
    }

}
