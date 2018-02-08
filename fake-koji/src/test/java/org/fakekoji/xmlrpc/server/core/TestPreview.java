/*
 * The MIT License
 *
 * Copyright 2018 zzambers.
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

import java.io.File;
import java.net.HttpURLConnection;
import org.fakekoji.JavaServer;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPreview {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    public static JavaServer javaServer = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        /* prepare directory structure for fake-koji with some dummy builds */
        File tmpDir = temporaryFolder.newFolder();
        tmpDir.mkdir();
        /* create fake koji server */
        javaServer = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(tmpDir);
        /* start fake-koji server */
        javaServer.start();
    }

    @AfterClass
    public static void afterClass() {
        /* stop fake-koji */
        if (javaServer != null) {
            javaServer.stop();
        }
    }

    @Test
    public void testResponseCodeMainPage() throws Exception {
        int response = FakeKojiTestUtil.doHttpRequest("http://127.0.0.1:8080", "GET");
        assertEquals(HttpURLConnection.HTTP_OK, response);
    }

}
