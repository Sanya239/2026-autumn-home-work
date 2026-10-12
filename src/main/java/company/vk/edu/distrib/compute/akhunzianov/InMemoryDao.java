package company.vk.edu.distrib.compute.akhunzianov;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import company.vk.edu.distrib.compute.Dao;

public class InMemoryDao implements Dao<String> {

    private final Map<String, String> storage = new ConcurrentHashMap<>();

    @Override
    public String get(String key) throws NoSuchElementException {
        var value = storage.get(key);
        if (value == null) {
            throw new NoSuchElementException("No such key as '" + key + '\'');
        }
        return value;
    }

    @Override
    public void upsert(String key, String value) {
        storage.put(key, value);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }

    @Override
    public void close() {
        storage.clear();
    }
}
