package org.fakekoji.jobmanager.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fakekoji.Utils;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JDKProjectManager implements Manager<JDKProject> {

    static final String CONFIG_FILE = "config.xml";

    private final ConfigManager configManager;
    private final File jenkinsJobsRoot;
    private final File jenkinsJobsArchiveRoot;
    private final File repositoriesRoot;

    public JDKProjectManager(
            final ConfigManager configManager,
            final File jenkinsJobsRoot,
            final File jenkinsJobsArchiveRoot,
            final File repositoriesRoot
    ) {
        this.configManager = configManager;
        this.jenkinsJobsRoot = jenkinsJobsRoot;
        this.jenkinsJobsArchiveRoot = jenkinsJobsArchiveRoot;
        this.repositoriesRoot = repositoriesRoot;
    }

    @Override
    public void create(JDKProject project) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        try {
            if (storage.contains(project.getId())) {
                throw new ManagementException("JDKProject with id: " + project.getId() + " already exists");
            }
            final Set<Job> jobs = new JDKProjectParser(configManager, repositoriesRoot).parse(project);
            // TODO: clone repo
            generate(jobs);
            storage.store(project.getId(), project);
        } catch (IOException e) {
            throw new StorageException(e.getMessage());
        }
    }

    @Override
    public JDKProject read(String id) throws StorageException, ManagementException {
        if (!configManager.getJdkProjectStorage().contains(id)) {
            throw new ManagementException("No project with id: " + id);
        }
        return configManager.getJdkProjectStorage().load(id, JDKProject.class);
    }

    @Override
    public List<JDKProject> readAll() throws StorageException {
        return configManager.getJdkProjectStorage().loadAll(JDKProject.class);
    }

    @Override
    public void update(String id, String json) throws StorageException, ManagementException {
        final ObjectMapper mapper = new ObjectMapper();
        final JDKProject project;
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        final JDKProject oldProject = storage.load(id, JDKProject.class);
        try {
            project = mapper.readValue(json, JDKProject.class);
        } catch (IOException e) {
            throw new ManagementException("Invalid json", e);
        }
        if (!oldProject.getUrl().equals(project.getUrl())) {
            updateProjectUrl();
        }
        final Set<Job> newJobs = new JDKProjectParser(configManager, repositoriesRoot).parse(project);
        final Set<Job> oldJobs = new JDKProjectParser(configManager, repositoriesRoot).parse(oldProject);
        updateJobs(newJobs, oldJobs);
    }

    @Override
    public void delete(String id) throws StorageException, ManagementException {
    }

    void updateProjectUrl() {
        // TODO: implement
    }

    void updateJobs(Set<Job> oldJobs, Set<Job> newJobs) {
        final Set<String> archivedJobs = new HashSet<>(Arrays.asList(jenkinsJobsArchiveRoot.list()));
        oldJobs.stream()
                .filter(oldJob -> newJobs.stream().noneMatch(newJob -> oldJob.toString().equals(newJob.toString())))
                .forEach(this::archive);
        for (final Job job : newJobs) {
            if (archivedJobs.contains(job.toString())) {
                revive(job);
                continue;
            }
            final Optional<Job> optional = oldJobs.stream()
                    .filter(oldJob -> job.toString().equals(oldJob.toString()))
                    .findAny();
            if (optional.isPresent()) {
                final Job oldJob = optional.get();
                if (!oldJob.equals(job)) {
                    update(job);
                }
                continue;
            }
            generate(job);
        }
    }

    void generate(Set<Job> jobs) throws IOException {
        for (final Job job : jobs) {
            generate(job);
        }
    }

    boolean generate(Job job) {
        final File jobDir = Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile();
        if (!jobDir.mkdir()) {
            return false;
        }
        try {
            Utils.writeToFile(
                    Paths.get(jobDir.getAbsolutePath(), CONFIG_FILE),
                    job.generateTemplate()
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean revive(Job job) {
        try {
            Utils.moveFile(
                    Paths.get(jenkinsJobsArchiveRoot.getAbsolutePath(), job.toString()).toFile(),
                    Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile()
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean archive(Job job) {
        try {
            Utils.moveFile(
                    Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile(),
                    Paths.get(jenkinsJobsArchiveRoot.getAbsolutePath(), job.toString()).toFile()
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean update(Job job) {
        try {
            Utils.writeToFile(
                    Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()),
                    job.generateTemplate()
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
