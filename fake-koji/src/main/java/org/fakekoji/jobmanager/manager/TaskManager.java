package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Task;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.Collections;
import java.util.List;

public class TaskManager implements Manager<Task> {

    private final Storage<Task> taskStorage;
    private final JobUpdater jobUpdater;

    public TaskManager(
            final Storage<Task> taskStorage,
            final JobUpdater jobUpdater
    ) {
        this.taskStorage = taskStorage;
        this.jobUpdater = jobUpdater;
    }

    @Override
    public ManagementResult<Task> create(Task task) throws StorageException, ManagementException {
        if (taskStorage.contains(task.getId())) {
            throw new ManagementException("Task with id " + task.getId() + " already exists");
        }
        taskStorage.store(task.getId(), task);
        return new ManagementResult<>(task, new JobUpdateResults());
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
        List<Task> l = taskStorage.loadAll(Task.class);
        Collections.sort(l);
        return l;
    }

    @Override
    public ManagementResult<Task> update(String id, Task task) throws StorageException, ManagementException {
        if (!taskStorage.contains(id)) {
            throw new ManagementException("No task with id: " + id);
        }
        taskStorage.store(id, task);
        final JobUpdateResults jobUpdateResults = jobUpdater.update(task);
        return new ManagementResult<>(task, jobUpdateResults);
    }

    @Override
    public ManagementResult<Task> delete(String id) throws StorageException, ManagementException {
        throw new ManagementException("Not supported");
    }
}
