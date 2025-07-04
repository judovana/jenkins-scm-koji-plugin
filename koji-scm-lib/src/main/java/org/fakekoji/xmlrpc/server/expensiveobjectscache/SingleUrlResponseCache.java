package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    private static String asMinutes(long l) {
        return " (" + (l / 1000 / 600) + "min)";
    }


    private static final Map<Class<?>, Class<?>> WRAPPER_TYPE_MAP;

    static {
        WRAPPER_TYPE_MAP = new HashMap<Class<?>, Class<?>>(20);
        WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        WRAPPER_TYPE_MAP.put(Character.class, char.class);
        WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_TYPE_MAP.put(Double.class, double.class);
        WRAPPER_TYPE_MAP.put(Float.class, float.class);
        WRAPPER_TYPE_MAP.put(Long.class, long.class);
        WRAPPER_TYPE_MAP.put(Short.class, short.class);
        WRAPPER_TYPE_MAP.put(Void.class, void.class);
        WRAPPER_TYPE_MAP.put(String.class, String.class);
    }

    public synchronized void dump(String preffix, BufferedWriter bw, RemoteRequestsCache validator) throws IOException {
        List<Map.Entry<XmlRpcRequestParams, ResultWithTimeStamp>> entries = new ArrayList(cache.entrySet());
        entries.sort((o1, o2) -> o1.getKey().getMethodName().compareTo(o2.getKey().getMethodName()));
        for (Map.Entry<XmlRpcRequestParams, ResultWithTimeStamp> entry : entries) {
            bw.write(preffix + XmlRpcRequestParams.toNiceString(entry.getKey()) + ": ");
            bw.newLine();
            bw.write(preffix + "  dateCreated: " + entry.getValue().dateCreated);
            bw.newLine();
            bw.write(preffix + "  notBeingRepalced: " + entry.getValue().notBeingRepalced);
            bw.newLine();
            bw.write(preffix + "  validity: " + validator.isValid(entry.getValue(), entry.getKey().getMethodName(), id.getHost()));
            bw.newLine();
            long ttl = validator.getPerMethodValidnesMilis(entry.getKey().getMethodName(), id.getHost());
            bw.write(preffix + "  original ttl: " + ttl + "ms" + asMinutes(ttl));
            bw.newLine();
            long cttl = new Date().getTime() - entry.getValue().dateCreated.getTime();
            bw.write(preffix + "  time alive " + cttl + "ms" + asMinutes(cttl));
            bw.newLine();
            bw.write(preffix + "  => ttl: " + (ttl - cttl) + "ms" + asMinutes(ttl - cttl));
            bw.newLine();
            if (WRAPPER_TYPE_MAP.containsKey(entry.getValue().result.getClass())) {
                bw.write(preffix + "  result: " + entry.getValue().result + " (" + entry.getValue().result.getClass().getName() + ")");
                bw.newLine();
            } else {
                bw.write(preffix + "  result: ");
                bw.newLine();
                entry.getValue().dump(preffix + "  ", bw);
            }
        }
        bw.write(preffix + "total: " + entries.size());
        bw.newLine();
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

        public void dump(String preffix, BufferedWriter bw) throws IOException {
            dump(preffix, result, bw);
        }

        private static final String FINAL_INCREMENT = "  ";

        public static void dump(String preffix, Object o, BufferedWriter bw) throws IOException {
            if (o == null) {
                bw.write(preffix + "null");
                bw.newLine();
                return;
            }
            if (o instanceof Map) {
                bw.write(preffix + " map " + o.getClass().getName() + " map (size: " + ((Map) o).size());
                bw.newLine();
                Set<Map.Entry> entries = ((Map) o).entrySet();
                for (Map.Entry e : entries) {
                    if (e.getKey() == null) {
                        bw.write(preffix + FINAL_INCREMENT + "null=");
                        bw.newLine();
                        dump(preffix + FINAL_INCREMENT + FINAL_INCREMENT, e.getValue(), bw);
                    } else {
                        bw.write(preffix + FINAL_INCREMENT + e.getKey() + "=");
                        bw.newLine();
                        dump(preffix + FINAL_INCREMENT + FINAL_INCREMENT, e.getValue(), bw);
                    }
                }
            } else if (o.getClass().isArray()) {
                bw.write(preffix + " ary " + o.getClass().getName() + " ary (size: " + Array.getLength(o));
                bw.newLine();

                if (o instanceof Object[]) {
                    for (Object e : (Object[]) o) {
                        dump(preffix + FINAL_INCREMENT, e, bw);
                    }
                } else if (o instanceof int[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (int e : (int[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof byte[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (byte e : (byte[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof char[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (char e : (char[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof boolean[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (boolean e : (boolean[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof double[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (double e : (double[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof float[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (float e : (float[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof long[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (long e : (long[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                } else if (o instanceof short[]) {
                    bw.write(preffix + FINAL_INCREMENT);
                    for (short e : (short[]) o) {
                        bw.write("" + e + ",");
                    }
                    bw.newLine();
                }
            } else if (o instanceof Collection) {
                bw.write(preffix + " col " + o.getClass().getName() + " col (size: " + ((Collection) o).size());
                bw.newLine();
                for (Object e : (Collection) o) {
                    dump(preffix + FINAL_INCREMENT, e, bw);
                }
            } else if (o instanceof Iterable) {
                bw.write(preffix + " ite " + o.getClass().getName() + " ite");
                bw.newLine();
                for (Object e : (Iterable) o) {
                    dump(preffix + FINAL_INCREMENT, e, bw);
                }
            } else {
                bw.write(preffix + o + " (" + o.getClass().getName() + ")");
                bw.newLine();
            }
        }
    }

    Set<Map.Entry<XmlRpcRequestParams, ResultWithTimeStamp>> getContent() {
        return cache.entrySet();
    }
}
