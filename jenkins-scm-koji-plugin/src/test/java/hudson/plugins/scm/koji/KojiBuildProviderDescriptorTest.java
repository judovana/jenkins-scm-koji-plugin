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

import hudson.util.FormValidation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static hudson.plugins.scm.koji.KojiBuildProvider.KojiBuildProviderDescriptor;

public class KojiBuildProviderDescriptorTest {

    @Test
    public void simpelValidValue() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r1 = descriptor.doCheckDownloadUrl("http://aaa.cz");
        FormValidation r2 = descriptor.doCheckTopUrl("http:///bbb.com");
        assertEquals(FormValidation.Kind.OK, r1.kind);
        assertEquals(FormValidation.Kind.OK, r2.kind);
    }

    @Test
    public void simpelInValidValue1() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r = descriptor.doCheckDownloadUrl("blah");
        assertEquals(FormValidation.Kind.ERROR, r.kind);
    }

    @Test
    public void simpelInValidValue2() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r = descriptor.doCheckTopUrl("blah");
        assertEquals(FormValidation.Kind.ERROR, r.kind);
    }

    @Test
    public void advacnedValidValue() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r1 = descriptor.doCheckDownloadUrl("http://aaa.cz:DPORT/bbb");
        FormValidation r2 = descriptor.doCheckTopUrl("http:///bbb.com:XPORT");
        assertEquals(FormValidation.Kind.OK, r1.kind);
        assertEquals(FormValidation.Kind.OK, r2.kind);
    }

    @Test
    public void advacnedValidValues() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r1 = descriptor.doCheckDownloadUrl("http://aaa.cz http://aaa.cz:DPORT");
        FormValidation r2 = descriptor.doCheckTopUrl("http://aaa.cz http:///bbb.com:XPORT/aaa");
        assertEquals(FormValidation.Kind.OK, r1.kind);
        assertEquals(FormValidation.Kind.OK, r2.kind);
    }

    public void advacnedInvalidValues1() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r1 = descriptor.doCheckDownloadUrl("http://aaa.cz aaa");
        assertEquals(FormValidation.Kind.OK, r1.kind);
    }

    public void advacnedInvalidValues2() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r2 = descriptor.doCheckTopUrl("http://aaa.cz aaa");
        assertEquals(FormValidation.Kind.OK, r2.kind);
    }

    public void advacnedInvalidValues3() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r1 = descriptor.doCheckDownloadUrl("bbb http://aaa.cz");
        assertEquals(FormValidation.Kind.OK, r1.kind);
    }

    public void advacnedInvalidValues4() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r2 = descriptor.doCheckTopUrl("ccc http://aaa.cz");
        assertEquals(FormValidation.Kind.OK, r2.kind);
    }

    public void advacnedHidenInvalidValues() {
        final KojiBuildProviderDescriptor descriptor = new KojiBuildProviderDescriptor();
        FormValidation r2 = descriptor.doCheckTopUrl("http://aaa.cz:DPORTbad");
        assertEquals(FormValidation.Kind.ERROR, r2.kind);
    }

}
