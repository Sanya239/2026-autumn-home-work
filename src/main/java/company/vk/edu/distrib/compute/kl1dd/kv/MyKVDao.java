package company.vk.edu.distrib.compute.kl1dd.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyKVDao implements Dao<byte[]> {
    private static final Path STORAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "kl1dd-kv-storage");
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    public MyKVDao() throws IOException {
        Files.createDirectories(STORAGE_DIR);
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        LOCK.readLock().lock();
        try {
            Path filePath = STORAGE_DIR.resolve(key);

            if (!Files.exists(filePath)) {
                throw new NoSuchElementException();
            }

            return Files.readAllBytes(filePath);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        LOCK.writeLock().lock();
        Path tempFile = null;
        try {
            Path filePath = STORAGE_DIR.resolve(key);

            tempFile = Files.createTempFile(STORAGE_DIR, "kv-", ".tmp");
            Files.write(tempFile, value);
            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            try {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } finally {
                LOCK.writeLock().unlock();
            }
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        LOCK.writeLock().lock();
        try {
            Path filePath = STORAGE_DIR.resolve(key);
            Files.deleteIfExists(filePath);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // later
    }
}
