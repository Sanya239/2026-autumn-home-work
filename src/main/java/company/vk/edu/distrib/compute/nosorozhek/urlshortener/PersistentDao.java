package company.vk.edu.distrib.compute.nosorozhek.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class PersistentDao implements Dao<String> {
    private final Map<String, String> entries;
    private final Path filePath;

    PersistentDao(Path filePath) throws IOException {
        this.filePath = filePath;
        entries = loadEntries();
    }

    private void saveEntries() throws IOException {
        Path temporary = Files.createTempFile(filePath.getParent(), "urlshortener-data-", ".tmp");
        try {
            try (OutputStream outputStream = Files.newOutputStream(temporary);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                objectOutputStream.writeObject(entries);
            }
            Files.move(temporary, filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadEntries() throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (Map<String, String>) objectInputStream.readObject();
        } catch (NoSuchFileException e) {
            return new HashMap<>();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException {
        String value = entries.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException {
        entries.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException {
        entries.remove(key);
    }

    @Override
    public void close() throws IOException {
        saveEntries();
    }
}
