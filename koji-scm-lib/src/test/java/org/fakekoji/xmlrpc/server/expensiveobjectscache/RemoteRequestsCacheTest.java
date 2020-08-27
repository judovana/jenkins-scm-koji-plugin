package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import java.io.File;
import java.util.Arrays;
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
        int i = 0;

        @Override
        public Object obtainOriginal(String url, XmlRpcRequestParams params) {
            i++;
            return i;
        }

        public int getState() {
            return i;
        }
    };

    private static class DummyRequestparam implements  XmlRpcRequestParams {
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
    public void cacheWorks(){
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        int r1 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r2 = (int)provider.obtainOriginal(null,null);
        int r3 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r4 = (int)provider.obtainOriginal(null,null);
        Assert.assertEquals(1,r1);
        Assert.assertEquals(2,r2);
        Assert.assertEquals(1,r3);
        Assert.assertEquals(3,r4);
    }

    @Test
    public void cacheCanBeDisabled(){
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider){
            @Override
            protected long getDefaultValidnesMilis() {
                return 0;
            }
        };
        int r1 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r2 = (int)provider.obtainOriginal(null,null);
        int r3 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r4 = (int)provider.obtainOriginal(null,null);
        Assert.assertEquals(1,r1);
        Assert.assertEquals(2,r2);
        Assert.assertEquals(3,r3);
        Assert.assertEquals(4,r4);
    }

    @Test
    public void cacheWorksPerMethodAndParam(){
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        int r1 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r2 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        int r3 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r4 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p2"}));
        int r5 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        Assert.assertEquals(1,r1);
        Assert.assertEquals(2,r2);
        Assert.assertEquals(1,r3);
        Assert.assertEquals(3,r4);
        Assert.assertEquals(2,r5);
    }

    @Test
    public void cacheWorksPerUrl(){
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        RemoteRequestsCache cache = new RemoteRequestsCache(null, provider);
        int r1 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r2 = (int)cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r3 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r4 = (int)cache.obtain("http://url:2/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        Assert.assertEquals(1,r1);
        Assert.assertEquals(2,r2);
        Assert.assertEquals(1,r3);
        Assert.assertEquals(2,r4);
    }

    @Test
    public void cachCanBeDisabeldPerMethod(){
        DummyOriginalObjectProvider provider = new DummyOriginalObjectProvider();
        AccessibleRemoteRequestsCache cache = new AccessibleRemoteRequestsCache(null, provider);
        Properties p = new Properties();
        p.setProperty("m1", "1");
        p.setProperty("m2", "0");
        cache.setProperties(p);
        int r1 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r2 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        int r3 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        int r4 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m1", new Object[]{"p1"}));
        int r5 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m2", new Object[]{"p1"}));
        int r6 = (int)cache.obtain("http://url:1/path", new DummyRequestparam("m3", new Object[]{"p1"}));
        Assert.assertEquals(1,r1);
        Assert.assertEquals(2,r2);
        Assert.assertEquals(3,r3);
        Assert.assertEquals(1,r4);
        Assert.assertEquals(4,r5);
        Assert.assertEquals(3,r6);
    }




}
