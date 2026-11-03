package company.vk.edu.distrib.compute.masha533.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantLock;

public class PersistentByteDao implements Dao<byte[]> {
    private final Path file;
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private static final byte UPSERT = 0;
    private static final byte DELETE = 1;
    private final ReentrantLock lock = new ReentrantLock();

    private void appendOperation(byte op, String key, byte[] value) throws IOException {
        Path path = file.toAbsolutePath();
        Path tmp = Files.createTempFile(path.getParent(), "kv-", ".tmp");

        try {
            if (Files.exists(path)) {
                Files.copy(path, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            try (var output = new DataOutputStream(Files.newOutputStream(tmp, StandardOpenOption.APPEND))) {
                output.writeByte(op);
                output.writeUTF(key);
                if (op == UPSERT) {
                    output.writeInt(value.length);
                    output.write(value);
                }
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public PersistentByteDao(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) {
            try (var input = new DataInputStream(Files.newInputStream(file))) {
                while (true) {
                    try {
                        byte op = input.readByte();
                        String key = input.readUTF();

                        if (op == UPSERT) {
                            int len = input.readInt();
                            byte[] value = input.readNBytes(len);
                            if (value.length != len) {
                                break;
                            }
                            storage.put(key, value);
                        } else if (op == DELETE) {
                            storage.remove(key);
                        }
                    } catch (EOFException e) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException {
        byte[] value = storage.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }

        return value;
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        lock.lock();
        try {
            appendOperation(UPSERT, key, value);
            storage.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        lock.lock();
        try {
            appendOperation(DELETE, key, null);
            storage.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            storage.clear();
        } finally {
            lock.unlock();
        }
    }
}
