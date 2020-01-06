/*
 * The MIT License
 *
 * Copyright 2017 jvanek.
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
package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class JobTest {


    @Test
    public void testSumTrims() throws Exception {
        String res = Job.truncatedSha("aaaa", 1);
        Assert.assertEquals("4", res);
    }

    @Test
    public void testSumTrimsFromEnd() throws Exception {
        String res = Job.truncatedSha("aaaa", 10);
        Assert.assertEquals("f27b9af0b4", res);
    }

    @Test
    public void testSumTrimDoNotProlong() throws Exception {
        String res = Job.truncatedSha("aaaa", 1000000);
        Assert.assertEquals(64, res.length());
    }

    @Test
    public void testFirstLetterAcceptNothing() throws Exception {
        Assert.assertEquals("", Job.firstLetter(null));
        Assert.assertEquals("", Job.firstLetter(""));
        Assert.assertEquals("", Job.firstLetter("     "));
        ;
    }

    @Test
    public void testFirstLetterWorks() throws Exception {
        Assert.assertEquals("a", Job.firstLetter("a"));
        Assert.assertEquals("a", Job.firstLetter("ab"));
        Assert.assertEquals("c", Job.firstLetter("    cde "));
        ;
    }

    @Test
    public void testSanitize() throws Exception {
        Assert.assertEquals("a-b", Job.sanitizeNames("a-b"));
        Assert.assertEquals("a-b", Job.sanitizeNames("a--b"));
        Assert.assertEquals("    -c-d-e- ", Job.sanitizeNames("    -c-d---e-- "));
        ;
    }

}