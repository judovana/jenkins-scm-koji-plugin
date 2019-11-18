package org.fakekoji;

import jdk.nashorn.api.scripting.URLReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

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
                content.append(line).append('\n');
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
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "utf-8"))) {
            writer.write(content);
            writer.flush();
        }
    }

    public static void moveDir(File source, File target) throws IOException {
        // we cannot simply move, because archive can, and likely is, diffferent mount point
        // Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING);
        // we can not use appache commons either, because they are unable to handle broken ysmlinks like lastBuild or so
        //FileUtils.moveDirectory(source, target);
        //so we have to go on our own or delete or broken symlinks before the FileUtils.moveDirectory execution
        //anyway, appache commons can be dangeorus as they seems to be exiting with first failure
    }
}
