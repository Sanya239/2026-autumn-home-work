package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import company.vk.edu.distrib.compute.Dao;

public final class FileStringDao implements Dao<String> {
    private final Path file;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, String> entries = new ConcurrentHashMap<>();

    public FileStringDao(Path storageFile) throws IOException {
        file = storageFile.toAbsolutePath().normalize();
        load();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        checkKey(key);
        lock.lock();
        try {
            String stored = entries.get(key);
            if (stored == null) {
                throw new NoSuchElementException("Key is not stored");
            }
            return stored;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        checkKey(key);
        lock.lock();
        try {
            String previous = entries.put(key, value);
            try {
                save();
            } catch (IOException ex) {
                if (previous == null) {
                    entries.remove(key);
                } else {
                    entries.put(key, previous);
                }
                throw ex;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        checkKey(key);
        lock.lock();
        try {
            String removed = entries.remove(key);
            if (removed == null) {
                return;
            }
            try {
                save();
            } catch (IOException ex) {
                entries.put(key, removed);
                throw ex;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            entries.clear();
        } finally {
            lock.unlock();
        }
    }

    private static void checkKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
    }

    private void load() throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (value != null) {
                entries.put(name, value);
            }
        }
    }

    private void save() throws IOException {
        Path parent = file.getParent();
        Path name = file.getFileName();
        if (parent == null || name == null) {
            throw new IOException("Storage path has no parent directory");
        }
        Files.createDirectories(parent);
        Path lockFile = parent.resolve(name.toString() + ".lock");
        try (FileChannel channel = FileChannel.open(
                lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock fileLock = channel.lock()) {
            if (!fileLock.isValid()) {
                throw new IOException("Failed to lock storage");
            }
            Path temporary = Files.createTempFile(parent, ".dao-", ".tmp");
            try {
                writeProperties(temporary);
                moveIntoPlace(temporary, file);
            } catch (IOException ex) {
                deleteQuietly(temporary, ex);
                throw ex;
            }
        }
    }

    private void writeProperties(Path target) throws IOException {
        Properties properties = new Properties();
        properties.putAll(entries);
        try (OutputStream output = Files.newOutputStream(target)) {
            properties.store(output, "mperikov-urlshortener");
        }
    }

    private static void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path temporary, IOException failure) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ex) {
            failure.addSuppressed(ex);
        }
    }
}
