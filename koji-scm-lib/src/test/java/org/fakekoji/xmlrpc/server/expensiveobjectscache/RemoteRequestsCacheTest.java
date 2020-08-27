package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

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
    }

    private static class DummyOriginalObjectProvider implements OriginalObjectProvider {
        long i = 0;

        @Override
        public Object obtainOriginal(String url, XmlRpcRequestParams params) {
            i++;
            return i;
        }

        public long getState() {
            return i;
        }
    }

    ;

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
        int threadCount = 20;
        final boolean[] alive = new boolean[]{true};
        final List<Long> l = Collections.synchronizedList(new LinkedList<>());
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
        int threadCount = 20;
        final boolean[] alive = new boolean[]{true};
        final List<Long>[] l = new List[]{Collections.synchronizedList(new LinkedList<>())};
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
        l[0] = Collections.synchronizedList(new LinkedList<>());
        necessarryTimeout[0] = 1;
        Thread.sleep(50);
        necessarryTimeout[0] = Long.MAX_VALUE;
        Thread.sleep(50);
        List<Long> l2 = l[0];
        l[0] = Collections.synchronizedList(new LinkedList<>());
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

}
