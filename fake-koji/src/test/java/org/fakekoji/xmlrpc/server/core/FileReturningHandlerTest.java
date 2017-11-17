package org.fakekoji.xmlrpc.server.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;


public class FileReturningHandlerTest {



    private final String[] ojdk7files = new String[] {
            "icedtea.2.7.0pre14",
            "icedtea.2.7.0pre12",
            "icedtea.2.7.0pre11",
            "icedtea.2.7.0pre10",
            "icedtea.2.7.0pre09",
            "icedtea.2.7.0pre07",
            "icedtea.2.7.0pre06",
            "icedtea.2.6.11",
            "icedtea.2.6.10",
            "icedtea.2.6.10pre01",
            "jdk7u151.b01",
            "jdk7u141.b02",
            "jdk7u131.b00",
            "jdk7u111.b01"
    };

    private final String[] ojdk8files = new String[] {
            "aarch64.jdk8u144.b02",
            "aarch64.jdk8u144.b01",
            "aarch64.jdk8u141.b16",
            "aarch64.jdk8u131.b13",
            "aarch64.jdk8u131.b12",
            "aarch64.jdk8u121.b14",
            "aarch64.shenandoah.jdk8u144.b02",
            "aarch64.shenandoah.jdk8u144.b01",
            "aarch64.shenandoah.jdk8u141.b16",
            "aarch64.shenandoah.jdk8u141.b16.shenandoah.merge.2017.07.27.02",
            "aarch64.shenandoah.jdk8u141.b16.shenandoah.merge.2017.07.26",
            "aarch64.shenandoah.jdk8u131.b12",
            "aarch64.shenandoah.jdk8u131.b12.shenandoah.merge.2017.04.20",
            "aarch64.shenandoah.jdk8u131.b11",
            "aarch64.shenandoah.jdk8u121.b14.shenandoah.merge.2017.03.23",
            "aarch64.shenandoah.jdk8u121.b14.shenandoah.merge.2017.03.09",
            "aarch64.shenandoah.jdk8u121.b14.shenandoah.merge.2017.03.08",
            "aarch64.shenandoah.jdk8u121.b14.shenandoah.merge.2017.03.06",
            "arch64.jdk8u131.b12",
            "jdk8u999.b99.zgu.20160920",
            "jdk8u162.b01",
            "jdk8u162.b00",
            "jdk8u152.b04",
            "jdk8u152.b03",
            "jdk8u152.b02",
            "jdk8u152.b01",
            "jdk8u144.b01",
            "jdk8u141.b15",
            "jdk8u122.b01",
            "jdk8u122.b00",
            "jdk8u121.b13",
            "jdk8u.zgu.20160920aarch64.jdk8u144.b02"
    };

    private final String[] ojdk9files = new String[] {
            "9.0.0.163",
            "9.0.0.154",
            "jdk.9.181",
            "jdk.9.180",
            "jdk.9.178",
            "jdk.9.176",
            "jdk.9.175",
            "jdk.9.174",
            "jdk.9.173",
            "jdk.9.172",
            "jdk.9.170",
            "jdk.9.167",
            "jdk.9.165",
            "jdk.9.164",
            "jdk.9.156"
    };

    private List<FileReturningHandler.FileInfo> getFileInfoList(String[] files) {
        List<FileReturningHandler.FileInfo> list = new ArrayList<>();
        long size = 1;
        for (String str : files) {
            list.add(new FileReturningHandler.FileInfo(str, null, null, size));
            size++;
        }
        return list;
    }

    @Test
    public void testComparatorByVersionOjdk7() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByVersion();
        List<FileReturningHandler.FileInfo> list = getFileInfoList(ojdk7files);
        List<FileReturningHandler.FileInfo> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        copy.sort(c);
        Assert.assertTrue(list.equals(copy));
    }

    @Test
    public void testComparatorByVersionOjdk8() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByVersion();
        List<FileReturningHandler.FileInfo> list = getFileInfoList(ojdk8files);
        List<FileReturningHandler.FileInfo> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        copy.sort(c);
        Assert.assertTrue(list.equals(copy));
    }

    @Test
    public void testComparatorByVersionOjdk9() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByVersion();
        List<FileReturningHandler.FileInfo> list = getFileInfoList(ojdk9files);
        List<FileReturningHandler.FileInfo> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        copy.sort(c);
        Assert.assertTrue(list.equals(copy));
    }

    @Test
    public void testComparatorByVersion() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByVersion();
        FileReturningHandler.FileInfo fi1 = new FileReturningHandler.FileInfo("name1", null, null, 1);
        FileReturningHandler.FileInfo fi2 = new FileReturningHandler.FileInfo("name2", null, null, 2);
        FileReturningHandler.FileInfo fi3 = new FileReturningHandler.FileInfo("name3", null, null, 3);
        FileReturningHandler.FileInfo[] fi = new FileReturningHandler.FileInfo[]{
                fi2, fi3, fi1};
        Arrays.sort(fi, c);
        Assert.assertArrayEquals(
                new FileReturningHandler.FileInfo[]{
                        fi3, fi2, fi1
                }, fi);
    }

    @Test
    public void testComparatorByLastModifiedDirContent() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByLastModifiedDirContent();
        FileReturningHandler.FileInfo fi1 = new FileReturningHandler.FileInfo("name1", null, null, 1);
        FileReturningHandler.FileInfo fi2 = new FileReturningHandler.FileInfo("name2", null, null, 2);
        FileReturningHandler.FileInfo fi3 = new FileReturningHandler.FileInfo("name3", null, null, 3);
        FileReturningHandler.FileInfo[] fi = new FileReturningHandler.FileInfo[]{
            fi2, fi3, fi1
        };
        Arrays.sort(fi, c);
        Assert.assertArrayEquals(
                new FileReturningHandler.FileInfo[]{
                    fi3, fi2, fi1
                }, fi);
    }

    @Test
    public void testComparatorByLastModified() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByLastModified();
        FileReturningHandler.FileInfo fi1 = new FileReturningHandler.FileInfo("name1", null, null, 1) {
            @Override
            public long getLastModified() {
                return getLastModifiedDirContent();
            }

        };
        FileReturningHandler.FileInfo fi2 = new FileReturningHandler.FileInfo("name2", null, null, 2) {
            @Override
            public long getLastModified() {
                return getLastModifiedDirContent();
            }

        };;
        FileReturningHandler.FileInfo fi3 = new FileReturningHandler.FileInfo("name3", null, null, 3) {
            @Override
            public long getLastModified() {
                return getLastModifiedDirContent();
            }

        };;
        FileReturningHandler.FileInfo[] fi = new FileReturningHandler.FileInfo[]{
            fi2, fi3, fi1
        };
        Arrays.sort(fi, c);
        Assert.assertArrayEquals(
                new FileReturningHandler.FileInfo[]{
                    fi3, fi2, fi1
                }, fi);
    }
}
