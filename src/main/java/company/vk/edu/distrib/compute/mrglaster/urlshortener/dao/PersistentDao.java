package company.vk.edu.distrib.compute.mrglaster.urlshortener.dao;

import company.vk.edu.distrib.compute.Dao;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class PersistentDao<T> implements Dao<T> {
    private final Map<String, T> storage;

    private final Set<String> modifiedKeys;
    private final Set<String> removedKeys;

    private final ReentrantLock lock;
    private final RandomAccessFile log;
    private final Serializer<T> serializer;

    public interface Serializer<T> {
        byte[] serialize(T value);

        T deserialize(byte[] data);
    }

    public static Serializer<String> stringSerializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(String value) {
                return value.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String deserialize(byte[] data) {
                return new String(data, StandardCharsets.UTF_8);
            }
        };
    }

    public static Serializer<byte[]> byteArraySerializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(byte[] value) {
                return value;
            }

            @Override
            public byte[] deserialize(byte[] data) {
                return data;
            }
        };
    }

    public static Serializer<ByteBuffer> byteBufferSerializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(ByteBuffer value) {
                ByteBuffer dup = value.duplicate();
                byte[] out = new byte[dup.remaining()];
                dup.get(out);
                return out;
            }

            @Override
            public ByteBuffer deserialize(byte[] data) {
                return ByteBuffer.wrap(data);
            }
        };
    }

    public PersistentDao(String filePath, Serializer<T> serializer) throws IOException {
        this.serializer = serializer;
        this.log = new RandomAccessFile(filePath, "rw");
        this.storage = new ConcurrentHashMap<>();
        this.modifiedKeys = ConcurrentHashMap.newKeySet();
        this.removedKeys = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantLock();
        loadFromFile();
    }

    public static PersistentDao<String> forStrings(String filePath) throws IOException {
        return new PersistentDao<>(filePath, stringSerializer());
    }

    private boolean readRecord() throws IOException {
        if (log.getFilePointer() >= log.length()) {
            return false;
        }

        String operation = log.readUTF();
        String key = log.readUTF();

        if ("PUT".equals(operation)) {
            int length = log.readInt();
            byte[] data = new byte[length];
            log.readFully(data);
            storage.put(key, serializer.deserialize(data));
        } else if ("DELETE".equals(operation)) {
            storage.remove(key);
        }

        return true;
    }

    private void loadFromFile() throws IOException {
        log.seek(0);
        while (log.getFilePointer() < log.length()) {
            try {
                if (!readRecord()) {
                    break;
                }
            } catch (EOFException e) {
                break;
            }
        }
    }

    @Override
    public T get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        T value = storage.get(key);
        if (value == null) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        return value;
    }

    @Override
    public void upsert(String key, T value) throws IllegalArgumentException, IOException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
        lock.lock();
        try {
            storage.put(key, value);
            modifiedKeys.add(key);
            removedKeys.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        lock.lock();
        try {
            storage.remove(key);
            removedKeys.add(key);
            modifiedKeys.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public void save() throws IOException {
        lock.lock();
        try {
            log.seek(log.length());
            byte[] data;
            for (String key : modifiedKeys) {
                T value = storage.get(key);
                if (value == null) {
                    continue;
                }
                log.writeUTF("PUT");
                log.writeUTF(key);
                data = serializer.serialize(value);
                log.writeInt(data.length);
                log.write(data);
            }
            for (String key : removedKeys) {
                log.writeUTF("DELETE");
                log.writeUTF(key);
            }
            modifiedKeys.clear();
            removedKeys.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        save();
        lock.lock();
        try {
            if (log != null) {
                log.close();
            }
        } finally {
            lock.unlock();
        }
    }
}
