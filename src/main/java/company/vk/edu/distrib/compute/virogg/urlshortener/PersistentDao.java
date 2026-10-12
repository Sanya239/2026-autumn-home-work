package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import company.vk.edu.distrib.compute.Dao;

public final class PersistentDao implements Dao<String> {
    private static final String UPSERT = "+";
    private static final String DELETE = "-";
    private static final String SEPARATOR = "\t";
    private static final String END = "\n";
    private static final int UPSERT_FIELDS = 4;
    private static final int DELETE_FIELDS = 3;

    private final Path dir;
    private final ConcurrentMap<String, String> data = new ConcurrentHashMap<>();
    private final FileChannel lockFile;
    private final FileChannel journal;
    private final Lock writeLock = new ReentrantLock();

    public PersistentDao(Path dir, String fileName) throws IOException {
        this.dir = Files.createDirectories(dir);
        this.lockFile = FileChannel.open(dir.resolve(fileName + ".lock"),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            acquire(lockFile, fileName);
            Path file = dir.resolve(fileName);
            if (Files.exists(file)) {
                replay(file);
            }
            compact(file, dir.resolve(fileName + ".tmp"));
            this.journal = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            lockFile.close();
            throw e;
        }
    }

    @Override
    public String get(String key) {
        String value = data.get(requireKey(key));
        if (value == null) {
            throw new NoSuchElementException("No value for key " + key);
        }
        return value;
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        String record = upsertRecord(requireKey(key), value);
        writeLock.lock();
        try {
            append(record);
            data.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        requireKey(key);
        writeLock.lock();
        try {
            if (data.containsKey(key)) {
                append(String.join(SEPARATOR, DELETE, encode(key), END));
                data.remove(key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isWritable() {
        return Files.isWritable(dir);
    }

    @Override
    public void close() throws IOException {
        writeLock.lock();
        try (lockFile) {
            journal.close();
        } finally {
            writeLock.unlock();
        }
    }

    private void append(String record) throws IOException {
        long size = journal.size();
        try {
            writeFully(journal, record);
            journal.force(false);
        } catch (IOException e) {
            journal.truncate(size);
            throw e;
        }
    }

    private void replay(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                apply(line.split(SEPARATOR, -1));
            }
        }
    }

    private void apply(String... fields) {
        if (fields.length == UPSERT_FIELDS && UPSERT.equals(fields[0]) && fields[3].isEmpty()) {
            data.put(decode(fields[1]), decode(fields[2]));
        } else if (fields.length == DELETE_FIELDS && DELETE.equals(fields[0]) && fields[2].isEmpty()) {
            data.remove(decode(fields[1]));
        }
    }

    private void compact(Path file, Path tmp) throws IOException {
        try (FileChannel snapshot = FileChannel.open(tmp, StandardOpenOption.CREATE,
            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                writeFully(snapshot, upsertRecord(entry.getKey(), entry.getValue()));
            }
            snapshot.force(true);
        }
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel directory = FileChannel.open(dir, StandardOpenOption.READ)) {
            directory.force(true);
        } catch (AccessDeniedException expected) {
        }
    }

    private static void acquire(FileChannel lockFile, String fileName) throws IOException {
        try {
            if (lockFile.tryLock() != null) {
                return;
            }
        } catch (OverlappingFileLockException expected) {
        }
        throw new IOException(fileName + " is already used by another instance");
    }

    private static void writeFully(FileChannel channel, String text) throws IOException {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(text);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static String upsertRecord(String key, String value) {
        return String.join(SEPARATOR, UPSERT, encode(key), encode(value), END);
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private static String requireKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return key;
    }
}
