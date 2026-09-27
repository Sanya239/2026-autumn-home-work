package company.vk.edu.distrib.compute.sanya239.urlshortener;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class Dao implements company.vk.edu.distrib.compute.Dao<String> {

    private final RandomAccessFile file;
    private final Map<String, String> storage = new ConcurrentHashMap<>();

    public Dao(String path) throws IOException {
        var absolutePath = Path.of(path).toAbsolutePath();
        Files.createDirectories(absolutePath.getParent());
        file = new RandomAccessFile(absolutePath.toFile(), "rw");
        load();
    }

    private void load() throws IOException {
        file.seek(0);
        while (file.getFilePointer() < file.length()) {
            storage.put(file.readUTF(), file.readUTF());
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return storage.get(key);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        storage.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        storage.remove(key);
    }

    @Override
    public void close() throws IOException {
        save();
        file.close();
    }

    private void save() throws IOException {
        file.setLength(0);
        file.seek(0);
        for (Map.Entry<String, String> entry : storage.entrySet()) {
            file.writeUTF(entry.getKey());
            file.writeUTF(entry.getValue());
        }
    }
}
