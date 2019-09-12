package org.fakekoji.jobmanager;

import org.fakekoji.storage.StorageException;

import java.util.List;

public interface Manager<T> {

    void create(T t) throws StorageException, ManagementException;

    T read(String id) throws StorageException, ManagementException;

    List<T> readAll() throws StorageException;

    void update(String id, T t) throws StorageException, ManagementException;

    void delete(String id) throws StorageException, ManagementException;


}
