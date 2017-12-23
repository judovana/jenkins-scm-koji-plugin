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
package org.fakekoji.xmlrpc.server.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.sshd.server.SshServer;
import org.fakekoji.xmlrpc.server.SshUploadService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jvanek
 */
public class TestSshApi {

    private static File kojiDb;
    private static File sources;
    private static File secondDir;
    private static File priv;
    private static File pub;
    private static int port;
    private static SshServer sshd;
    private static final boolean debug = true;

    @BeforeClass
    public static void startSshdServer() throws IOException, GeneralSecurityException {
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        final File keys = File.createTempFile("ssh-fake-koji.", ".TestKeys");
        keys.delete();
        keys.mkdir();
        priv = new File(keys, "id_rsa");
        pub = new File(keys, "id_rsa.pub");
        createFile(priv, IDS_RSA);
        createFile(pub, IDS_RSA_PUB);
        Set<PosixFilePermission> perms = new HashSet<>();
        Files.setPosixFilePermissions(pub.toPath(), perms);
        Files.setPosixFilePermissions(priv.toPath(), perms);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(pub.toPath(), perms);
        Files.setPosixFilePermissions(priv.toPath(), perms);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                priv.delete();
                pub.delete();
                keys.delete();
            }
        });
        s.close();
        SshUploadService server = new SshUploadService();
        kojiDb = File.createTempFile("ssh-fake-koji.", ".root");
        kojiDb.delete();
        kojiDb.mkdir();
        kojiDb.deleteOnExit();
        sshd = server.setup(port, kojiDb, "tester=" + pub.getAbsolutePath());
        sources = File.createTempFile("ssh-fake-koji.", ".sources");
        sources.delete();
        sources.mkdir();
        sources.deleteOnExit();
        secondDir = File.createTempFile("ssh-fake-koji.", ".secondDir");
        secondDir.delete();
        secondDir.mkdir();
        secondDir.deleteOnExit();
    }

    @AfterClass
    public static void stopSshd() throws IOException {
        sshd.stop(true);
    }

    private static void createFile(File path, String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(content);
        }
    }

    private static String readFile(File path) {
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                return br.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "impossible";
        }
    }

    private static int scpTo(String target, String... source) throws InterruptedException, IOException {
        return scpTo(new String[0], target, null, source);
    }

    private static int scpTo(String target, File cwd, String... source) throws InterruptedException, IOException {
        return scpTo(new String[0], target, cwd, source);
    }

    private static int scpTo(String[] params, String target, File cwd, String... source) throws InterruptedException, IOException {
        String fullTarget = "tester@localhost:" + target;
        return scpRaw(params, fullTarget, cwd, source);
    }

    private static int scpFrom(String target, String source) throws InterruptedException, IOException {
        return scpFrom(new String[0], target, null, source);
    }

    private static int scpFrom(String target, File cwd, String source) throws InterruptedException, IOException {
        return scpTo(new String[0], target, cwd, source);
    }

    private static int scpFrom(String[] params, String target, File cwd, String source) throws InterruptedException, IOException {
        String fullSource = "tester@localhost:" + source;
        return scpRaw(params, target, cwd, fullSource);
    }

    private static int scpRaw(String[] params, String target, File cwd, String... source) throws InterruptedException, IOException {
        title(3);
        List<String> cmd = new ArrayList<>(params.length + source.length + 9);
        cmd.add("scp");
        //cmd.add("-v"); //verbose 
        cmd.add("-o");
        cmd.add("StrictHostKeyChecking=no");
        cmd.add("-i");
        cmd.add(priv.getAbsolutePath());
        cmd.add("-P");
        cmd.add("" + port);
        cmd.addAll(Arrays.asList(params));
        cmd.addAll(Arrays.asList(source));
        cmd.add(target);
        if (debug) {
            for (int i = 0; i < source.length; i++) {
                String string = source[i];
                System.out.println(i + ". scp from " + string);
                System.out.println("   scp to   " + target);
            }
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null) {
            pb.directory(cwd);
        }
        Process p = pb.start();
        if (debug) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                System.out.println(s);
            }
        }
        int i = p.waitFor();
        if (debug) {
            System.out.println(" === scpEnd === ");
        }
        return i;
    }

    @After
    public void cleanSecondDir() {
        clean(secondDir);
    }

    @After
    public void cleanSources() {
        clean(sources);
    }

    @After
    public void cleanKojiDb() {
        clean(kojiDb);
    }

    private void clean(File f) {
        File[] content = f.listFiles();
        for (File file : content) {
            deleteRecursively(file);
        }
        Assert.assertTrue(f.isDirectory());
        Assert.assertTrue(f.listFiles().length == 0);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] content = file.listFiles();
            for (File f : content) {
                deleteRecursively(f);
            }
        }
        file.delete();
    }

    private static void checkFileExists(File f) {
        if (debug) {
            System.out.println(f + " is supposed to exists. f.exists() is: " + f.exists());
            if (f.exists()) {
                System.out.println("content: '" + readFile(f) + "'");
            }
        }
        Assert.assertTrue(f + " was supposed to exists. was not", f.exists());
    }

    private static void checkFileNotExists(File f) {
        if (debug) {
            System.out.println(f + " is supposed to NOT exists. f.exists() is: " + f.exists());
            if (f.exists()) {
                System.out.println("content: '" + readFile(f) + "'");
            }
        }
        Assert.assertFalse(f + " was supposed to NOT exists. was", f.exists());
    }

    private static void title(int i) {
        if (debug) {
            String s = "method-unknow";
            if (Thread.currentThread().getStackTrace().length > i) {
                s = Thread.currentThread().getStackTrace()[i].getMethodName();
            }
            System.out.println(" ==" + i + "== " + s + " ==" + i + "== ");
        }
    }

    private class NvraTarballPathsHelper {

        private final String vid;
        private final String rid;
        private final String aid;

        public NvraTarballPathsHelper(String id) {
            this(id, id, id);
        }

        public NvraTarballPathsHelper(String vid, String rid, String aid) {
            this.vid = vid;
            this.rid = rid;
            this.aid = aid;
        }

        public String getName() {
            return "terrible-x-name-version" + vid + "-release" + rid + ".arch" + aid + ".suffix";
        }

        public String getStub() {
            return "terrible-x-name/version" + vid + "/release" + rid + "/arch" + aid;
        }

        public String getContent() {
            return "nvra - " + vid + ":" + rid + ":" + ":" + aid;
        }

        public String getStubWithName() {
            return getStub() + "/" + getName();
        }

        public File getLocalFile() {
            return new File(sources, getName());
        }

        public File getSecondaryLocalFile() {
            return new File(secondDir, getName());
        }

        public void createLocal() throws IOException {
            createFile(getLocalFile(), getContent());
            checkFileExists(getLocalFile());
        }

        public void createSecondaryLocal() throws IOException {
            createFile(getSecondaryLocalFile(), getContent());
            checkFileExists(getSecondaryLocalFile());
        }

        public void createRemote() throws IOException {
            getRemoteFile().getParentFile().mkdirs();
            createFile(getRemoteFile(), getContent());
            checkFileExists(getRemoteFile());
        }

        public File getRemoteFile() {
            return new File(kojiDb, getStubWithName());
        }

    }

    @Test
    /*
     * scp /abs/path/nvra tester@localhost:
     */
    public void scpNvraAbsPathsTo() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1t1");
        nvra.createLocal();
        int r = scpTo("", nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     *scp tester@localhost:nvra /abs/path/
     */
    public void scpNvraAbsPathsFrom1() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1f1");
        nvra.createRemote();
        int r2 = scpFrom(nvra.getLocalFile().getParent(), nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());

    }

    @Test
    /*
     *scp tester@localhost:/nvra /abs/path
     */
    public void scpNvraAbsPathsFrom2() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1f2");
        nvra.createRemote();
        int r3 = scpFrom(nvra.getLocalFile().getParent(), "/" + nvra.getName());
        Assert.assertTrue(r3 == 0);
        checkFileExists(nvra.getLocalFile());
    }

    @Test
    /*
     * scp /abs/path/nvra tester@localhost:/nvra
     */
    public void scpNvraAbsPathsRenameLikeTo() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t1");
        nvra.createLocal();
        int r = scpTo("/" + nvra.getName(), nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp tester@localhost:nvra /abs/path/nvra
     */
    public void scpNvraAbsPathsRenameLikeFrom1() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t1");
        nvra.createRemote();
        int r1 = scpFrom(nvra.getLocalFile().getAbsolutePath(), nvra.getName());
        Assert.assertTrue(r1 == 0);
        checkFileExists(nvra.getLocalFile());
    }

    @Test
    /*
     * scp tester@localhost:nvra /abs/path/nvra2
     */
    public void scpNvraAbsPathsRenameLikeFrom2() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t2");
        nvra.createRemote();
        int r2 = scpFrom(new File(nvra.getLocalFile().getParent(), "nvra2").getAbsolutePath(), nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(new File(nvra.getLocalFile().getParent(), "nvra2"));
    }

    /*
     * scp nvra tester@localhost:nvra
     * scp tester@localhost:nvra .
     * scp tester@localhost:nvra ./nvra2
     */

 /*
     * scp /abs/path/nvra tester@localhost:/some/garbage
     * scp tester@localhost:some/garbage/nvra /abs/path/
     */
    private static final String IDS_RSA = "-----BEGIN RSA PRIVATE KEY-----\n"
            + "MIISKQIBAAKCBAEA6DOkXJ7K89Ai3E+XTvFjefNmXRrYVIxx8TFonXGfUvSdNISD\n"
            + "uZW+tC9FbFNBJWZUFludQdHAczLCKLOoUq7mTBe/wPOreSyIDI1iNnawV/KsX7Ok\n"
            + "yThsDolKxgRA+we8JuUYAes2y94FKaw4kAY/Ob16WSf7AP9Y8Oa4/PcK6KCIkzQx\n"
            + "iqL+SGG3mLy+XhTU/pJYnEC8c1lw+Gah8oPWG1vx5W578iWixgTbNp0TTXNr1+jU\n"
            + "xVRg1SitC4WP8g67af6f5rhcJZt5Dz/gWajHqKkK97nmPSDttso56ueeUW3L8lM3\n"
            + "scFjGQu3QbpLmFpMZeTpePOn7CjVjfBZnocNzDdqkgE+ivEB7nWWNbgEwALX4NR4\n"
            + "DzkGGoFPUKdsIdEBC5D73XC6NJxHKWOO9L+KyUxoeA8hUHBuWc3pVSi7NyG04Pvq\n"
            + "EfO0Ea1p4Tn2nb2CreEgOyCQ/nLJJrPEDef+8GUKKs3tVawbOn6tLFyi7aFY9u35\n"
            + "KX3fgt5t8TiIqmcIxs7oC16ny/97pe7gBRZuLU4AxK76m6Nxr1lGw7wJOXllhoKb\n"
            + "Zys676qlIG6VvSw/dAUekY/Vk9duIm9BHHLVZetv/LL4hWNy7xMvlpl8V141azJw\n"
            + "BE10V1GuvJnRtsf57ZqSOjiezTewavteJLwTZlRTV7QwsVpUGM7YyYJacv1GkFzz\n"
            + "00UWg+/Pf1gyLHf+JgsWx1ftUU9WlnVgBSfZpWKHexjcKrJzk4n96HWFL4ihCfxd\n"
            + "895ZkBsbTyuRAsC3Tw+k/FC6j0bbM5eE9weBH9b3Ap6tZFlIwEwXCvU28i0SbxEq\n"
            + "PXpnxpK33XRHz3byu8QtX4BulVrwa0P/ZeY0fvooUJo+tUqHbE7Pf5DlzylZarXQ\n"
            + "BSHjPcG+bs37LA2ar/1o6hRXJ5Z3b8f4YH3HumGuiYY3NjDvq++P5RTnasWFN7+f\n"
            + "1xYexB3HZE9RXxCTTVIn09fVHnaloNCnvhl6fn40TfsmS10qN1hwh55nzvpUjqGz\n"
            + "qyC+9Z/F+RpqIIAVKyijyMy2PiJRBy8sPZa1qf9+xDCZsRYdUPebr8t38sPEYYtR\n"
            + "8wMfY1iq0qXn1Pi/G+ghmTn+/UIdubn/m7Iu/F0GSyN6cjz4fskpS5bAb1oJhoWD\n"
            + "yrx6EAf96OoVIrMQcaBkLVcd7MW59rSDK0gNK7DE3ZXa85VwcMj/Y9HCybY/Xlf5\n"
            + "v7Csi38E+ybHP2R3YEPMduLg3hrs0UVfQaZoirA/yVtvVF3mv5Nh/RfCvCvGGR2s\n"
            + "PNgnq7ZKia5/nXqV4/l2Yrrg2QPYtAYwuv2Sf6GK5MqhqBtWb8qZRZVH7df4IroJ\n"
            + "orWmi2obRqdU5+w2F+OaL7OOs8GIwLb9iGeswwIDAQABAoIEAQC9ZPnoLgEeMyNs\n"
            + "DWM+GbfozXYt9OqEs/VwJLvOx9GLaUgcgQWsRw7Ai1oVzCZz6e4mOl2fRQWzMLCb\n"
            + "YEaoAk6HvEtEh7vSX1cs3dlA0Thu09pzSOTc16+Tf7pEn02dM6btFqmpTwBn8tTF\n"
            + "M9sC5oWFhB4aQHkETEJwY9B5TMtSCTa80rKiAOZlhYaqBzFDLby5VAcAk/DiKQ7z\n"
            + "HUt0ssHdmPZKC/7++GG3IFjpR99pqf5JoniB55v/4Wib4DoT1p5ZCz3Dg5Ztek2Y\n"
            + "+aH1n6wSzqbKfo/kRkp+cJ4jEv7YLjVOlz/zNeitkhfMfbaRMv3jkn44kIzkHD5r\n"
            + "wqJmooPHkV/UbT1lOMU5iiGV+V2ue+M3WDYBPKLU1aorABQ71O0EUSKOcRcAOIP2\n"
            + "p2UADoeWP0NqwfSLVtk7WK+8LTfe9RhC9lbqg5vZW1fkRFH6QYwoZVrTv3FkiZ22\n"
            + "eqQsL5GK5O8RENxHp9ShtpdrerfOGW+mIV680BWR+fk06sbWLqpC9prgQzmcM+vX\n"
            + "4WpJ3AzL2TbZNlvkvMDKpIgKuQHRJkqAF2HIGcO9nrOHK4vpPAEZkd9oHSi4qNwF\n"
            + "LDewi52xvwKd3CDHM+GYTU7giJqZ7JantAEYEVEWs+JRpSkf7CbX/d7NrEci3gyA\n"
            + "hj04u0sbiSZdf/TDhAjaH0VFv5Qk+xEf4NbGvL+UZse/WYCINYQNSbLZCH3ZmnFn\n"
            + "3JG/vlAB1ojpAHJb2Eg2zTIcPw//ocig27ZvzZG1fwebLB0dwPoMOtaeh66hzSv/\n"
            + "TTduDJsiLg/x/I9Fbw/uJd6DSypndSCq+BNUy3g32umnsmj4x3XTnfn56d2E5n2w\n"
            + "ivMxYaFlyMTJ3ilJJEpk2ioLzlWYVhZFMielmrpsE8EM4Rnv+lVfPzu7VDSIyMDI\n"
            + "L6VPLRG4+wakSajacwKBGfVB/wEWrhGxQb9uHdFcXbzNmx2m/70S5LEiOTmTv47g\n"
            + "rSD7bPxQ9ghx9XcXnoFjts+Crz9Pl0NbmMCJQ2KTPmAXCMJNBC0Xof3yrP0+ZKM+\n"
            + "ZNXTAhCPesDTgOmYtMnyKZa0XxzCf5DgBYa6zzT484w0qZcMRYewFLJEv0oD2cwC\n"
            + "WSbjvvCmBJLHxp9+L3H5uE4hMLuZhdIV7+KrwTetylRNxjM7wi8w0uUUE9/6uHK4\n"
            + "Wy4DA7kfjPmvGhbbv/63baVQRRCs7M8Sk6rUD+JZ+ZKzVK31+j8WwNT7zbXte2qs\n"
            + "NCnEPrGvJRKb3arq6VMJXzLlknFUbOiO6S8EvgeglunPtqyadMIhNQdk7cW+4hzq\n"
            + "tY4aNtT4zdozmji/WzDBSPMhIYh0CV7zSgVuFS/SsWyAWDHX4IvZCoHLlg+bROaE\n"
            + "NdnX6B6JAoICAQD9V6GdwcUsgbH0jJb3dhQ0Zpqcrrl8U496msYkg+b6MyqX55Ev\n"
            + "YItv16grvhl7QupK93pD256zGm+83e6Jxox4ZxLLmgx6fH3ubkRC7CbpRWtb7R1o\n"
            + "3YENjo8wmbjjcs38tUG5LEHFsZOeyVWkkW+/PCuOUl4m/3m7BOiwEYIA6vYdC42F\n"
            + "kBH8FaRi2zZ+kiHX2vHIbdpb7H013gSZt8ieKrLJh6FESFSINuW4ISGbCgn9nEw7\n"
            + "uAG7EMwQliCe66SIx+aYxaaxJL0q0tJkX/glzO//9Fu5LX+n5nCgqcTv2TB5IANi\n"
            + "fyi/YrbaX1hUxJFYyh39a1rIpjk4wSZwXZIC6ZHc3CKu5mNhML4a809W5siKXR/9\n"
            + "hQqiSeCIXuBq96E+s9kGNp9hLBUgQGooZwMZdVgOX0dSf4E6KoVz5c4PTEuJO7K8\n"
            + "wxlTfegUxzKzYi+A29lNHiYJF51CSJGr7Z1VFWpVUa7Ts953SgsfIYqB1Yrflgtn\n"
            + "vJi3+FDuNLPIMeVJLDdp8tXBUDnDzZL4eRdQEIzmYJuExEj3g3i8Vmd1fJ5yhtEd\n"
            + "7N93KVstqp8mGvYYnfCfXZUiwn1ivRgzfeYR+siTKuTXmwhsXBcy6Z5cLZOatjUC\n"
            + "uBME4We4ra9AoXc6iU9Zrn8zAEMeEtl8QSiIk2OJkqTIaWck1Li6RpRJVwKCAgEA\n"
            + "6qM8dzcv6naHOSJmbdZT8c8yVVAEbhkgj22mdfHJa437o0dqXm8oKyDhucDhG1kx\n"
            + "Uj4lBlx7vH5uDJJ3YbrsLQcQhf1r7ol84VAlJmm9OF6horLKRWc2Eke8LbtRw3VV\n"
            + "9M9fOP4b5gwJFeVOYHK61iKRwDw+qs1GX/5jOQLPdDC+BSzLFCGvDCFk6BrAadcn\n"
            + "IVfJ+7Sid9v30kpUPGpDiJzZ0Ofb6+1ppcW/YlZE6OPBJMD0cIcd3ogMFPBA+ZuQ\n"
            + "zLPf3PVbEUxDNLOCjTvLie5jvGvBTXrGSvXXTRt+rpBkkrd6GqM3jKBEOPMxWiFm\n"
            + "Uiw8WqOKKC46FpiUAD82uJddnd1R/dM1cbtJiF3QX5CEGbaKdAWzoG0CODw5tyX1\n"
            + "Cxgy43PCQafz/AOKAecNp6eCWkyEbjCBsCrJg3lKfQWyJ0MqToadSA+iDUYRA6PB\n"
            + "sEJWiy8UtnbwgDtTtUNKxWhbGOLxGRQnnSo7USE93ew8tuWa9oW9S/BEF7AiCk+S\n"
            + "HCINMB7ek2QXzFqS3h7WBSzDn+ZsSOo3EH5I5NV0EfgeGfAnC9mKLc9wM6XiuBOY\n"
            + "eLiFPc6nLlB8jJpEFioK44oQl4qV8NCP4Dwz2r+iLUt8E8vQAypYNZrZTLEktIL9\n"
            + "Egl+3+bUPgMJOVqwI4YCkICnJjnlVDkBL2xpFmsOGHUCggIAEvUDuvJM9s+dqVb7\n"
            + "1PiY+nLTDvZkGtGF4v7B5OmZ1w8NGODTFGB9DplslBldfsO7FHEATSOZ9Hz973wL\n"
            + "5XNd/4R2+5VDacb3BWhq4zcYkkwHhJFxqe8pQQJx5IkcNKjakRZfHKQbJ9fp2+/k\n"
            + "4LOhUQYHnFa9hN2JFl1/q+0jdT4fvHyo0l29eseDzYHpyf7VWXmgrgbKWCaSF/3N\n"
            + "ClOeR3eaeUoU3y8qZCb3eZfBFADkTn3rlmxmdMEFBBi3yCyJ21JaBwSDPK4rGZE8\n"
            + "/RXRU8LKErUOSAUHkGDF/L+3ZNszrVyf5DbvraKNXDnWOkGbPrGhHN1zpaAKmByb\n"
            + "67yUuHMR3xz522yR8yvajdm3DiGmz/O3+RiDezFcA9hVoqt0/WQn0Tc1JehOjGNF\n"
            + "jlBnAvis5iZrB9lSqi+UXN/NU4e5/0LgVQ+kTYMWYrelK5clRtcso4CmB/gkZFlZ\n"
            + "zSuyojNACbJbCqxi8ToxKtsvqhd4lNJ9d/28z8ddBvYandhd9+O/IcZyCE0ghW5U\n"
            + "mRM2k18pq/N+r6igbSUBW9Z7V2dD0/4Sl9KpxhjqIbiqwAc0cxMedk5iYn97MnBD\n"
            + "51Z8aMwDRj/nb9rB/pnFgqHIn80pRmJsBRARHERhpogYnRV3/oFX1rYf/oj+fLmc\n"
            + "XJfjmJSu1hSLEBQTC8Z/LDEr13ECggIBAOlnB7bvRtLMpSbIeWu5UDeyDDehKUb7\n"
            + "58/FG1kn81zyF+cMG1tk52g/hUrp+wLhbpaJCvuQ8+VFPuNyrx6gel8wL9eZh8v5\n"
            + "KChZORtFA90XBWJ6x4rSaI82nJJBS8xK4/5qaiafX9EvF7qYJ6b5ebGZIbNAOnZd\n"
            + "TCwhOUJ08Th7ZApxzHFyMFa4wU/BjLW8OEiKs3mW7iacwaCGH9UZP6Sdom6Utcey\n"
            + "mu00EHUZq+Ke7HpLFtz5C1VZr+sEMx4ZCakXJRD/YF+MpS2/g5ZKbOYAJWZBKkCQ\n"
            + "aMAYXNtvBk1PhTwNF4F36sIQisy73dPydX44UrE3DS97DH19uXulZiGpMI7gobcE\n"
            + "ap1/2F22NJlbgIyzcHaJVW24AgU+o4r0TxWCNNzdQddd4u5F9vp9hK/JiXmZtAKI\n"
            + "bfl4FoyaEubay6USwvrqHXqZUnIxyKr+MqXK15wMcWYwWny0h0hAcBh+/l97IKn5\n"
            + "yo4kfGzvzEL9xEeLjuK7ltn7X0DRDIuFK6qglM3RZ0bmwmWdk4sw0WTEarSc2gqO\n"
            + "MchOVuSLELLvRcI3ih/XfgSj3NEDqsvBcmJj6ubYsqT3m22h5yjFGZ/Or0KPsSej\n"
            + "z/sW594p0oGMHRj0HS+I58YrCw2nCQQnaOaQW40OaQJmsr5C4AP2QobL83mrDd0B\n"
            + "95PdG4wZYiQhAoICAAEQjP1dbHOT/4oycWukZIayEN3rGJbwjLJcM5n+VVMMprDE\n"
            + "2Bx9YmjijOMRHqCZY7rDr6OMhnuI1E2gkAbTfHfLWM3OpnagdL73uAjYWoU8QIZl\n"
            + "8NS3u8lNbDQby4WIbmtvIbPbKrnV47illLfdfcMvp16E+zXpgBERZfA5kjdvL2Zs\n"
            + "CoKitgJmes6Cw7gpxq04wuuq5xJ856rPmwiFTJCJGoN5yXQYZsqNsz1iWvjTVUen\n"
            + "JjNj5aveSb1a34JEqlLxk2NoYrT47A54iMJQSqXQQDwbW2U0vOZMv9FtwjJti9EO\n"
            + "aIma8wcoQhLGqt9/yQ66BzLCCQfnxMDgGUK7RD85Jcq7WghgSJv8iRQ4Hu8J7tTO\n"
            + "SZJOJy9IDmSp/Yh5R/9KMEdgvUQRo4JFqORyguokkKTa2EYYPpoDfCv1uEg8rr+4\n"
            + "xtKBxoiw+UJZGnEbUGJw2Yt2ynbV4dUlUkgMOBrznYdtmivaF544DD8Tgk8elNdT\n"
            + "WowvdHlPa9HCY7VNWo80vaOUXUSo6ftUkTVhyd9EI84g1JyFGPcpd624NsAhQrSm\n"
            + "kg8qubbQM3Tij5oGYewJ2PzeweosiQ4AVlq3+mVGnkNLPLyABHOgGmoUPpyR297R\n"
            + "nDe3iQJfqKGHQck94dCtx7mlbcgBwWiM1SCdZcJ8H06gBWA99hNqcwWS9kSv\n"
            + "-----END RSA PRIVATE KEY-----\n";

    private static final String IDS_RSA_PUB = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAEAQDoM6Rcnsrz0CLcT5dO8WN582ZdGthUjHHxMWidcZ9S9J00hIO5lb60L0VsU0ElZlQWW51B0cBzMsIos6hSruZMF7/A86t5LIgMjWI2drBX8qxfs6TJOGwOiUrGBED7B7wm5RgB6zbL3gUprDiQBj85vXpZJ/sA/1jw5rj89wrooIiTNDGKov5IYbeYvL5eFNT+klicQLxzWXD4ZqHyg9YbW/HlbnvyJaLGBNs2nRNNc2vX6NTFVGDVKK0LhY/yDrtp/p/muFwlm3kPP+BZqMeoqQr3ueY9IO22yjnq555RbcvyUzexwWMZC7dBukuYWkxl5Ol486fsKNWN8Fmehw3MN2qSAT6K8QHudZY1uATAAtfg1HgPOQYagU9Qp2wh0QELkPvdcLo0nEcpY470v4rJTGh4DyFQcG5ZzelVKLs3IbTg++oR87QRrWnhOfadvYKt4SA7IJD+cskms8QN5/7wZQoqze1VrBs6fq0sXKLtoVj27fkpfd+C3m3xOIiqZwjGzugLXqfL/3ul7uAFFm4tTgDErvqbo3GvWUbDvAk5eWWGgptnKzrvqqUgbpW9LD90BR6Rj9WT124ib0EcctVl62/8sviFY3LvEy+WmXxXXjVrMnAETXRXUa68mdG2x/ntmpI6OJ7NN7Bq+14kvBNmVFNXtDCxWlQYztjJglpy/UaQXPPTRRaD789/WDIsd/4mCxbHV+1RT1aWdWAFJ9mlYod7GNwqsnOTif3odYUviKEJ/F3z3lmQGxtPK5ECwLdPD6T8ULqPRtszl4T3B4Ef1vcCnq1kWUjATBcK9TbyLRJvESo9emfGkrfddEfPdvK7xC1fgG6VWvBrQ/9l5jR++ihQmj61SodsTs9/kOXPKVlqtdAFIeM9wb5uzfssDZqv/WjqFFcnlndvx/hgfce6Ya6Jhjc2MO+r74/lFOdqxYU3v5/XFh7EHcdkT1FfEJNNUifT19UedqWg0Ke+GXp+fjRN+yZLXSo3WHCHnmfO+lSOobOrIL71n8X5GmoggBUrKKPIzLY+IlEHLyw9lrWp/37EMJmxFh1Q95uvy3fyw8Rhi1HzAx9jWKrSpefU+L8b6CGZOf79Qh25uf+bsi78XQZLI3pyPPh+ySlLlsBvWgmGhYPKvHoQB/3o6hUisxBxoGQtVx3sxbn2tIMrSA0rsMTdldrzlXBwyP9j0cLJtj9eV/m/sKyLfwT7Jsc/ZHdgQ8x24uDeGuzRRV9BpmiKsD/JW29UXea/k2H9F8K8K8YZHaw82CertkqJrn+depXj+XZiuuDZA9i0BjC6/ZJ/oYrkyqGoG1ZvyplFlUft1/giugmitaaLahtGp1Tn7DYX45ovs46zwYjAtv2IZ6zD tester";
}
