package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class PlatformManager implements Manager<Platform> {

    private final Storage<Platform> storage;

    public PlatformManager(final Storage<Platform> storage) {
        this.storage = storage;
    }

    @Override
    public ManagementResult create(Platform platform) throws StorageException, ManagementException {
        if (storage.contains(platform.getId())) {
            throw new ManagementException("Platform with id " + platform.getId() + " already exists");
        }
        storage.store(platform.getId(), platform);
        return null;
    }

    @Override
    public Platform read(String id) throws StorageException, ManagementException {
        if (!storage.contains(id)) {
            throw new ManagementException("No platform with id: " + id);
        }
        return storage.load(id, Platform.class);
    }

    @Override
    public List<Platform> readAll() throws StorageException {
        return storage.loadAll(Platform.class);
    }

    @Override
    public ManagementResult update(String id, Platform platform) throws StorageException, ManagementException {
        return null;
    }

    @Override
    public ManagementResult delete(String id) throws StorageException, ManagementException {
        return null;
    }
}
