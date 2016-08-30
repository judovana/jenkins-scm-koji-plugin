/*
 * The MIT License
 *
 * Copyright 2016 jvanek.
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

/**
 *
 * This package is for emualting koji.
 *
 * You can run java -Xmx1g -cp unpacked:jenkins-scm-koji-plugin.hpi:libs org.fakekoji.xmlrpc.server.JavaServer some_dir
 *
 * over some_dir to emulate koji database, and so allow koji plugin work over
 * your own builds.
 * 
 * 
 * eg:
 * 
 * java -Xmx1g -cp hamcrest-core-1.3.jar:jenkins-scm-koji-plugin.jar:junit-4.12.jar:ws-commons-util-1.0.2.jar:xml-apis-1.4.01.jar:xmlrpc-client-3.1.3.jar:xmlrpc-common-3.1.3.jar:xmlrpc-server-3.1.3.jar org.fakekoji.xmlrpc.server.JavaServer ~/NetBeansProjects/CustomXmlRpc/src/test/resources/local-builds/
 * 
 * FIXME investigate:
 * mvn assembly:assembly -DskipTests
 * to simplyfy launching
 * 
 */
