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

    private static void sentFile(File f, HttpExchange t) throws IOException {
        long size = f.length();
        System.out.println(f.getAbsolutePath() + " is " + size + " bytes long");
        t.sendResponseHeaders(200, size);
        OutputStream os = t.getResponseBody();
        copy(new FileInputStream(f), os);
        os.close();
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
        System.out.println(f.getAbsolutePath() + " listing directory!");
        String[] s = f.list();
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("  <body>\n");
        for (String string : s) {
            sb.append("    <a href=\"").append(requestedFile).append("/").append(string).append("\">");
            sb.append(string);
            sb.append("    </a><br/>\n");
        }
        sb.append("  </body>\n");
        sb.append("</html>\n");
        String result = sb.toString();
        long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
        t.sendResponseHeaders(200, size);
        OutputStream os = t.getResponseBody();
        os.write(result.getBytes());
        os.close();

    }

}
