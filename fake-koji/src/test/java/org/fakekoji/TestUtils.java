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
package org.fakekoji;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;


public class TestUtils {


    @Test
    public void testPlainOperations() throws IOException, InterruptedException {
        final File pt = File.createTempFile("processed", "txt");
        Utils.writeToFile(pt, "a #bb\nb  \n\nc #10 \n");
        String s = Utils.readFile(pt);
        List<String> l1 = Utils.readFileToLines(pt, null);
        List<String> l2 = Utils.readProcessedTxt(pt);
        Assertions.assertEquals("a #bb\nb  \n\nc #10 \n", s);
        Assertions.assertEquals(Arrays.asList("a #bb", "b  ", "", "c #10 "), l1);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), l2);
    }

    @Test
    public void testRemoveNvrFromProcessed() throws IOException, InterruptedException {
        final File pt = File.createTempFile("processed", "txt");
        Utils.writeToFile(pt, "bb# comment\na #bb\nb  \n\nc #10 \nbb\n\n  \na #10");
        final Utils.RemovedNvrsResult r = Utils.removeNvrFromProcessed(pt, "bb");
        String s = Utils.readFile(pt);
        Assertions.assertEquals("a #bb\nb  \nc #10 \na #10\n", s);
        List<String> removedNvras = Arrays.asList(
                "bb# comment",
                "bb");
        Set<String> removedNvrasUniq = new HashSet<>(Arrays.asList("bb"));
        List<String> allRead = Arrays.asList(
                "bb# comment",
                "a #bb",
                "b  ",
                "",
                "c #10 ",
                "bb",
                "",
                "  ",
                "a #10");
        List<String> saved = Arrays.asList(
                "a #bb",
                "b  ",
                "c #10 ",
                "a #10"
        );
        Assertions.assertEquals(removedNvras, r.removedNVRs);
        Assertions.assertEquals(removedNvrasUniq, r.removedNVRsUniq);
        Assertions.assertEquals(allRead, r.allRead);
        Assertions.assertEquals(saved, r.saved);

    }


}
