package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.Platform;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.Collections;
import java.util.List;

public class PlatformManager implements Manager<Platform> {

    private final Storage<Platform> storage;

    public PlatformManager(final Storage<Platform> storage) {
        this.storage = storage;
    }

    @Override
    public Platform create(Platform platform) throws StorageException, ManagementException {
        // as frontend doesn't generate platform's ID, this will create platform with the correct ID based on its
        // os, version, architecture and provider
        final Platform newPlatform = Platform.create(platform);
        ManagementUtils.checkID(newPlatform.getId(), storage, false);
        storage.store(newPlatform.getId(), newPlatform);
        return newPlatform;
    }

    @Override
    public Platform read(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        return storage.load(id, Platform.class);
    }

    @Override
    public List<Platform> readAll() throws StorageException {
        List<Platform> l = storage.loadAll(Platform.class);
        Collections.sort(l);
        return l;
    }

    @Override
    public Platform update(String id, Platform platform) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        storage.store(id, platform);
        return platform;
    }

    @Override
    public Platform delete(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        throw new ManagementException("Not supported");
    }

    @Override
    public boolean contains(String id) {
        return storage.contains(id);
    }
}
