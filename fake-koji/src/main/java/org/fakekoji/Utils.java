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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

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
        copyDirPreserveSymlinks(source, target);
        deleteDir(source);
    }

    private static final Level SILENCE = Level.FINEST;
    
    public static void deleteDir(final File source) throws IOException {
        final List<String> outLog = new ArrayList<>();
        final List<Exception> errLog = new ArrayList<>();
        Files.walkFileTree(source.getAbsoluteFile().toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String msg = "Deleting file" + file;
                try {
                    outLog.add(msg);
                    LOGGER.log(SILENCE, msg);
                    Files.delete(file);
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "issue while " + msg, ex);
                    errLog.add(ex);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                LOGGER.log(Level.INFO, "Failed to visit " + file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                String msg = "Deleting dir" + dir;
                try {
                    outLog.add(msg);
                    LOGGER.log(SILENCE, msg);
                    Files.delete(dir);
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "issue while " + msg, ex);
                    errLog.add(ex);
                }
                return FileVisitResult.CONTINUE;
            }
        }
        );
        if (!errLog.isEmpty()) {
            throw new IOException("Noted " + errLog.size() + " issues from " + outLog.size() + " operations. See logs");
        }
    }

    public static void copyDirPreserveSymlinks(File source, File target) throws IOException {
        List<String> outLog = new ArrayList<>();
        List<Exception> errLog = new ArrayList<>();

        Files.walkFileTree(source.getAbsoluteFile().toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(dir)) {
                    System.out.println("aaa");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String msg = "unknown cause";
                try {
                    String destSuffix = file.toString().replace(source.toString(), "");
                    File dest = new File(target.getAbsoluteFile(), destSuffix);
                    File parent = dest.getParentFile();
                    boolean dirOk = true;
                    if (!parent.exists()) {
                        //choice is mkdir here and elaborate on pre-visit, chosing mkdirs
                        dirOk = parent.mkdirs();
                        if (!dirOk) {
                            msg = "Failed to create dir " + parent;
                            IOException ee = new IOException(msg);
                            LOGGER.log(Level.INFO, msg, ee);
                        }
                    }
                    if (Files.isSymbolicLink(file)) {
                        Path lnTarget = Files.readSymbolicLink(file);
                        String nwTarget = lnTarget.toString().replace(source.getAbsolutePath(), target.getAbsolutePath());
                        msg = "recriating symlink " + dest + " -> " + nwTarget + " (was " + lnTarget + ")";
                        LOGGER.log(SILENCE, msg);
                        outLog.add(msg);
                        Files.createSymbolicLink(dest.toPath(), new File(nwTarget).toPath());
                    } else {
                        msg = ("Copy " + file + " to " + dest);
                        LOGGER.log(SILENCE, msg);
                        outLog.add(msg);
                        Files.copy(file, dest.toPath());
                    }
                    if (!dirOk) {
                        msg = "Failed to create dir " + parent;
                        throw new IOException(msg);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "issue while " + msg, ex);
                    errLog.add(ex);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                LOGGER.log(Level.INFO, "Failed to visit " + file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        if (!errLog.isEmpty()) {
            throw new IOException("Noted " + errLog.size() + " issues from " + outLog.size() + " operations. See logs");
        }
    }

    public static void main(String... args) throws IOException {
        copyDirPreserveSymlinks(new File("/tmp/AAA/a"), new File("/tmp/AAA/x"));
    }

}
