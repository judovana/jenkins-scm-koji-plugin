package org.fakekoji.storage;

import java.util.List;

public interface Storage <T> {

    void store(String id, T t) throws StorageException;

    void delete(String id) throws StorageException;

    T load(String id, Class<T> valueType) throws StorageException;

    List<T> loadAll(Class<T> valueType) throws StorageException;

    boolean contains(String id);
}
