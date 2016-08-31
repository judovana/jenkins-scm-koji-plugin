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
import java.util.Date;

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
            System.out.println(new Date().toString() + "attempting: " + requestedFile);
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
                    System.out.println(f.getAbsolutePath() + " not found");
                    f = new File(root + "/" + requestedFile.replaceAll("\\.rpm$", ".tarxz"));
                }
                if (f.exists()) {
                    sentFile(f, t);
                } else {
                    System.out.println(f.getAbsolutePath() + " not found");
                    t.sendResponseHeaders(404, 0);
                    OutputStream os = t.getResponseBody();
                    os.close();

                }
            }
        }

    }

    private static void sentFullLIst(File ff, final String requestedFile, HttpExchange t) throws IOException {
        File f = ff.getParentFile();
        System.out.println(f.getAbsolutePath() + " listing all files!");
        StringBuilder sb = generateHtmlAllFiles(requestedFile, f);
        String result = sb.toString();
        long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
        t.sendResponseHeaders(200, size);
        try (OutputStream os = t.getResponseBody()) {
            os.write(result.getBytes());
        }

    }

    private static StringBuilder generateHtmlAllFiles(final String requestedFile, final File f) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("  <body>\n");
        sb.append("  <h2>").append(requestedFile).append("</h2>\n");
        sb.append("    <a href=\"").append(new File(requestedFile).getParent()).append("\">");
        sb.append("..");
        sb.append("    </a><br/>\n");
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
                sb.append("    <a href=\"").append(path).append("\">");
                sb.append(fileChunk);
                sb.append("    </a><br/>\n");
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
        sb.append("  </body>\n");
        sb.append("</html>\n");
        return sb;
    }

    private static void sentFile(File f, HttpExchange t) throws IOException {
        long size = f.length();
        System.out.println(f.getAbsolutePath() + " is " + size + " bytes long");
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

    private static void sentDirListing(File f, String requestedFile, HttpExchange t) throws IOException {
        System.out.println(f.getAbsolutePath() + " listing directory!");
        String[] files = f.list();
        String[] s = new String[files.length + 1];
        s[0] = "ALL";
        System.arraycopy(files, 0, s, 1, files.length);
        StringBuilder sb = generateHtmlFromArray(requestedFile, s);
        String result = sb.toString();
        long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
        t.sendResponseHeaders(200, size);
        try (OutputStream os = t.getResponseBody()) {
            os.write(result.getBytes());
        }
    }

    private static StringBuilder generateHtmlFromArray(String requestedFile, String[] s) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("  <body>\n");
        sb.append("  <h2>").append(requestedFile).append("</h2>\n");
        sb.append("    <a href=\"").append(new File(requestedFile).getParent()).append("\">");
        sb.append("..");
        sb.append("    </a><br/>\n");
        for (String string : s) {
            String path = "/" + requestedFile + "/" + string;
            path = path.replaceAll("/+", "/");
            sb.append("    <a href=\"").append(path).append("\">");
            sb.append(string);
            sb.append("    </a><br/>\n");
        }
        sb.append("  </body>\n");
        sb.append("</html>\n");
        return sb;
    }

}
