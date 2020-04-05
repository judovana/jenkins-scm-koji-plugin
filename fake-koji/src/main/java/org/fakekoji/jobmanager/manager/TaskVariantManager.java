package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TaskVariantManager implements Manager<TaskVariant> {

    private final Storage<TaskVariant> storage;

    public TaskVariantManager(final Storage<TaskVariant> storage) {
        this.storage = storage;
    }

    @Override
    public TaskVariant create(TaskVariant taskVariant) throws StorageException, ManagementException {
        if (storage.contains(taskVariant.getId())) {
            throw new ManagementException("Task variant with id " + taskVariant.getId() + " already exists");
        }
        storage.store(taskVariant.getId(), taskVariant);
        return null;
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
        List<TaskVariant> l = storage.loadAll(TaskVariant.class);
        Collections.sort(l);
        return l;
    }

    @Override
    public TaskVariant update(String id, TaskVariant taskVariant) throws StorageException, ManagementException {
        return null;
    }

    @Override
    public TaskVariant delete(String id) throws StorageException, ManagementException {
        return null;
    }

    @Override
    public boolean contains(String id) {
        return storage.contains(id);
    }

    List<TaskVariant> getTaskVariants(Task.Type type) throws StorageException {
        return storage.loadAll(TaskVariant.class)
                .stream()
                .filter(variant -> variant.getType() == type)
                .sorted(TaskVariant::compareTo)
                .collect(Collectors.toList());
    }

    public List<TaskVariant> getBuildVariants() throws StorageException {
        return getTaskVariants(Task.Type.BUILD);
    }

    public List<TaskVariant> getTestVariants() throws StorageException {
        return getTaskVariants(Task.Type.TEST);
    }
}
