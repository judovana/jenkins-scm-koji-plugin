package hudson.plugins.scm.koji;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;

public class KojiSCMtest {

    private static void strToFile(String s, File f) throws FileNotFoundException, IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write(s);
        }
    }

    private static void strsToFile(String[] s, File f) throws FileNotFoundException, IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            for (String s1 : s) {
                bw.write(s1);
                bw.newLine();
            }
        }
    }

    private static String[] fileToStrings(File f) throws IOException {
        return fileToString(f).split("\n");
    }

    private static String fileToString(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader bw = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            while (true) {
                String s = bw.readLine();
                if (s == null) {
                    break;
                }
                sb.append(s).append("\n");
            }
        }
        return sb.toString();
    }

    String[] s1 = new String[]{
        "java-1.8.0-openjdk-1.8.0.71-0.b15.el7_2",
        "java-1.8.0-openjdk-1.8.0.71-1.b15.el7_2",
        "java-1.8.0-openjdk-1.8.0.71-2.b15.el7_2",
        "java-1.8.0-openjdk-1.8.0.72-0.b15.el7",
        "java-1.8.0-openjdk-1.8.0.72-1.b15.el7",
        "java-1.8.0-openjdk-1.8.0.72-2.b15.el7",
        "java-1.8.0-openjdk-1.8.0.72-3.b15.el7",
        "java-1.8.0-openjdk-1.8.0.72-4.b15.el7",
        "java-1.8.0-openjdk-1.8.0.72-5.b16.el7",
        "java-1.8.0-openjdk-1.8.0.72-12.b16.el7",
        "java-1.8.0-openjdk-1.8.0.72-13.b16.el7",
        "java-1.8.0-openjdk-1.8.0.77-0.b03.el7",
        "java-1.8.0-openjdk-1.8.0.77-0.b03.el7_2",
        "java-1.8.0-openjdk-1.8.0.77-1.b03.el7",
        "java-1.8.0-openjdk-1.8.0.77-2.b03.el7",
        "java-1.8.0-openjdk-1.8.0.91-0.b14.el7_2",
        "java-1.8.0-openjdk-1.8.0.91-1.b14.el7",
        "java-1.8.0-openjdk-1.8.0.91-2.b14.el7",
        "java-1.8.0-openjdk-1.8.0.91-3.b14.el7",
        "java-1.8.0-openjdk-1.8.0.92-1.b14.el7",
        "java-1.8.0-openjdk-1.8.0.92-2.b14.el7"
    };

    String[] s2 = new String[]{
        "java - 1.8.0-openjdk - 1.8.0.65-3.b17.el6_7",
        "java-1.8.0-openjdk-1.8.0.65-4.b17.el6",
        "java-1.8.0-openjdk-1.8.0.65-5.b17.el6",
        "java-1.8.0-openjdk-1.8.0.71-0.b15.el6_7",
        "java-1.8.0-openjdk-1.8.0.71-1.b15.el6",
        "java-1.8.0-openjdk-1.8.0.71-1.b15.el6_7",
        "java-1.8.0-openjdk-1.8.0.71-2.b15.el6",
        "java-1.8.0-openjdk-1.8.0.71-3.b15.el6",
        "java-1.8.0-openjdk-1.8.0.71-4.b15.el6",
        "java-1.8.0-openjdk-1.8.0.71-5.b15.el6",
        "java-1.8.0-openjdk-1.8.0.77-0.b03.el6_7",
        "java-1.8.0-openjdk-1.8.0.77-1.b03.el6",
        "java-1.8.0-openjdk-1.8.0.77-2.b03.el6",
        "java-1.8.0-openjdk-1.8.0.91-0.b14.el6_7",
        "java-1.8.0-openjdk-1.8.0.91-1.b14.el6",
        "java-1.8.0-openjdk-1.8.0.91-1.b14.el6"
    };

    String[] s3
            = new String[]{
                "java-1.8.0-openjdk-1.8.0.72-13.b16.fc24",
                "java-1.8.0-openjdk-1.8.0.91-2.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-3.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-4.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-4.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-6.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-7.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-5.b14.fc24",
                "java-1.8.0-openjdk-1.8.0.91-8.b14.fc24"

            };

    @Test
    public void testStringAddedCorrectly() throws Exception {
        File tmp = File.createTempFile("KojiSCMtest", "processed");
        tmp.deleteOnExit();
        strsToFile(s1, tmp);
        String[] t1 = fileToStrings(tmp);
        Assert.assertArrayEquals(s1, t1);
        String[] sn = new String[]{"new-nvr-a"};
        String[] s11 = new String[s1.length + 1];
        System.arraycopy(s1, 0, s11, 0, s1.length);
        System.arraycopy(sn, 0, s11, s1.length, sn.length);
        KojiSCM.appendStringProcessed(tmp, sn[0]);
        String[] t2 = fileToStrings(tmp);
        Assert.assertArrayEquals(s11, t2);
    }

    @Test
    public void testBuildAddedCorrectly() throws Exception {
        File tmp = File.createTempFile("KojiSCMtest", "processed");
        tmp.deleteOnExit();
        strsToFile(s1, tmp);
        String[] t1 = fileToStrings(tmp);
        Assert.assertArrayEquals(s1, t1);
        String[] sn = new String[]{"new-nvr-a"};
        String[] s11 = new String[s1.length + 1];
        System.arraycopy(s1, 0, s11, 0, s1.length);
        System.arraycopy(sn, 0, s11, s1.length, sn.length);
        KojiSCM.appendBuildNvrToProcessed(tmp,
                new Build(0, "new", "nvr", "a", sn[0], "now", new ArrayList<>(), new HashSet<>(), false)
        );
        String[] t2 = fileToStrings(tmp);
        Assert.assertEquals(s11.length, t2.length);
        for (int x = 0; x < s11.length - 1; x++) {
            Assert.assertEquals(s11[x], t2[x]);
        }
        int x = s11.length - 1;
        String[] ss1 = s11[x].split(" +");
        String[] tt2 = t2[x].split(" +");
        Assert.assertEquals(ss1[0], tt2[0]);
        Assert.assertEquals("#", tt2[1]);
    }

}
