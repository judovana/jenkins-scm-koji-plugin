package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementResult;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.List;
import java.util.logging.Logger;

public class JDKTestProjectManager implements Manager<JDKTestProject> {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final Storage<JDKTestProject> storage;
    private final JobUpdater jobUpdater;

    public JDKTestProjectManager(
            final Storage<JDKTestProject> storage,
            final JobUpdater jobUpdater
    ) {
        this.storage = storage;
        this.jobUpdater = jobUpdater;
    }

    @Override
    public ManagementResult<JDKTestProject> create(JDKTestProject jdkTestProject) throws StorageException, ManagementException {
        ManagementUtils.checkID(jdkTestProject.getId(), storage, false);
        LOGGER.info("Creating JDK test project " + jdkTestProject.getId());
        LOGGER.info("Storing config of JDK test project " + jdkTestProject.getId());
        storage.store(jdkTestProject.getId(), jdkTestProject);
        LOGGER.info("Creating jobs of JDK test project " + jdkTestProject.getId());
        final JobUpdateResults results = jobUpdater.update(null, jdkTestProject);
        return new ManagementResult<>(
                jdkTestProject,
                results
        );
    }

    @Override
    public JDKTestProject read(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        return storage.load(id, JDKTestProject.class);
    }

    @Override
    public List<JDKTestProject> readAll() throws StorageException {
        return storage.loadAll(JDKTestProject.class);
    }

    @Override
    public ManagementResult<JDKTestProject> update(String id, JDKTestProject jdkTestProject) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        LOGGER.info("Updating JDK test project " + jdkTestProject.getId());
        final JDKTestProject oldProject = storage.load(id, JDKTestProject.class);
        LOGGER.info("Storing config of JDK test project " + id);
        storage.store(id, jdkTestProject);
        LOGGER.info("Updating jobs of JDK test project " + jdkTestProject.getId());
        final JobUpdateResults results = jobUpdater.update(oldProject, jdkTestProject);
        return new ManagementResult<>(
                jdkTestProject,
                results
        );
    }

    @Override
    public ManagementResult<JDKTestProject> delete(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        LOGGER.info("Deleting JDK test project " + id);
        final JDKTestProject project = storage.load(id, JDKTestProject.class);
        storage.delete(id);
        LOGGER.info("Archiving jobs of JDK test project " + id);
        final JobUpdateResults results = jobUpdater.update(project, null);
        return new ManagementResult<>(
                null,
                results
        );
    }

}
