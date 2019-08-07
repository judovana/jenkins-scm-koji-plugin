package org.fakekoji.jobmanager;

import org.fakekoji.storage.StorageException;

public interface Parser<T, U> {

    U parse(T t) throws ManagementException, StorageException;
}
