package company.vk.edu.distrib.compute.playingpeano.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class PersistentByteArrayDao implements Dao<byte[]> {
    private final Path directory;
    private final Lock readLock;
    private final Lock writeLock;
    private boolean closed;

    PersistentByteArrayDao(Path directory) throws IOException {
        this.directory = directory;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        Files.createDirectories(directory);
    }

    @Override
    public byte[] get(String key) throws IOException {
        validateKey(key);
        readLock.lock();
        try {
            ensureOpen();
            try {
                return Files.readAllBytes(pathFor(key));
            } catch (NoSuchFileException exception) {
                if (!Files.isDirectory(directory)) {
                    throw exception;
                }
                throw new NoSuchElementException("No value for key: " + key, exception);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        validateKey(key);
        writeLock.lock();
        try {
            ensureOpen();
            Path temporaryFile = Files.createTempFile(directory, "value-", ".tmp");
            try {
                Files.write(
                    temporaryFile,
                    value,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
                Files.move(
                    temporaryFile,
                    pathFor(key),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException | RuntimeException | Error exception) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
                throw exception;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        validateKey(key);
        writeLock.lock();
        try {
            ensureOpen();
            Files.deleteIfExists(pathFor(key));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            closed = true;
        } finally {
            writeLock.unlock();
        }
    }

    boolean isAvailable() {
        readLock.lock();
        try {
            return !closed
                && Files.isDirectory(directory)
                && Files.isReadable(directory)
                && Files.isWritable(directory);
        } finally {
            readLock.unlock();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("DAO is closed");
        }
    }

    private static void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
    }

    private Path pathFor(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
        return directory.resolve(HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8))));
    }
}
