package company.vk.edu.distrib.compute.aartchik.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PersistentStringDao implements Dao<String> {
    private static final byte UPSERT = 1;
    private static final byte DELETE = 2;
    private static final int MAX_FIELD_SIZE = 16 * 1024 * 1024;

    private final Map<String, String> entries = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final FileChannel journal;
    private boolean closed;

    public PersistentStringDao(Path journalPath) throws IOException {
        Files.createDirectories(journalPath.getParent());
        load(journalPath);
        journal = FileChannel.open(
                journalPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    @Override
    public String get(String key) throws IOException {
        validateKey(key);
        lock.readLock().lock();
        try {
            ensureOpen();
            String value = entries.get(key);
            if (value == null) {
                throw new NoSuchElementException("No value for key: " + key);
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        validateKey(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        lock.writeLock().lock();
        try {
            ensureOpen();
            append(UPSERT, keyBytes, valueBytes);
            entries.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        validateKey(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        lock.writeLock().lock();
        try {
            ensureOpen();
            append(DELETE, keyBytes, new byte[0]);
            entries.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isAvailable() {
        lock.readLock().lock();
        try {
            return !closed && journal.isOpen();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            if (!closed) {
                closed = true;
                journal.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void append(byte operation, byte[] key, byte[] value) throws IOException {
        ByteBuffer record = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES * 2 + key.length + value.length);
        record.put(operation);
        record.putInt(key.length);
        record.putInt(value.length);
        record.put(key);
        record.put(value);
        record.flip();
        while (record.hasRemaining()) {
            journal.write(record);
        }
        journal.force(true);
    }

    private void load(Path journalPath) throws IOException {
        if (!Files.exists(journalPath)) {
            return;
        }
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(journalPath)))) {
            while (true) {
                byte operation;
                try {
                    operation = input.readByte();
                } catch (EOFException endOfFile) {
                    return;
                }
                int keyLength = readLength(input, "key");
                int valueLength = readLength(input, "value");
                String key = readString(input, keyLength);
                if (operation == UPSERT) {
                    String value = readString(input, valueLength);
                    entries.put(key, value);
                } else if (operation == DELETE && valueLength == 0) {
                    entries.remove(key);
                } else {
                    throw new IOException("Unknown journal operation: " + operation);
                }
            }
        }
    }

    private static int readLength(DataInputStream input, String field) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_FIELD_SIZE) {
            throw new IOException("Invalid " + field + " length in journal: " + length);
        }
        return length;
    }

    private static String readString(DataInputStream input, int length) throws IOException {
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("DAO is closed");
        }
    }
}
