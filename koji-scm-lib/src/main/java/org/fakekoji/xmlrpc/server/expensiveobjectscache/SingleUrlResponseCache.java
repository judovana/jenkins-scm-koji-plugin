package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;

import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        cache.put(params, new ResultWithTimeStamp(new Date(), result));
    }


    public static final class ResultWithTimeStamp {
        private final Date dateCreated;
        private final Object result;
        private boolean notBeingRepalced = true;

        public ResultWithTimeStamp(final Date datecreated, final Object result) {
            this.dateCreated = datecreated;
            this.result = result;
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        public Object getResult() {
            return result;
        }

        public boolean isNotBeingRepalced() {
            return notBeingRepalced;
        }

        public void flagBeingRepalced() {
            this.notBeingRepalced = false;
        }
    }
}
