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
package hudson.plugins.scm.koji;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotProcessedNvrPredicateTest {

    String[] winJdk8 = new String[]{
        "openjdk8-win-1.8.0.131-1.b11",
        "openjdk8-win-1.8.0.131-2.b11.test20170523",
        "openjdk8-win-1.8.0.131-2.b11.test20170701",
        "openjdk8-win-1.8.0.131-2.b11",
        "openjdk8-win-1.8.0.144-1.b01.test20170829",
        "openjdk8-win-1.8.0.144-1.b01.test20170903",
        "openjdk8-win-jdk8u152.b04-21.dev.upstream",
        "openjdk8-win-aarch64.jdk8u141.b16-2.aarch64.upstream",
        "openjdk8-win-jdk8u144.b01-31.upstream",
        "openjdk8-win-jdk8u162.b00-0.upstream",
        "openjdk8-win-aarch64.jdk8u144.b01-0.aarch64.upstream",
        "openjdk8-win-aarch64.shenandoah.jdk8u144.b01-0.aarch64.shenandoah.upstream",
        "openjdk8-win-jdk8u144.b01-27.dev.upstream",
        "openjdk8-win-aarch64.jdk8u144.b01-1.aarch64.upstream",
        "openjdk8-win-jdk8u144.b01-36.dev.upstream",
        "openjdk8-win-aarch64.shenandoah.jdk8u144.b02-0.aarch64.shenandoah.upstream",
        "openjdk8-win-aarch64.jdk8u144.b02-1.aarch64.upstream",
        "openjdk8-win-jdk8u152.b03-28.dev.upstream",};
    String[] ojdk8 = new String[]{
        "java-1.8.0-openjdk-1.8.0.131-10.b12.el7 some garbage",
        "java-1.8.0-openjdk-1.8.0.141-1.b16.el7_3",
        "java-1.8.0-openjdk-1.8.0.141-2.b16.el7_4",
        "java-1.8.0-openjdk-1.8.0.141-1.b16.el7_3",
        "java-1.8.0-openjdk-1.8.0.141-3.b16.el7",
        "java-1.8.0-openjdk-1.8.0.144-0.b01.el7_4",
        "java-1.8.0-openjdk-jdk8u144.b01-31.upstream     more garbage",
        "java-1.8.0-openjdk-1.8.0.144-1.b01.el7",
        "java-1.8.0-openjdk-jdk8u162.b00-0.upstream",
        "java-1.8.0-openjdk-jdk8u162.b00-0.upstream",
        "java-1.8.0-openjdk-aarch64.jdk8u144.b01-0.aarch64.upstream",
        "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u144.b01-0.aarch64.shenandoah.upstream",
        "java-1.8.0-openjdk-jdk8u144.b01-27.dev.upstream",
        "java-1.8.0-openjdk-jdk8u144.b01-36.dev.upstream",
        "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u144.b02-0.aarch64.shenandoah.upstream",
        "java-1.8.0-openjdk-aarch64.jdk8u144.b01-1.aarch64.upstream",
        "java-1.8.0-openjdk-aarch64.jdk8u144.b02-1.aarch64.upstream",
        "java-1.8.0-openjdk-jdk8u144.b01-44.dev.upstream",
        "java-1.8.0-openjdk-jdk8u162.b00-350.dev.upstream",
        "java-1.8.0-openjdk-aarch64.jdk8u144.b01-0.aarch64.upstream",
        "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u144.b02-1.aarch64.shenandoah.upstream",
        "java-1.8.0-openjdk-jdk8u162.b00-351.dev.upstream",
        "java-1.8.0-openjdk-jdk8u162.b00-351.dev.upstream",
        "java-1.8.0-openjdk-jdk8u162.b00-359.dev.upstream",
        "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u144.b02-78.aarch64.shenandoah.upstream",
        "java-1.8.0-openjdk-jdk8u162.b01-2.upstream",
        "java-1.8.0-openjdk-1.8.0.144-2.b01.el7",
        "java-1.8.0-openjdk-aarch64.shenandoah.jdk8u144.b02-78.aarch64.shenandoah.upstream",
        "java-1.8.0-openjdk-1.8.0.144-1.b01.el7_4",
        "java-1.8.0-openjdk-1.8.0.151-1.b12.el7",
        "java-1.8.0-openjdk-1.8.0.151-1.b12.el7_4",
        "java-1.8.0-openjdk-1.8.0.151-3.b12.el7",
        "java-1.8.0-openjdk-1.8.0.151-4.b12.el7",
        "java-1.8.0-openjdk-jdk8u152.b03-28.dev.upstream"};

    @Test
    public void checTruePredicate() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertFalse(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("java-1.8.0-openjdk-jdk8u162.b00-351.dev.upstream"));
    }
    @Test
    public void checTruePredicateWithGarbage() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertFalse(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("java-1.8.0-openjdk-1.8.0.131-10.b12.el7"));
    }
    @Test
    public void checTruePredicateWithMoreGarbage() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertFalse(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("java-1.8.0-openjdk-jdk8u144.b01-31.upstream"));
    }
    
     @Test
    public void checFalsePredicate() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("blah"));
    }
    
    @Test
    public void checFalsePredicateWithGarbage() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("java-1.8.0-openjdk-1.8.0.131-10.b12.el7 some garbage"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("some garbage"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("some"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("garbage"));
    }
    @Test
    public void checFalsePredicateWithMoreGarbage() throws IOException {
        List<String> l = Arrays.asList(ojdk8);
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("java-1.8.0-openjdk-jdk8u144.b01-31.upstream     more garbage"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("more garbage"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("more"));
        assertTrue(NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromStream(l.stream()).test("garbage"));
        
    }

}
