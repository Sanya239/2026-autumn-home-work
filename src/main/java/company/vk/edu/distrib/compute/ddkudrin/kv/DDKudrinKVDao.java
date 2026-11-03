package company.vk.edu.distrib.compute.ddkudrin.kv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.ddkudrin.urlshortener.DDKudrinPersistentDao;

public final class DDKudrinKVDao implements Dao<byte[]> {
    private final Dao<String> dao;

    public DDKudrinKVDao(Path file) throws IOException {
        this.dao = new DDKudrinPersistentDao(file);
    }

    @Override
    public byte[] get(String key) throws IOException {
        return Base64.getDecoder().decode(dao.get(encodeKey(key)));
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        dao.upsert(encodeKey(key), Base64.getEncoder().encodeToString(value));
    }

    @Override
    public void delete(String key) throws IOException {
        dao.delete(encodeKey(key));
    }

    @Override
    public void close() throws IOException {
        dao.close();
    }

    private static String encodeKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
}
