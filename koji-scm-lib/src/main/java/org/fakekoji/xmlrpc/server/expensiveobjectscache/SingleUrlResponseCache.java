package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;

import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.*;

public class SingleUrlResponseCache {


    private final URL id;
    private final Map<XmlRpcRequestParams, ResultWithTimeStamp> cache = Collections.synchronizedMap(new HashMap<>());


    public SingleUrlResponseCache(final URL u) {
        this.id = u;
    }

    public ResultWithTimeStamp get(final XmlRpcRequestParams params) {
        return cache.get(params);
    }

    public void put(final Object result, XmlRpcRequestParams params) {
        cache.put(params, new ResultWithTimeStamp(result));
    }

    public void remove(XmlRpcRequestParams key) {
        cache.remove(key);
    }

    public URL getId() {
        return id;
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "pure wrapper class")
    public static final class ResultWithTimeStamp {

        private final Date dateCreated;
        private final Object result;
        private boolean notBeingRepalced = true;

        public ResultWithTimeStamp(final Object result) {
            this.dateCreated = new Date();
            this.result = result;
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        public Object getResult() {
            return result;
        }

        public boolean isNotBeingReplaced() {
            return notBeingRepalced;
        }

        public void flagBeingReplaced() {
            this.notBeingRepalced = false;
        }
    }

    Set<Map.Entry<XmlRpcRequestParams, ResultWithTimeStamp>> getContent() {
        return cache.entrySet();
    }
}
