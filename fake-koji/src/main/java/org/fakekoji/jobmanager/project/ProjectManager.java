package org.fakekoji.jobmanager.project;

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
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectManager implements Manager {

    static final String CONFIG_FILE = "config.xml";

    private final ConfigManager configManager;
    private final File jenkinsJobsRoot;
    private final File jenkinsJobsArchiveRoot;
    private final File repositoriesRoot;

    public ProjectManager(
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
    public String create(String json) throws StorageException, ManagementException {
        final ObjectMapper mapper = new ObjectMapper();
        final JDKProject project;
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        try {
            project = mapper.readValue(json, JDKProject.class);
            if (storage.contains(project.getId())) {
                throw new ManagementException("JDKProject with id: " + project.getId() + " already exists");
            }
            final Set<Job> jobs = new ProjectParser(configManager, repositoriesRoot).parse(project);
            // TODO: clone repo
            generate(jobs);
            storage.store(project.getId(), project);
            return mapper.writeValueAsString(project);
        } catch (IOException e) {
            throw new ManagementException("Invalid json", e);
        }
    }

    @Override
    public String read(String id) throws StorageException, ManagementException {
        return null; // TODO
    }

    @Override
    public String readAll() throws StorageException {
        return null; // TODO
    }

    @Override
    public String update(String id, String json) throws StorageException, ManagementException {
        final ObjectMapper mapper = new ObjectMapper();
        final JDKProject project;
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        final JDKProject oldProject = storage.load(id, JDKProject.class);
        try {
            project = mapper.readValue(json, JDKProject.class);
            final Set<Job> newProjectJobs = new ProjectParser(configManager, repositoriesRoot).parse(project);
            final Set<Job> oldProjectJobs = new ProjectParser(configManager, repositoriesRoot).parse(oldProject);
            final Set<File> jobFiles = Arrays.stream(jenkinsJobsRoot.listFiles())
                    .filter(file -> file.getName().contains(project.getId()))
                    .collect(Collectors.toSet());
            final Set<File> jobFilesToRevive = Arrays.stream(jenkinsJobsArchiveRoot.listFiles())
                    .filter(file -> file.getName().contains(project.getId()))
                    .filter(file -> newProjectJobs
                            .stream()
                            .anyMatch((Job job) -> job.toString().startsWith(file.getName())))
                    .collect(Collectors.toSet());
            final Set<File> jobFilesToArchive = oldProjectJobs
                    .stream()
                    .filter(job -> !newProjectJobs.contains(job))
                    .map(job -> jobFiles
                            .stream()
                            .filter((File file) -> file.getName().startsWith(job.toString()))
                            .findFirst().orElse(null)
                    )
                    .collect(Collectors.toSet());
            final Set<Job> jobsToCreate = newProjectJobs
                    .stream()
                    .filter(job -> !oldProjectJobs.contains(job))
                    .collect(Collectors.toSet());
            revive(jobFilesToRevive);
            archive(jobFilesToArchive);
            generate(jobsToCreate);
        } catch (IOException e) {
            throw new ManagementException("Invalid json", e);
        }
        return null;
    }

    @Override
    public String delete(String id) throws StorageException, ManagementException {
        return null; // TODO
    }

    void generate(Set<Job> jobs) throws IOException {
        for (final Job job : jobs) {
            final File jobDir = Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile();
            jobDir.mkdir();
            Utils.writeToFile(
                    Paths.get(jobDir.getAbsolutePath(), CONFIG_FILE),
                    job.generateTemplate()
            );
        }
    }

    void archive(Set<File> jobFiles) throws IOException {
        for (final File jobFile : jobFiles) {
            Utils.moveFile(
                    jobFile,
                    Paths.get(jenkinsJobsArchiveRoot.getAbsolutePath(), jobFile.getName()).toFile()
            );
        }
    }

    void revive(Set<File> jobFiles) throws IOException {
        for (final File jobFile : jobFiles) {
            Utils.moveFile(
                    jobFile,
                    Paths.get(jenkinsJobsRoot.getAbsolutePath(), jobFile.getName()).toFile()
            );
        }
    }
}
