package org.fakekoji.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fakekoji.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryJsonStorage<T> implements Storage<T> {

    private static final String SUFFIX= ".json";
    private final File storageFile;

    public DirectoryJsonStorage(File storageFile) {
        this.storageFile = storageFile;
    }

    @Override
    public void store(String id, T t) throws StorageException {
        try {
            Utils.writeToFile(
                    Paths.get(storageFile.getAbsolutePath(), id + SUFFIX),
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(t)
            );
        } catch (IOException e) {
            throw new StorageException(e.getMessage());
        }
    }


    @Override
    public void delete(String id) throws StorageException {
        final File file = Paths.get(storageFile.getAbsolutePath(), id + SUFFIX).toFile();
        if (!file.delete()) {
            throw new StorageException("Failed to delete " + file.getName());
        }
    }

    @Override
    public T load(String id, Class<T> valueType) throws StorageException {
        try {
            return new ObjectMapper().readValue(
                    Utils.readFile(Paths.get(storageFile.getAbsolutePath(), id + SUFFIX).toFile()),
                    valueType
            );
        } catch (IOException e) {
            throw new StorageException(e.getMessage());
        }
    }

    @Override
    public List<T> loadAll(Class<T> valueType) throws StorageException {
        final File[] files = storageFile.listFiles();
        final List<T> list = new ArrayList<>();
        if (files == null) {
            return Collections.emptyList();
        }
        for (final File file : files) {
            if (file.getName().endsWith(SUFFIX)) {
                list.add(load(file.getName().replace(SUFFIX, ""), valueType));
            }
        }
        return list;
    }

    @Override
    public boolean contains(String id) {
        final String fileName = id + SUFFIX;
        final File[] files = storageFile.listFiles();
        if (files == null) {
            return false;
        }
        for (final File file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }
}
