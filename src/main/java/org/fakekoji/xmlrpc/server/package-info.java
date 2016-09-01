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
 * You can run java -Xmx1g -cp unpacked:jenkins-scm-koji-plugin.hpi:libs:anotherClasspath org.fakekoji.xmlrpc.server.JavaServer some_dir
 *
 * over some_dir to emulate koji database, and so allow koji plugin work over
 * your own builds.
 * 
 * To simplyfy the packaging, you can use included maven assembly .
 * This is not exactly automated, as you need to change packaging to jar.
 * then:
 * mvn clean install -DskipTests  && mvn assembly:assembly -DskipTests
 * cd target && java -Xmx1g -cp jenkins-scm-koji-plugin-jar-with-dependencies.jar:/usr/share/java/apache-commons-logging.jar org.fakekoji.xmlrpc.server.JavaServer /mnt/raid1/local-builds/ ; cd ..
 *
 * where logging.jar is any logging implementation. Apache xmlrpc are happy with apache logging.
 * 
 */
