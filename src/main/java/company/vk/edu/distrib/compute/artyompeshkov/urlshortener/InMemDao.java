package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import company.vk.edu.distrib.compute.Dao;

public class InMemDao implements Dao<String> {
    private final Map<String, String> data = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        String value = data.get(key);
        if (value == null) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        return value;
    }

    @Override
    public void upsert(String key, String value) {
        data.put(key, value);
    }

    @Override
    public void delete(String key) {
        data.remove(key);
    }

    @Override
    public void close() {
        data.clear();
    }
}
