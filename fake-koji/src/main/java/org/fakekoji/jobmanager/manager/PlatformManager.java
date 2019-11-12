package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class PlatformManager implements Manager<Platform> {

    private final Storage<Platform> storage;
    private final JobUpdater jobUpdater;

    public PlatformManager(final Storage<Platform> storage, final JobUpdater jobUpdater) {
        this.storage = storage;
        this.jobUpdater = jobUpdater;
    }

    @Override
    public ManagementResult<Platform> create(Platform platform) throws StorageException, ManagementException {
        if (storage.contains(platform.getId())) {
            throw new ManagementException("Platform with id " + platform.getId() + " already exists");
        }
        storage.store(platform.getId(), platform);
        return new ManagementResult<>(platform, new JobUpdateResults());
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
    public ManagementResult<Platform> update(String id, Platform platform)
            throws StorageException, ManagementException {
        if (!storage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        storage.store(id, platform);
        final JobUpdateResults jobUpdateResults = jobUpdater.update(platform);
        return new ManagementResult<>(platform, jobUpdateResults);
    }

    @Override
    public ManagementResult<Platform> delete(String id) throws StorageException, ManagementException {
        throw new ManagementException("Not supported");
    }
}
