package org.fakekoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;

public class Utils {

    private static final ClassLoader classLoader = Utils.class.getClassLoader();

    public static String readResource(Path path) throws IOException {
        return readFile(classLoader.getResource(path.toString()));
    }

    public static String readFile(File file) throws IOException {
        final StringBuilder content = new StringBuilder();
        final FileReader fileReader = new FileReader(file);
        final BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line);
        }
        bufferedReader.close();
        fileReader.close();
        return content.toString();
    }

    public static String readFile(URL url) throws IOException {
        return readFile(new File(url.getFile()));
    }

    public static void writeToFile(Path path, String content) throws IOException {
        writeToFile(path.toFile(), content);
    }

    public static void writeToFile(File file, String content) throws IOException {
        final PrintWriter writer = new PrintWriter(file.getAbsolutePath());
        writer.write(content);
        writer.flush();
        writer.close();
    }
}
