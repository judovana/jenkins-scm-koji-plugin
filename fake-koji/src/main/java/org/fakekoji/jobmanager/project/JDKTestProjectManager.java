package org.fakekoji.jobmanager.project;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ManagementUtils;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class JDKTestProjectManager implements Manager<JDKTestProject> {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final Storage<JDKTestProject> storage;

    public JDKTestProjectManager(final Storage<JDKTestProject> storage) {
        this.storage = storage;
    }

    @Override
    public JDKTestProject create(JDKTestProject jdkTestProject) throws StorageException, ManagementException {
        ManagementUtils.checkID(jdkTestProject.getId(), storage, false);
        LOGGER.info("Creating JDK test project " + jdkTestProject.getId());
        LOGGER.info("Storing config of JDK test project " + jdkTestProject.getId());
        storage.store(jdkTestProject.getId(), jdkTestProject);
        return jdkTestProject;
    }

    @Override
    public JDKTestProject read(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        return storage.load(id, JDKTestProject.class);
    }

    @Override
    public List<JDKTestProject> readAll() throws StorageException {
        List<JDKTestProject> l = storage.loadAll(JDKTestProject.class);
        Collections.sort(l);
        return l;
    }

    @Override
    public JDKTestProject update(String id, JDKTestProject jdkTestProject) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        LOGGER.info("Updating JDK test project " + jdkTestProject.getId());
        LOGGER.info("Storing config of JDK test project " + id);
        storage.store(id, jdkTestProject);
        return jdkTestProject;
    }

    @Override
    public JDKTestProject delete(String id) throws StorageException, ManagementException {
        ManagementUtils.checkID(id, storage);
        LOGGER.info("Deleting JDK test project " + id);
        final JDKTestProject project = storage.load(id, JDKTestProject.class);
        storage.delete(id);
        return project;
    }

    @Override
    public boolean contains(String id) {
        return storage.contains(id);
    }
}
