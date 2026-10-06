package company.vk.edu.distrib.compute.sanya239.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileDao implements Dao<byte[]> {
    private final RandomAccessFile file;
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    public FileDao(String path) throws IOException {
        Path absolutePath = Path.of(path).toAbsolutePath();
        Files.createDirectories(absolutePath.getParent());
        file = new RandomAccessFile(absolutePath.toFile(), "rw");
        load();
    }

    private void load() throws IOException {
        file.seek(0);
        while (file.getFilePointer() < file.length()) {
            String key = file.readUTF();
            int size = file.readInt();
            var value = createValue(size);
            file.readFully(value);
            storage.put(key, value);
        }
    }

    private byte[] createValue(int size) {
        return new byte[size];
    }

    @Override
    public byte[] get(String key) {
        return storage.get(key);
    }

    @Override
    public void upsert(String key, byte[] value) {
        storage.put(key, value);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }

    @Override
    public void close() throws IOException {
        file.setLength(0);
        file.seek(0);
        for (Map.Entry<String, byte[]> entry : storage.entrySet()) {
            file.writeUTF(entry.getKey());
            file.writeInt(entry.getValue().length);
            file.write(entry.getValue());
        }
        file.close();
    }
}
