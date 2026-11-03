package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class LinksDao implements Dao<String> {
    private final Map<String, String> links = new ConcurrentHashMap<>();

    private static void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9]{10}")) {
            throw new IllegalArgumentException("invalid key: " + key);
        }
    }

    @Override
    public String get(String key) {
        validateKey(key);

        String value = links.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }

        return value;
    }

    @Override
    public void upsert(String key, String value) {
        validateKey(key);
        links.put(key, value);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        links.remove(key);
    }

    @Override
    public void close() throws IOException {
        //
    }
}
