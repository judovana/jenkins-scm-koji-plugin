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
package org.fakekoji.xmlrpc.server.core;

/* all known tags in time of writing
     find  ~/jenkins/jenkins_home/jobs/ -name config.xml -exec grep tag {} \; | sort | uniq 
     <tag>dist-5*-extras*</tag>
     <tag>f23-*</tag>
     <tag>f23-updates*</tag>
     <tag>f24-*</tag>
     <tag>f24*</tag>
     <tag>f24-updates*</tag>
     <tag>openjdk-win-candidate</tag>
     <tag>oracle-java-rhel-5.*</tag>
     <tag>oracle-java-rhel-6.*</tag>
     <tag>oracle-java-rhel-7.*</tag>
     <tag>RHEL-5.*-candidate</tag>
     <tag>RHEL-6.*-candidate</tag>
     <tag>rhel-6.*-supp*</tag>
     <tag>rhel-7.*-candidate</tag>
     <tag>supp-rhel-7.*</tag>
 */
/**
 * This class is trying to emulate tags for static (and possibly other) builds, to some future wihtout need to recompile it with each new fedora.
 * 
 */
public class TagsProvider {

    static String[] getSuplementaryRhel5LikeTag() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = "dist-" + (i + Ffrom) + ".X-extras-fakeTag";

        }
        return r;
    }

    private static final int Ffrom = 22;
    private static final int Fto = 40;
    //we do not support rhel 5 and older anymore
    private static final int Rfrom = 6;
    private static final int Rto = 9;
    private static final int Wfrom = 0;
    private static final int Wto = 1;

    static String[] getFedoraTags() {
        String[] r = new String[Fto - Ffrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getFedoraBase((i + Ffrom));

        }
        return r;
    }

    static String getFedoraBase(int i) {
        return "f" + (i) + "-updates-fakeTag";
    }

    static String[] getWinTags() {
        String[] r = new String[Wto - Wfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = "openjdk-win-fakeTag";

        }
        return r;
    }

    static String[] getOracleTags() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getOracleBase(i + Rfrom);

        }
        return r;
    }

    private static String getOracleBase(int i) {
        return "oracle-java-rhel-" + i + ".X";
    }

    static String[] getRHELtags() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getRhel5Rhel6Base(i + Rfrom);

        }
        return r;
    }

    static String getRhel5Rhel6Base(int i) {
        return "RHEL-" + (i) + ".X-candidate";
    }

    static String[] getRhelTags() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getRhel7Base(i + Rfrom);
        }
        return r;
    }

    static String getRhel7Base(int i) {
        return "rhel-" + (i) + ".X-candidate";
    }

    static String[] getSuplementaryRhel6LikeTag() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getIbmRhel6Base(i + Rfrom);

        }
        return r;
    }

    private static String getIbmRhel6Base(int i) {
        return "rhel-" + (i) + ".X-supp-fakeTag";
    }

    static String[] getSuplementaryRhel7LikeTag() {
        String[] r = new String[Rto - Rfrom];
        for (int i = 0; i < r.length; i++) {
            r[i] = getIbmRhel7Base(i + Rfrom);

        }
        return r;
    }

    private static String getIbmRhel7Base(int i) {
        return "supp-rhel-" + (i) + "-X-fakeTag";
    }
}
