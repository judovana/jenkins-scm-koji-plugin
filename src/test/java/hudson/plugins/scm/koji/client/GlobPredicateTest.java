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
package hudson.plugins.scm.koji.client;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class GlobPredicateTest {

    @Test
    public void testSimpleAsterics() {
        String glob = "f23-updates*";
        String input = "f23-updates-candidate";
        assertTrue(new GlobPredicate(glob).test(input));
    }

    @Test
    public void testCurlyBrackets() {
        List<String> input = Arrays.asList(
                "java-1.8.0-openjdk-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-accessibility-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-accessibility-debug-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-debug-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-debuginfo-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-demo-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-demo-debug-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-devel-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-devel-debug-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-headless-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-headless-debug-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-src-1.8.0.65-3.b17.fc23.x86_64.rpm",
                "java-1.8.0-openjdk-src-debug-1.8.0.65-3.b17.fc23.x86_64.rpm"
        );
        String glob = "{*debug*,*accessibility*,*src*,*demo*}";
        GlobPredicate predicate = new GlobPredicate(glob);
        assertEquals(3, input.stream().filter(predicate.negate()).count());
    }

}
