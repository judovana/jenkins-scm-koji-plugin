/*
 * The MIT License
 *
 * Copyright 2015 user.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fakekoji.xmlrpc.server.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import hudson.plugins.scm.koji.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.fakekoji.xmlrpc.server.ServerLogger;

/**
 *
 * This class is very simple files providing handler. If it is known file in
 * declared root, then it is returned. If it is directory, listing is returned,
 * otherwise 404.
 */
public class FileReturningHandler implements HttpHandler {

    private final File root;

    public FileReturningHandler(File dbFileRoot) {
        this.root = dbFileRoot;
    }

    /**
     * This misleading variable can serve tarxz instead of rpm. Its for testing
     * purposes only.
     */
    private static final boolean ALLOW_FAKE_FILE = false;

    @Override
    public void handle(HttpExchange t) throws IOException {
        //moving result toseparate thread is increasing performance by 1000%
        RequestRunner rr = new RequestRunner(t);
        new Thread(rr).start();
    }

    private class RequestRunner implements Runnable {

        private final HttpExchange t;

        public RequestRunner(HttpExchange t) {
            this.t = t;
        }

        @Override
        public void run() {
            try {
                runImpl();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void runImpl() throws IOException {
            String requestedFile = t.getRequestURI().getPath();
            File f = new File(root + "/" + requestedFile);
            ServerLogger.log(new Date().toString() + " attempting: " + requestedFile);
            if (f.getName().equals("ALL")) {
                sentFullLIst(f, requestedFile, t);
                return;
            }
            if (f.exists() && f.isDirectory()) {
                sentDirListing(f, requestedFile, t);
                return;
            }
            if (f.exists()) {
                sentFile(f, t);
            } else {
                if (ALLOW_FAKE_FILE) {
                    ServerLogger.log(f.getAbsolutePath() + " not found");
                    f = new File(root + "/" + requestedFile.replaceAll("\\.rpm$", ".tarxz"));
                }
                if (f.exists()) {
                    sentFile(f, t);
                } else {
                    ServerLogger.log(f.getAbsolutePath() + " not found");
                    t.sendResponseHeaders(404, 0);
                    OutputStream os = t.getResponseBody();
                    os.close();

                }
            }
        }

    }

    private static void sentFullLIst(File ff, final String requestedFile, HttpExchange t) throws IOException {
        File f = ff.getParentFile();
        ServerLogger.log(f.getAbsolutePath() + " listing all files!");
        String init = "<html>\n  <body>\n";
        StringBuilder sb1 = generateHtmlFromFileList(requestedFile, f, new ComparatorByVersion());
        StringBuilder sb2 = generateHtmlFromFileList(requestedFile, f, new ComparatorByLastModified());
        String close = "  </body>\n</html>\n";
        String result = init + sb1.toString() + "<hr/>" + sb2 + close;
        long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
        t.sendResponseHeaders(200, size);
        try (OutputStream os = t.getResponseBody()) {
            os.write(result.getBytes());
        }

    }

    private static List<FileInfo> getRecursiveFileList(final String requestedFile, final File f) throws IOException {
        List<FileInfo> list = new ArrayList();
        Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileChunk = file.toAbsolutePath().toString().substring(f.getAbsolutePath().length());
                String rf = new File(requestedFile).getParent();
                String path = "/" + rf + "/" + fileChunk;
                path = path.replaceAll("/+", "/");
                list.add(new FileInfo(fileChunk, path, file));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return list;
    }

    private static StringBuilder generateHtmlFromFileList(final String requestedFile, final File f, Comparator c) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final InfoProvider provider = (InfoProvider) c;
        sb.append("  <h2>").append(requestedFile);
        sb.append(provider.getTitle());
        sb.append("</h2>\n");
        sb.append("    <a href=\"").append(new File(requestedFile).getParent()).append("\">");
        sb.append("..");
        sb.append("    </a><br/>\n");
        List<FileInfo> fileList = getRecursiveFileList(requestedFile, f);
        Collections.sort(fileList, c);
        for (FileInfo file : fileList) {
            sb.append("    <a href=\"").append(file.getPath()).append("\">");
            sb.append(file.getFileChunk());
            sb.append("    </a>");
            sb.append(provider.getInfo(file));
            sb.append("<br/>\n");
        }
        return sb;
    }

    private static void sentFile(File f, HttpExchange t) throws IOException {
        long size = f.length();
        ServerLogger.log(f.getAbsolutePath() + " is " + size + " bytes long");
        t.sendResponseHeaders(200, size);
        try (OutputStream os = t.getResponseBody()) {
            copy(new FileInputStream(f), os);
        }
    }

    private static final int BUF_SIZE = 0x1000; // 4K

    public static long copy(InputStream from, OutputStream to)
            throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    private void sentDirListing(File f, String requestedFile, HttpExchange t) throws IOException {
        ServerLogger.log(f.getAbsolutePath() + " listing directory!");
        String[] files = f.list();
        ArrayList<FileInfo> s = new ArrayList();
        s.add(0, new FileInfo("ALL", "ALL", new File(root + "/" + requestedFile + "/" + "ALL").toPath()));
        for (int i = 0; i < files.length; i++) {
            String fileChunk = files[i];
            long lastModifiedDirContent = 0;
            File file = new File(root + "/" + requestedFile + "/" + fileChunk);
            if (file.exists()) {
                if (Files.isDirectory(file.toPath())) {
                    lastModifiedDirContent = getNewestDateIn(file).getTime();
                }
            }
            s.add(new FileInfo(fileChunk, fileChunk, file.toPath(), lastModifiedDirContent));
        }
        String init = "<html>\n  <body>\n";

        StringBuilder sb1 = generateHtmlFromList(requestedFile, s, new ComparatorByVersion());
        StringBuilder sb2 = generateHtmlFromList(requestedFile, s, new ComparatorByLastModified());
        StringBuilder sb3 = generateHtmlFromList(requestedFile, s, new ComparatorByLastModifiedDirContent());
        String close = "  </body>\n</html>\n";
        String result = init + sb1.toString() + "<hr/>" + sb2.toString() + "<hr/>" + sb3.toString() + close;
        long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
        t.sendResponseHeaders(200, size);
        try (OutputStream os = t.getResponseBody()) {
            os.write(result.getBytes());
        }
    }

    private void sortFileList(ArrayList<FileInfo> list, Comparator c) {
        if (!list.get(0).getFileChunk().equals("ALL")) {
            throw new RuntimeException("File list doesn't contain ALL file.");
        }
        FileInfo all = list.get(0);
        list.remove(0);
        Collections.sort(list, c);
        list.add(0, all);
    }

    private static boolean areNumeric(String s1, String s2) {
        try {
            Integer.parseInt(s1);
            Integer.parseInt(s2);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String addDots(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        char c = str.charAt(0);
        for (int i = 1; i < str.length(); i++) {
            char next = str.charAt(i);
            stringBuilder.append(c);
            if (isAlphaNumericChange(c, next)) {
                stringBuilder.append('.');
            }
            c = next;
        }
        stringBuilder.append(c);
        return stringBuilder.toString();
    }

    private static boolean isAlphaNumericChange(char first, char second) {
        return ((Character.isDigit(first) && Character.isAlphabetic(second)) || (Character.isAlphabetic(first) && Character.isDigit(second)));
    }

    private StringBuilder generateHtmlFromList(String requestedFile, ArrayList<FileInfo> s, Comparator c) throws IOException {
        StringBuilder sb = new StringBuilder();
        InfoProvider provider = (InfoProvider) c;
        sb.append("  <h2>").append(requestedFile);
        sb.append(provider.getTitle());
        sb.append("</h2>\n");
        sb.append("    <a href=\"").append(new File(requestedFile).getParent()).append("\">");
        sb.append("..");
        sb.append("    </a><br/>\n");
        sortFileList(s, c);
        for (FileInfo file : s) {
            String path = "/" + requestedFile + "/" + file.getFileChunk();
            path = path.replaceAll("/+", "/");
            sb.append("    <a href=\"").append(path).append("\">");
            sb.append(file.getFileChunk());
            sb.append("    </a>");
            sb.append(provider.getInfo(file));
            sb.append("<br/>\n");
        }
        return sb;
    }

    private static Date getNewestDateIn(File root) throws IOException {
        File f = getNewestFile(root);
        if (f == null) {
            return new Date(root.lastModified());
        } else {
            return new Date(f.lastModified());
        }

    }

    private static File getNewestFile(File root) throws IOException {
        List<File> files = getNonLogs(root);
        if (files == null || files.isEmpty()) {
            files = getLogs(root);
        }
        if (files == null || files.isEmpty()) {
            return null;
        }
        Collections.sort(files, (File o1, File o2) -> {
            if (o1.lastModified() == o2.lastModified()) {
                return 0;
            }
            if (o1.lastModified() > o2.lastModified()) {
                return -1;
            }
            return 1;
        });
        return files.get(0);
    }

    private static List<File> getNonLogs(File root) throws IOException {
        List<File> logs = new ArrayList<>();
        Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    if (!file.toFile().getAbsolutePath().contains("/logs/")) {
                        logs.add(file.toFile());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return logs;
    }

    private static List<File> getLogs(File root) throws IOException {
        List<File> logs = new ArrayList<>();
        Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    if (file.toFile().getAbsolutePath().contains("/logs/")) {
                        logs.add(file.toFile());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return logs;
    }

    static class FileInfo {

        private final String fileChunk;
        private final String path;
        private final Path file;
        private final long lastModifiedDirContent;

        public FileInfo(String fileChunk, String path, Path file) {
            this.fileChunk = fileChunk;
            this.path = path;
            this.file = file;
            lastModifiedDirContent = 0;
        }

        public FileInfo(String fileChunk, String path, Path file, long lastModifiedDirContent) {
            this.fileChunk = fileChunk;
            this.path = path;
            this.file = file;
            this.lastModifiedDirContent = lastModifiedDirContent;
        }

        public String getPath() {
            return path;
        }

        public String getFileChunk() {
            return fileChunk;
        }

        public Path getFile() {
            return file;
        }

        public long getLastModifiedDirContent() {
            return lastModifiedDirContent;
        }

        public long getLastModified() {
            return file.toFile().lastModified();
        }

        public long getFileSize() {
            return file.toFile().length() / 1024l;
        }
    }

    static class ComparatorByVersion implements Comparator<FileInfo>, InfoProvider {

        private static final String NON_ALPHANUMERIC_REGEX = "[^a-zA-Z0-9]";

        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            /*
              we need to add a dot between every adjacent number and letter so fileChunk can be split into numeric and alphabetical strings for better sorting ojdk7 and ojdk8 files
              alphabetical strings must be sorted in ascending order(a first, z last) and numeric string in descending order(9 first, 0 last) - we want the last version on the top
              example:
              old: jdk8u152.b01 -> (split) -> [ojdk8u152, b01]
              new: jdk8u152.b01 -> (addDots)-> jdk.8.u.152.b.01 -> (split) -> [jdk, 8, u, 152, b, 01] (numbers and letters separated)
             */
            String[] arr1 = addDots(f1.getFileChunk()).split(NON_ALPHANUMERIC_REGEX);
            String[] arr2 = addDots(f2.getFileChunk()).split(NON_ALPHANUMERIC_REGEX);
            int min = Math.min(arr1.length, arr2.length);
            for (int i = 0; i < min; i++) {
                int compare = areNumeric(arr1[i], arr2[i]) ? Integer.compare(Integer.parseInt(arr2[i]), Integer.parseInt(arr1[i])) : arr1[i].compareTo(arr2[i]);
                if (compare != 0) {
                    return compare;
                }
            }
            return arr1.length - arr2.length;
        }

        @Override
        public String getInfo(FileInfo f) {
            return "";
        }

        @Override
        public String getTitle() {
            return "";
        }
    }

    static class ComparatorByLastModifiedDirContent implements Comparator<FileInfo>, InfoProvider {

        @Override
        public int compare(FileInfo o1, FileInfo o2) {

            if (o2.getLastModifiedDirContent() == o1.getLastModifiedDirContent()) {
                return 0;
            }
            if (o2.getLastModifiedDirContent() > o1.getLastModifiedDirContent()) {
                return 1;
            }
            return -1;
        }

        @Override
        public String getInfo(FileInfo f) throws IOException {
            if (f.getFileChunk().equals("ALL")) {
                return "";
            }
            String time = Constants.DTF2.format(LocalDateTime.ofInstant(FileTime.from(f.getLastModifiedDirContent(), TimeUnit.MILLISECONDS).toInstant(), ZoneId.systemDefault()));
            return " (" + time + ") (" + f.getFileSize() + ")";
        }

        @Override
        public String getTitle() {
            return "(lastModified dirContent) (size KB)";
        }
    }

    static class ComparatorByLastModified implements InfoProvider, Comparator<FileInfo> {

        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            if (o2.getLastModified() == o1.getLastModified()) {
                return 0;
            }
            if (o2.getLastModified() > o1.getLastModified()) {
                return 1;
            }
            return -1;
        }

        @Override
        public String getInfo(FileInfo f) throws IOException {
            if (f.getFileChunk().equals("ALL")) {
                return "";
            }
            String time = Constants.DTF2.format(LocalDateTime.ofInstant(FileTime.from(f.getFile().toFile().lastModified(), TimeUnit.MILLISECONDS).toInstant(), ZoneId.systemDefault()));
            time = " (" + time + ") (" + f.getFileSize() + ")";
            return time;
        }

        @Override
        public String getTitle() {
            return "(lastModified) (size KB)";
        }

    }

    static interface InfoProvider {

        public String getTitle();

        public String getInfo(FileInfo f) throws IOException;
    }
}
