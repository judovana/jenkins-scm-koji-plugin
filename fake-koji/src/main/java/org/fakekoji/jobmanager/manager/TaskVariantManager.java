package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class TaskVariantManager implements Manager<TaskVariant> {

    private final Storage<TaskVariant> storage;

    public TaskVariantManager(final Storage<TaskVariant> storage) {
        this.storage = storage;
    }

    @Override
    public void create(TaskVariant taskVariant) throws StorageException, ManagementException {
        if (storage.contains(taskVariant.getId())) {
            throw new ManagementException("Task variant with id " + taskVariant.getId() + " already exists");
        }
        storage.store(taskVariant.getId(), taskVariant);
    }

    @Override
    public TaskVariant read(String id) throws StorageException, ManagementException {
        if (!storage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        return storage.load(id, TaskVariant.class);
    }

    @Override
    public List<TaskVariant> readAll() throws StorageException {
        return storage.loadAll(TaskVariant.class);
    }

    @Override
    public void update(String id, TaskVariant taskVariant) throws StorageException, ManagementException {

    }

    @Override
    public void delete(String id) throws StorageException, ManagementException {

    }
}
