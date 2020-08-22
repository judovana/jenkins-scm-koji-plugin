package org.fakekoji.xmlrpc.server.expensiveobjectscache;

/**
 * This object can be cached in maps. Thus hashcode and equals are superimportant
 */
public interface CachableRequest {

    boolean equals(Object o);

    int hashCode();
}
