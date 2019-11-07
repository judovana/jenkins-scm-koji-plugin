package org.fakekoji.jobmanager;

import org.fakekoji.storage.StorageException;

import java.util.List;

public interface Manager<T> {

    ManagementResult create(T t) throws StorageException, ManagementException;

    T read(String id) throws StorageException, ManagementException;

    List<T> readAll() throws StorageException;

    ManagementResult update(String id, T t) throws StorageException, ManagementException;

    ManagementResult delete(String id) throws StorageException, ManagementException;


}
