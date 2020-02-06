package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.ManagementUtils;
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
        // as frontend doesn't generate platform's ID, this will create platform with the correct ID based on its
        // os, version, architecture and provider
        final Platform newPlatform = new Platform(
                platform.getOs(),
                platform.getVersion(),
                platform.getArchitecture(),
                platform.getProviders(),
                platform.getVmName(),
                platform.getTags()
        );
        ManagementUtils.checkID(newPlatform.getId(), storage, false);
        storage.store(newPlatform.getId(), newPlatform);
        return new ManagementResult<>(platform, new JobUpdateResults());
    }

    @Override
    public Platform read(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        return storage.load(id, Platform.class);
    }

    @Override
    public List<Platform> readAll() throws StorageException {
        return storage.loadAll(Platform.class);
    }

    @Override
    public ManagementResult<Platform> update(String id, Platform platform) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        storage.store(id, platform);
        final JobUpdateResults jobUpdateResults = jobUpdater.update(platform);
        return new ManagementResult<>(platform, jobUpdateResults);
    }

    @Override
    public ManagementResult<Platform> delete(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        throw new ManagementException("Not supported");
    }

}
