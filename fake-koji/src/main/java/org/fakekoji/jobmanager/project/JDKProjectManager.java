package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Product;
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
import java.util.Set;
import java.util.logging.Logger;

public class JDKProjectManager implements Manager<JDKProject> {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

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
    public ManagementResult create(JDKProject project) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        final Storage<Product> productStorage = configManager.getProductStorage();
        if (storage.contains(project.getId())) {
            throw new ManagementException("JDKProject with id: " + project.getId() + " already exists");
        }
        if (!productStorage.contains(project.getProduct())) {
            throw new ManagementException("Unknown product: " + project.getProduct());
        }
        final Product product = productStorage.load(project.getProduct(), Product.class);
        final Set<Job> jobs = new JDKProjectParser(configManager, repositoriesRoot, scriptsRoot).parse(project);
        LOGGER.info("Storing JDK project " + project.getId() + " before cloning its repository");
        storage.store(project.getId(), setProjectRepoStatus(project, JDKProject.RepoState.CLONING));
        final JDKProject.RepoState repoState = cloneProject(
                project.getId(),
                project.getUrl(),
                repositoriesRoot,
                product
        );
        LOGGER.info("Storing JDK project " + project.getId() + " after cloning its repository");
        storage.store(project.getId(), setProjectRepoStatus(project, repoState));
        if (repoState == JDKProject.RepoState.CLONE_ERROR) {
            throw new StorageException("Cloning failed. Skipping creating jobs");
        }
        LOGGER.info("Creating project's jobs");
        final JobUpdateResults results = jobUpdater.update(Collections.emptySet(), jobs);
        return new ManagementResult(
                project,
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
        return configManager.getJdkProjectStorage().loadAll(JDKProject.class);
    }

    @Override
    public ManagementResult update(String id, JDKProject jdkProject) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        LOGGER.info("Updating JDK project " + jdkProject.getId());
        final JDKProject oldProject = storage.load(id, JDKProject.class);
        if (!oldProject.getUrl().equals(jdkProject.getUrl())) {
            updateProjectUrl();
        }
        final Set<Job> newJobs = new JDKProjectParser(configManager, repositoriesRoot, scriptsRoot).parse(jdkProject);
        final Set<Job> oldJobs = new JDKProjectParser(configManager, repositoriesRoot, scriptsRoot).parse(oldProject);
        LOGGER.info("Updating the project's jobs");
        LOGGER.info("Storing the project");
        storage.store(id, jdkProject);
        final JobUpdateResults results = jobUpdater.update(oldJobs, newJobs);
        return new ManagementResult(
                jdkProject,
                results
        );
    }

    @Override
    public ManagementResult delete(String id) throws StorageException, ManagementException {
        final Storage<JDKProject> storage = configManager.getJdkProjectStorage();
        if (!storage.contains(id)) {
            throw new ManagementException("JDKProject with id: " + id + " doesn't exists");
        }
        final JDKProject jdkProject = storage.load(id, JDKProject.class);
        LOGGER.info("Deleting JDK project " + jdkProject.getId());
        final Set<Job> jobs = new JDKProjectParser(configManager, repositoriesRoot, scriptsRoot).parse(jdkProject);
        LOGGER.info("Archiving the project's jobs");
        storage.delete(id);
        final JobUpdateResults results = jobUpdater.update(jobs, Collections.emptySet());
        return new ManagementResult(
                null,
                results
        );
    }

    JDKProject.RepoState cloneProject(
            final String projectName,
            final String projectUrl,
            final File repositoriesRoot,
            final Product product
    ) {
        LOGGER.info("Starting cloning project's repository");
        final ProcessBuilder processBuilder = new ProcessBuilder(
                "bash",
                Paths.get(scriptsRoot.getAbsolutePath(), "otool", "clone_repo.sh").toString(),
                product.getVersion(),
                projectUrl,
                Paths.get(repositoriesRoot.getAbsolutePath(), projectName).toString()
        );
        processBuilder.directory(repositoriesRoot);
        LOGGER.info("Executing command: " + String.join(" ", processBuilder.command()));
        try {
            final Process cloningProcess = processBuilder.start();
            final int exitCode = cloningProcess.waitFor();
            String line;
            final BufferedReader outputReader = new BufferedReader(new InputStreamReader(cloningProcess.getInputStream()));
            while ((line = outputReader.readLine()) != null) {
                System.out.println(line);
            }
            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(cloningProcess.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.out.println(line);
            }
            return exitCode == 0 ? JDKProject.RepoState.CLONED : JDKProject.RepoState.CLONE_ERROR;
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Exception occurred during cloning: " + e.getMessage());
            return JDKProject.RepoState.CLONE_ERROR;
        }
    }

    void updateProjectUrl() {
        // TODO: implement
    }

    JDKProject setProjectRepoStatus(final JDKProject jdkProject, final JDKProject.RepoState repoState) {
        return new JDKProject(
                jdkProject.getId(),
                jdkProject.getType(),
                repoState,
                jdkProject.getUrl(),
                jdkProject.getBuildProviders(),
                jdkProject.getProduct(),
                jdkProject.getJobConfiguration()
        );
    }
}
