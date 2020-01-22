package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class JDKVersionManager implements Manager<JDKVersion> {

    private final Storage<JDKVersion> storage;

    public JDKVersionManager(final Storage<JDKVersion> storage) {
        this.storage = storage;
    }

    @Override
    public ManagementResult create(JDKVersion jdkVersion) throws StorageException, ManagementException {
        if (storage.contains(jdkVersion.getId())) {
            throw new ManagementException("JDK version with id " + jdkVersion.getId() + " already exists");
        }
        storage.store(jdkVersion.getId(), jdkVersion);
        return null;
    }

    @Override
    public JDKVersion read(String id) throws StorageException, ManagementException {
        if (!storage.contains(id)) {
            throw new ManagementException("No JDK version with id: " + id);
        }
        return storage.load(id, JDKVersion.class);
    }

    @Override
    public List<JDKVersion> readAll() throws StorageException {
        return storage.loadAll(JDKVersion.class);
    }

    @Override
    public ManagementResult update(String id, JDKVersion product) throws StorageException, ManagementException {
        return null;
    }

    @Override
    public ManagementResult delete(String id) throws StorageException, ManagementException {
        return null;
    }
}
