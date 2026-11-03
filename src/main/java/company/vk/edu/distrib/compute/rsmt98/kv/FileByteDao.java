package company.vk.edu.distrib.compute.rsmt98.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class FileByteDao implements Dao<byte[]> {
    private final Path dir;
    private final Lock lock = new ReentrantLock();
    private boolean isClosed;

    public FileByteDao(Path path) throws IOException {
        dir = path;
        Files.createDirectories(dir);
    }

    @Override
    public byte[] get(String key) throws IOException {
        lock.lock();
        try {
            checkOpen();
            return Files.readAllBytes(resolveKeyPath(key));
        } catch (NoSuchFileException e) {
            if (!Files.isDirectory(dir)) {
                throw e;
            }
            throw new NoSuchElementException("No value exists for the key", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        lock.lock();
        try {
            checkOpen();
            Path target = resolveKeyPath(key);
            Path tmpFile = Files.createTempFile(dir, null, null);
            try {
                Files.write(tmpFile, value);
                Files.move(
                        tmpFile,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException | Error e) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException ee) {
                    e.addSuppressed(ee);
                }
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        lock.lock();
        try {
            checkOpen();
            if (!Files.deleteIfExists(resolveKeyPath(key)) && !Files.isDirectory(dir)) {
                throw new NoSuchFileException(dir.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isAvailable() {
        lock.lock();
        try {
            return !isClosed
                    && Files.isDirectory(dir)
                    && Files.isReadable(dir)
                    && Files.isWritable(dir);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            isClosed = true;
        } finally {
            lock.unlock();
        }
    }

    private void checkOpen() throws IOException {
        if (isClosed) {
            throw new IOException("Storage is closed");
        }
    }

    private Path resolveKeyPath(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty");
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        for (int i = 0; i < key.length(); ++i) {
            char ch = key.charAt(i);
            digest.update((byte) (ch >>> Byte.SIZE));
            digest.update((byte) ch);
        }
        return dir.resolve(HexFormat.of().formatHex(digest.digest()) + ".bin");
    }
}
