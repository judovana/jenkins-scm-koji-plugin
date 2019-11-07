package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.Task;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class TaskManager implements Manager<Task> {

    private final Storage<Task> taskStorage;

    public TaskManager(final Storage<Task> taskStorage) {
        this.taskStorage = taskStorage;
    }

    @Override
    public ManagementResult create(Task task) throws StorageException, ManagementException {
        if (taskStorage.contains(task.getId())) {
            throw new ManagementException("Task with id " + task.getId() + " already exists");
        }
        taskStorage.store(task.getId(), task);
        return null;
    }

    @Override
    public Task read(String id) throws StorageException, ManagementException {
        if (!taskStorage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        return taskStorage.load(id, Task.class);
    }

    @Override
    public List<Task> readAll() throws StorageException {
        return taskStorage.loadAll(Task.class);
    }

    @Override
    public ManagementResult update(String id, Task task) throws StorageException, ManagementException {
        if (!taskStorage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        taskStorage.store(id, task);
        // TODO: update jobs that use this task
        return null;
    }

    @Override
    public ManagementResult delete(String id) throws StorageException, ManagementException {
        if (!taskStorage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        taskStorage.delete(id);
        // TODO: archive jobs that use this task
        return null;
    }
}
