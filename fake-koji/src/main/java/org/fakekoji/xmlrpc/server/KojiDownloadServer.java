/*
 * The MIT License
 *
 * Copyright 2018 .
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
package org.fakekoji.xmlrpc.server;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.fakekoji.xmlrpc.server.core.FileReturningHandler;

/**
 * This class implements http server used as koji download server ( packages are
 * downloaded from here by jenkins koji plugin ).
 *
 * Based on code, which was originally in JavaServer class.
 */
public class KojiDownloadServer {

    private File dbFileRoot;
    private int port;
    private HttpServer hs;

    public KojiDownloadServer(File dbFileRoot, int port) {
        this.dbFileRoot = dbFileRoot;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        if (hs == null) {
            hs = HttpServer.create(new InetSocketAddress(port), 0);
            hs.createContext("/", new FileReturningHandler(dbFileRoot));
        }
        hs.start();
    }

    public void stop() {
        if (hs != null) {
            hs.stop(10);
        }
    }
}
