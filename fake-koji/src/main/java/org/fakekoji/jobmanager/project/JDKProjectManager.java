package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class JDKProjectManager implements Manager<JDKProject> {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private Thread cloningThread;

    private final ConfigManager configManager;
    private final JobUpdater jobUpdater;
    private final File repositoriesRoot;
    private final File scriptsRoot;

    public JDKProjectManager(
            final ConfigManager configManager,
            final JobUpdater jobUpdater,
            final File repositoriesRoot,
            final File scriptsRoot
    ) {
        this.configManager = configManager;
        this.jobUpdater = jobUpdater;
        this.repositoriesRoot = repositoriesRoot;
        this.scriptsRoot = scriptsRoot;
    }

    @Override
    public ManagementResult<JDKProject> create(JDKProject project) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (storage.contains(project.getId())) {
            throw new ManagementException("JDKProject with id: " + project.getId() + " already exists");
        }
        final JDKProject createdJDKProject = cloneProject(project).config;
        LOGGER.info("Storing JDK project " + project.getId());
        storage.store(project.getId(), setProjectRepoStatus(project, JDKProject.RepoState.CLONING));
        LOGGER.info("Creating project's jobs");
        final JobUpdateResults results = jobUpdater.update(null, project);
        return new ManagementResult<>(
                createdJDKProject,
                results
        );
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
        List<JDKProject> l = configManager.getJdkProjectStorage().loadAll(JDKProject.class);
        Collections.sort(l);
        return l;
    }

    @Override
    public ManagementResult<JDKProject> update(String id, JDKProject jdkProject) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        LOGGER.info("Updating JDK project " + jdkProject.getId());
        final JDKProject oldProject = storage.load(id, JDKProject.class);
        if (!oldProject.getUrl().equals(jdkProject.getUrl())) {
            updateProjectUrl();
        }
        LOGGER.info("Storing the project");
        storage.store(id, jdkProject);
        LOGGER.info("Updating the project's jobs");
        final JobUpdateResults results = jobUpdater.update(oldProject, jdkProject);
        return new ManagementResult<>(
                jdkProject,
                results
        );
    }

    @Override
    public ManagementResult<JDKProject> delete(String id) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        final JDKProject jdkProject = storage.load(id, JDKProject.class);
        LOGGER.info("Deleting JDK project " + jdkProject.getId());
        storage.delete(id);
        LOGGER.info("Archiving the project's jobs");
        final JobUpdateResults results = jobUpdater.update(jdkProject, null);
        return new ManagementResult<>(
                jdkProject,
                results
        );
    }

    public ManagementResult<JDKProject> cloneProject(final JDKProject jdkProject) throws StorageException, ManagementException {
        final Storage<JDKVersion> productStorage = configManager.getJdkVersionStorage();
        if (!productStorage.contains(jdkProject.getProduct().getJdk())) {
            throw new ManagementException("Unknown product: " + jdkProject.getProduct());
        }
        final JDKVersion jdkVersion = productStorage.load(jdkProject.getProduct().getJdk(), JDKVersion.class);
        cloningThread = new Thread(createRepoCloningThread(jdkProject, jdkVersion));
        cloningThread.start();
        return new ManagementResult<>(setProjectRepoStatus(jdkProject, JDKProject.RepoState.CLONING), null);
    }

    private Runnable createRepoCloningThread(
            final JDKProject jdkProject,
            final JDKVersion jdkVersion
    ) {
        return () -> {
            LOGGER.info("Starting cloning project's repository");
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    "bash",
                    Paths.get(scriptsRoot.getAbsolutePath(), "otool", "clone_repo.sh").toString(),
                    jdkVersion.getVersion(),
                    jdkProject.getUrl(),
                    Paths.get(repositoriesRoot.getAbsolutePath(), jdkProject.getId()).toString()
            );
            processBuilder.directory(repositoriesRoot);
            LOGGER.info("Executing command: " + String.join(" ", processBuilder.command()));
            final Process cloningProcess;
            int exitCode;
            try {
                cloningProcess = processBuilder.start();
                exitCode = cloningProcess.waitFor();
                String line;
                final BufferedReader outputReader = new BufferedReader(new InputStreamReader(cloningProcess.getInputStream()));
                while ((line = outputReader.readLine()) != null) {
                    System.out.println(line);
                }
                final BufferedReader errorReader = new BufferedReader(new InputStreamReader(cloningProcess.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (InterruptedException | IOException e) {
                LOGGER.severe("Exception occurred during cloning: " + e.getMessage());
                exitCode = 1;
            }
            final JDKProject.RepoState repoState = exitCode == 0 ? JDKProject.RepoState.CLONED : JDKProject.RepoState.CLONE_ERROR;
            final JDKProject clonedJDKProject = setProjectRepoStatus(jdkProject, repoState);
            try {
                LOGGER.info("Updating JDK project " + clonedJDKProject.getId() + " after cloning its repository");
                configManager.getJdkProjectStorage().store(clonedJDKProject.getId(), clonedJDKProject);
            } catch (StorageException e) {
                LOGGER.severe(e.getMessage());
            }
        };
    }

    void updateProjectUrl() {
        // TODO: implement
    }

    JDKProject setProjectRepoStatus(final JDKProject jdkProject, final JDKProject.RepoState repoState) {
        return new JDKProject(
                jdkProject.getId(),
                jdkProject.getProduct(),
                repoState,
                jdkProject.getUrl(),
                jdkProject.getBuildProviders(),
                jdkProject.getJobConfiguration(),
                jdkProject.getVariables()
        );
    }

    Thread getCloningThread() {
        return cloningThread;
    }

    public JobUpdateResults regenerateAll() throws ManagementException, StorageException {
        JobUpdateResults sum = new JobUpdateResults();
        JenkinsJobUpdater.wakeUpJenkins();
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        final List<JDKProject> jdkProjects = storage.loadAll(JDKProject.class);
        for (final JDKProject jdkProject : jdkProjects) {
            JobUpdateResults r = jobUpdater.regenerate(jdkProject);
            sum = sum.add(r);
        }
        return sum;
    }

    @Override
    public boolean contains(String id) {
        return configManager.getJdkProjectStorage().contains(id);
    }
}
