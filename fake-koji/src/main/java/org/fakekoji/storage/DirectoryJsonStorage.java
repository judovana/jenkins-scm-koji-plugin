package org.fakekoji.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryJsonStorage<T> implements Storage<T> {

    private static final String SUFFIX= ".json";
    private final File storageFile;

    DirectoryJsonStorage(File storageFile) {
        this.storageFile = storageFile;
    }

    @Override
    public void store(String id, T t) throws StorageException {
        try {
            final File file = new File(storageFile.getAbsolutePath() + '/' + id + SUFFIX);
            final PrintWriter writer = new PrintWriter(file.getAbsolutePath(), "UTF-8");
            final ObjectMapper objectMapper = new ObjectMapper();
            writer.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(t));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new StorageException(e.getMessage());
        }
    }


    @Override
    public void delete(String id) throws StorageException {
        final File file = new File(storageFile.getAbsolutePath() + '/' + id + SUFFIX);
        if (!file.delete()) {
            throw new StorageException("Failed to delete " + file.getName());
        }
    }

    @Override
    public T load(String id, Class<T> valueType) throws StorageException {
        final File fileToLoad = new File(storageFile.getAbsolutePath() + '/' + id + SUFFIX);
        try {
            final BufferedReader reader = new BufferedReader(
                    new FileReader(fileToLoad.getAbsolutePath())
            );
            final StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return new ObjectMapper().readValue(stringBuilder.toString(), valueType);
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
