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

    private static class IncredibleThread extends Thread {

        private boolean alive = true;
        private boolean paused = false;
        private boolean killed = false;
        private final List<Long>[] resultsHolder;
        private final RemoteRequestsCache cache;
        private boolean pauseApplied = false;
        private boolean haveRun = false;

        private IncredibleThread(List<Long>[] resultsHolder, RemoteRequestsCache cache) {
            this.resultsHolder = resultsHolder;
            this.cache = cache;
        }


        @Override
        public void run() {
            while (alive) {
                if (!paused) {
                    pauseApplied = false;
                    long r = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
                    resultsHolder[0].add(r);
                    haveRun = true;
                } else {
                    pauseApplied = true;
                    haveRun = false;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            killed = true;
        }

        public void kill() {
            alive = false;
        }

        public boolean isKilled() {
            return killed;
        }

        public void pause() {
            this.paused = true;
        }

        public void unpause() {
            this.paused = false;
        }

        public boolean isPauseApplied() {
            return pauseApplied;
        }

        public boolean hadRun() {
            return haveRun;
        }
    }

    ;

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
        final List<Long>[] l = new List[]{Collections.synchronizedList(new ArrayList<>())};
        IncredibleThread[] threads1 = new IncredibleThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads1[i] = new IncredibleThread(l, cache);
        }
        startAll(threads1);
        Thread.sleep(50);
        pauseAll(threads1);
        List<Long> l1 = l[0];
        l[0] = Collections.synchronizedList(new ArrayList<>());
        necessarryTimeout[0] = 1;
        unpauseAll(threads1);
        Thread.sleep(50);
        pauseAll(threads1);
        necessarryTimeout[0] = Long.MAX_VALUE;
        unpauseAll(threads1);
        List<Long> l2 = l[0];
        l[0] = Collections.synchronizedList(new ArrayList<>());
        Thread.sleep(50);
        pauseAll(threads1);
        for (int i = 0; i < threadCount; i++) {
            threads1[i].kill();
        }
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

    private void startAll(IncredibleThread[] threads) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        waitForAllRun(threads);
    }

    private void pauseAll(IncredibleThread[] threads) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].pause();
        }
        waitForAllPaused(threads);
    }

    private void unpauseAll(IncredibleThread[] threads) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].unpause();
        }
        waitForAllRun(threads);
    }

    private void waitForAllRun(IncredibleThread[] threads) throws InterruptedException {
        while (!areAllStarted(threads)) {
            Thread.sleep(10);
        }
    }

    private void waitForAllPaused(IncredibleThread[] threads) throws InterruptedException {
        while (!areAllPaused(threads)) {
            Thread.sleep(10);
        }
    }


    private boolean areAllStarted(IncredibleThread[] threads) {
        for (int i = 0; i < threads.length; i++) {
            if (!threads[i].hadRun()) {
                return false;
            }
        }
        return true;
    }

    private boolean areAllPaused(IncredibleThread[] threads) {
        for (int i = 0; i < threads.length; i++) {
            if (!threads[i].isPauseApplied()) {
                return false;
            }
        }
        return true;
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
        p.setProperty("m1@anotherurl", "2000000000");
        p.setProperty("m2", "200");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "200");
        cache.setProperties(p);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        long r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(1, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(4, r6);
        p = new Properties();
        p.setProperty("m1", "10");
        p.setProperty("m1@anotherurl", "2000000000");
        p.setProperty("m2", "200");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "200");
        cache.setProperties(p);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);// tis intermediate sleeps are here to avodi fasle negativum when invalidation is in play
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(5, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(4, r6);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(6, r3);
        Assert.assertEquals(2, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(4, r6);
        p = new Properties();
        p.setProperty("m1", "200");
        p.setProperty("m1@anotherurl", "2000000000");
        p.setProperty("m2", "10");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "200");
        cache.setProperties(p);
        Thread.sleep(30);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(7, r2);
        Assert.assertEquals(6, r3);
        Assert.assertEquals(7, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(4, r6);
        Thread.sleep(35);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Thread.sleep(5);
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(8, r2);
        Assert.assertEquals(6, r3);
        Assert.assertEquals(8, r4);
        Assert.assertEquals(3, r5);
        Assert.assertEquals(4, r6);
        Thread.sleep(200);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(9, r1);
        Assert.assertEquals(10, r2);
        Assert.assertEquals(9, r3);
        Assert.assertEquals(10, r4);
        Assert.assertEquals(11, r5);
        Assert.assertEquals(4, r6);
        p = new Properties();
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "200");
        cache.setProperties(p);
        Thread.sleep(100);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r5 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        r6 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(9, r1);
        Assert.assertEquals(10, r2);
        Assert.assertEquals(9, r3);
        Assert.assertEquals(10, r4);
        Assert.assertEquals(11, r5);
        Assert.assertEquals(12, r6);

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
        p.setProperty(RemoteRequestCacheConfigKeys.BLACK_LISTED_URLS_LIST, "someGarbage .*url:2.*");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(3, r2);
        p = new Properties();
        p.setProperty(RemoteRequestCacheConfigKeys.BLACK_LISTED_URLS_LIST, "someGarbage .*url:2.* .*url:1.*");
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
        p.setProperty(RemoteRequestCacheConfigKeys.CONFIG_REFRESH_RATE_MINUTES, "10");
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
        p.setProperty(RemoteRequestCacheConfigKeys.BLACK_LISTED_URLS_LIST, ".*url:1.*");
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
        p.setProperty(RemoteRequestCacheConfigKeys.BLACK_LISTED_URLS_LIST, "someGarbage .*url:2.* .*url:1.*");
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


    @Test
    public void cleanWorks() throws InterruptedException, IOException {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1", "p2"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(3, r2);
        Properties p = new Properties();
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_CLEAN_COMMAND, "true");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(4, r1);
        Assert.assertEquals(5, r2);
    }

    private static class SlowOriginalObjectProvider implements OriginalObjectProvider {
        AtomicLong i = new AtomicLong(0);

        @Override
        public Object obtainOriginal(String url, XmlRpcRequestParams params) {
            try {
                Thread.sleep(1000);
                return i.incrementAndGet();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class LongReturningThread extends Thread {
        long r;
        private final XmlRpcRequestParams param;
        private final AccessibleRemoteRequestsCache cache;

        private LongReturningThread(XmlRpcRequestParams param, AccessibleRemoteRequestsCache cache) {
            this.param = param;
            this.cache = cache;
        }

        @Override
        public void run() {
            r = (long) cache.obtain("http://url:1/path", param);
        }

        public long getR() {
            return r;
        }
    }

    @Test
    public void lazyRefreshWorks() throws InterruptedException, IOException {
        SlowOriginalObjectProvider provider = new SlowOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider) {
            @Override
            protected long toUnits(long time) {
                return time;
            }
        };
        Properties p = new Properties();
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "1000");
        cache.setProperties(p);
        LongReturningThread l1 = new LongReturningThread(new DummyRequestparam("m1", new Object[]{"p1"}), cache);
        LongReturningThread l2 = new LongReturningThread(new DummyRequestparam("m1", new Object[]{"p1"}), cache);
        l1.start();
        l2.start();
        l1.join();
        l2.join();
        //both attempted to get null fromdb, hard to say which was first
        Assert.assertTrue((1 == l1.getR()) ^ (1 == l2.getR()));
        Assert.assertTrue((2 == l1.getR()) ^ (2 == l2.getR()));
        Thread.sleep(1000);//timeout the value
        l1 = new LongReturningThread(new DummyRequestparam("m1", new Object[]{"p1"}), cache);
        l2 = new LongReturningThread(new DummyRequestparam("m1", new Object[]{"p1"}), cache);
        l1.start();
        Thread.sleep(500);//some time for first thread to get
        p = new Properties();
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000000");//do not invalidate it again
        cache.setProperties(p);
        l2.start();
        l1.join();
        l2.join();
        //both attempting cached value, first have to wait for new one, invalidating result, but second get old vlaue again
        Assert.assertEquals(3, l1.getR()); //new value
        Assert.assertTrue(2 == l2.getR() || 1 == l2.getR()); //no guarantee which result was added to db; cached value obtained anyway
    }


    @Test
    public void disablingPerMethodWorks() throws InterruptedException {
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider) {
            @Override
            protected long toUnits(long time) {
                return time;
            }
        };
        Properties p = new Properties();
        p.setProperty("m1", "20000");
        p.setProperty("m1@anotherurl", "20000");
        p.setProperty("m2", "20000");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000");
        cache.setProperties(p);
        long r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        long r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        long r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(3, r3);
        Assert.assertEquals(4, r4);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(1, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(3, r3);
        Assert.assertEquals(4, r4);
        p = new Properties();
        p.setProperty("m1", "0");
        p.setProperty("m1@anotherurl", "20000");
        p.setProperty("m2", "20000");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(5, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(3, r3);
        Assert.assertEquals(4, r4);
        p = new Properties();
        p.setProperty("m1", "0");
        p.setProperty("m2", "20000");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(7, r3);
        Assert.assertEquals(4, r4);
        p = new Properties();
        p.setProperty("m1", "20000");
        p.setProperty("m1@anotherurl", "0");
        p.setProperty("m2", "20000");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(2, r2);
        Assert.assertEquals(8, r3);
        Assert.assertEquals(4, r4);
        p = new Properties();
        p.setProperty("m1", "20000");
        p.setProperty("m1@anotherurl", "20000");
        p.setProperty("m2", "0");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "20000");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(6, r1);
        Assert.assertEquals(9, r2);
        Assert.assertEquals(8, r3);
        Assert.assertEquals(4, r4);
        p = new Properties();
        p.setProperty("m1", "20000");
        p.setProperty("m1@anotherurl", "20000");
        p.setProperty("m2", "20000");
        p.setProperty(RemoteRequestCacheConfigKeys.CACHE_REFRESH_RATE_MINUTES, "0");
        cache.setProperties(p);
        r1 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r2 = (long) cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        r3 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        r4 = (long) cache.obtain("http://anotherurl:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(10, r1);
        Assert.assertEquals(11, r2);
        Assert.assertEquals(12, r3);
        Assert.assertEquals(13, r4);
    }
}
