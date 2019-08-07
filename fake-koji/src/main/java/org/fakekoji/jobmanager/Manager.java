package org.fakekoji.jobmanager;

import org.fakekoji.storage.StorageException;

public interface Manager {

    String create(String json) throws StorageException, ManagementException;

    String read(String id) throws StorageException, ManagementException;

    String readAll() throws StorageException;

    String update(String id, String json) throws StorageException, ManagementException;

    String delete(String id) throws StorageException, ManagementException;


}
