package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.junit.Assert;
import org.junit.Test;

public class RemoteRequestsCacheTest {

    private static class AccessibleRemoteRequestsCache extends RemoteRequestsCache {

        public AccessibleRemoteRequestsCache(File config, OriginalObjectProvider originalObjectProvider) {
            super(config, originalObjectProvider);
        }

        @Override
        public void setProperties(Properties prop) {
            super.setProperties(prop);
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void markNotLoaded() {
            loaded = false;
        }
    }

    private static class DummyOriginalObjectProvider implements OriginalObjectProvider {
        AtomicLong i = new AtomicLong(0);

        @Override
        public Object obtainOriginal(String url, XmlRpcRequestParams params) {
            return i.incrementAndGet();
        }

        public long getState() {
            return i.get();
        }
    }

    private static class DummyRequestparam implements XmlRpcRequestParams {
        private final String method;
        private final Object[] params;

        public DummyRequestparam(String method, Object[] params) {
            this.method = method;
            this.params = params;
        }

        @Override
        public Object[] toXmlRpcParams() {
            return params;
        }

        @Override
        public String getMethodName() {
            return method;
        }

        @Override
        public int hashCode() {
            return Objects.hash(params);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DummyRequestparam that = (DummyRequestparam) o;
            return Objects.equals(method, that.method) &&
                    Arrays.equals(params, that.params);
        }
    }

    @Test
    public void cacheWorks() {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) provider.obtainOriginal(null, null);
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) provider.obtainOriginal(null, null);
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(1, r3);
        Assert.assertEquals(3, r4);
    }

    @Test
    public void cacheCanBeDisabled() {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider) {
            @Override
            protected long getDefaultValidnesMilis() {
                return 0;
            }
        };
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) provider.obtainOriginal(null, null);
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) provider.obtainOriginal(null, null);
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(3, r3);
        Assert.assertEquals(4, r4);
    }

    @Test
    public void cacheWorksPerMethodAndParam() {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p2"}));
        long r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(1, r3);
        Assert.assertEquals(3, r4);
        Assert.assertEquals(2, r5);
    }

    @Test
    public void cacheWorksPerUrl() {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(1, r3);
        Assert.assertEquals(2, r4);
    }

    @Test
    public void cachCanBeDisabeldPerMethod() {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider);
        Properties p = new Properties();
        p.setProperty("m1", "1");
        p.setProperty("m2", "0");
        cache.setProperties(p);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r6 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(3, r3);
        Assert.assertEquals(1, r4);
        Assert.assertEquals(4, r5);
        Assert.assertEquals(3, r6);
    }

    @Test
    public void concurentReadWorks() throws InterruptedException {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider);
        //we put firts item seqentially, so we know what is cache
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        int threadCount = 100;
        final boolean[] alive = new boolean[]{true};
        final List<Long> l = Collections.synchronizedList(new ArrayList<>());
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (alive[0]) {
                        long r = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
                        l.add(r);
                    }
                }
            });
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        Thread.sleep(50);
        alive[0] = false;
        Thread.sleep(50);
        for (int i = 0; i < l.size(); i++) {
            Assert.assertEquals(1, l.get(i).intValue());
        }
        Assert.assertTrue(l.size() > 5);
    }

    @Test
    public void timoutWorks() throws InterruptedException {
        final long[] necessarryTimeout = new long[]{Long.MAX_VALUE};
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider) {
            @Override
            protected long getDefaultValidnesMilis() {
                return necessarryTimeout[0];
            }
        };
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Thread.sleep(50);
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        necessarryTimeout[0] = 1;
        Thread.sleep(50);
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Thread.sleep(50);
        long r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        necessarryTimeout[0] = Long.MAX_VALUE;
        Thread.sleep(50);
        long r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Thread.sleep(50);
        long r6 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(1, r2);
        Assert.assertEquals(2, r3);
        Assert.assertEquals(3, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(3, r6);
    }

    @Test
    public void concurentReadWriteWorks() throws InterruptedException {
        final long[] necessarryTimeout = new long[]{Long.MAX_VALUE};
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider) {
            @Override
            protected long getDefaultValidnesMilis() {
                return necessarryTimeout[0];
            }
        };
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        int threadCount = 100;
        final boolean[] alive = new boolean[]{true};
        final List<Long>[] l = new List[]{Collections.synchronizedList(new ArrayList<>())};
        Thread[] threads1 = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads1[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (alive[0]) {
                        long r = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
                        l[0].add(r);
                    }
                }
            });
        }
        for (int i = 0; i < threadCount; i++) {
            threads1[i].start();
        }
        Thread.sleep(50);
        List<Long> l1 = l[0];
        l[0] = Collections.synchronizedList(new ArrayList<>());
        necessarryTimeout[0] = 1;
        Thread.sleep(50);
        necessarryTimeout[0] = Long.MAX_VALUE;
        Thread.sleep(50);
        List<Long> l2 = l[0];
        l[0] = Collections.synchronizedList(new ArrayList<>());
        Thread.sleep(50);
        alive[0] = false;
        Thread.sleep(50);
        List<Long> l3 = l[0];
        for (int i = 0; i < l1.size(); i++) {
            Assert.assertEquals(1, l1.get(i).intValue());
        }
        Assert.assertTrue(l1.size() > 5);
        //in l2 is mayhem, we do not when the trigger was pulled.
        long max = Long.MIN_VALUE;
        for (int i = 0; i < l2.size(); i++) {
            if (l2.get(i).longValue() > max) {
                max = l2.get(i);
            }
        }
        Assert.assertTrue(max > 1);
        Assert.assertTrue(l2.size() > 5);
        for (int i = 0; i < l3.size(); i++) {
            Assert.assertEquals(max, l3.get(i).intValue());
        }
        Assert.assertTrue(l3.size() > 5);
    }

    @Test
    public void methodTimeoutWorks() throws InterruptedException {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider) {
            @Override
            protected long toUnits(long time) {
                return time;
            }
        };
        Properties p = new Properties();
        p.setProperty("m1", "200");
        p.setProperty("m2", "200");
        p.setProperty("cacheRefreshRateMinutes", "200");
        cache.setProperties(p);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(1, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        p = new Properties();
        p.setProperty("m1", "10");
        p.setProperty("m2", "200");
        p.setProperty("cacheRefreshRateMinutes", "200");
        cache.setProperties(p);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);// tis intermediate sleeps are here to avodi fasle negativum when invalidation is in play
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(4, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(4, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(5, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        p = new Properties();
        p.setProperty("m1", "200");
        p.setProperty("m2", "10");
        p.setProperty("cacheRefreshRateMinutes", "200");
        cache.setProperties(p);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(6, r2);
        Assert.assertEquals(5, r3);
        Assert.assertEquals(6, r4);
        Assert.assertEquals(3, r5);
        Thread.sleep(35);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(7, r2);
        Assert.assertEquals(5, r3);
        Assert.assertEquals(7, r4);
        Assert.assertEquals(3, r5);
        Thread.sleep(200);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(8, r1);
        Assert.assertEquals(9, r2);
        Assert.assertEquals(8, r3);
        Assert.assertEquals(9, r4);
        Assert.assertEquals(10, r5);
        p = new Properties();
        p.setProperty("cacheRefreshRateMinutes", "200");
        cache.setProperties(p);
        Thread.sleep(100);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(8, r1);
        Assert.assertEquals(9, r2);
        Assert.assertEquals(8, r3);
        Assert.assertEquals(9, r4);
        Assert.assertEquals(10, r5);

    }

    @Test
    public void excludeListWorks() throws InterruptedException {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Properties p = new Properties();
        p.setProperty("blackListedUrlsList", "someGarbage .*url:2.*");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(3, r2);
        p = new Properties();
        p.setProperty("blackListedUrlsList", "someGarbage .*url:2.* .*url:1.*");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(4, r1);
        Assert.assertEquals(5, r2);
        p = new Properties();
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(4, r1);
        Assert.assertEquals(5, r2);
    }


    @Test
    public void fileRefreshWorks() throws InterruptedException, IOException {
        File f = File.createTempFile("cache", ".config");
        Properties p = new Properties();
        p.setProperty("configRefreshRateMinutes", "10");
        FileWriter fw = new FileWriter(f);
        p.store(fw, null);
        fw.flush();
        fw.close();
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(f, provider) {
            @Override
            protected long toUnits(long time) {
                return time;
            }
        };
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Thread.sleep(20);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        p = new Properties();
        p.setProperty("blackListedUrlsList", ".*url:1.*");
        fw = new FileWriter(f);
        p.store(fw, null);
        fw.flush();
        fw.close();
        cache.markNotLoaded();
        while (!cache.isLoaded()) {
            Thread.sleep(5);
        }
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(3, r1);
        Assert.assertEquals(2, r2);
        Thread.sleep(20);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(4, r1);
        Assert.assertEquals(2, r2);
        p = new Properties();
        p.setProperty("blackListedUrlsList", "someGarbage .*url:2.* .*url:1.*");
        fw = new FileWriter(f);
        p.store(fw, null);
        fw.flush();
        fw.close();
        cache.markNotLoaded();
        while (!cache.isLoaded()) {
            Thread.sleep(5);
        }
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(6, r2);
        Thread.sleep(RemoteRequestsCache.CACHE_DEFAULT); //let it expire
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(7, r1);
        Assert.assertEquals(8, r2);
        p = new Properties();
        fw = new FileWriter(f);
        p.store(fw, null);
        fw.flush();
        fw.close();
        cache.markNotLoaded();
        while (!cache.isLoaded()) {
            Thread.sleep(5);
        }
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(7, r1);
        Assert.assertEquals(8, r2);
        Thread.sleep(20);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(7, r1);
        Assert.assertEquals(8, r2);

    }
}
