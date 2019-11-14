package org.fakekoji;

import jdk.nashorn.api.scripting.URLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Utils {

    public static String readResource(String resourcePath) throws IOException {
        try (final InputStreamReader inputStream = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath)))) {
            return readStream(inputStream);
        }
    }

    public static String readFile(URL url) throws IOException {
        try (final URLReader urlReader = new URLReader(url);
                final BufferedReader bufferedReader = new BufferedReader(urlReader)) {
            return readStream(bufferedReader);
        }

    }

    public static String readFile(File file) throws IOException {
        return readFile(file.toURI().toURL());
    }

    public static String readStream(Reader in) throws IOException {
        try (final BufferedReader bufferedReader = new BufferedReader(in)) {
            final StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    public static void writeToFile(Path path, String content) throws IOException {
        writeToFile(path.toFile(), content);
    }

    public static void writeToFile(File file, String content) throws IOException {
        writeToFile(new FileOutputStream(file), content);
    }

    public static void writeToFile(OutputStream os, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8")) {
            writer.write(content);
            writer.flush();
        }
    }

    public static void moveFile(File source, File target) throws IOException {
        Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING);
    }
}
