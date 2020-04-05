package org.fakekoji.jobmanager;

import org.fakekoji.storage.StorageException;

import java.util.List;

public interface Manager<T> {

    T create(T t) throws StorageException, ManagementException;

    T read(String id) throws StorageException, ManagementException;

    List<T> readAll() throws StorageException;

    T update(String id, T t) throws StorageException, ManagementException;

    T delete(String id) throws StorageException, ManagementException;

    boolean contains(String id);
}
