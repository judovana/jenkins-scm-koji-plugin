package org.fakekoji.xmlrpc.server.core;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.Assert;
import org.junit.Test;


public class FileReturningHandlerTest {

    @Test
    public void testComparatorByVersion() throws Exception {
        Comparator<FileReturningHandler.FileInfo> c = new FileReturningHandler.ComparatorByVersion();
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
