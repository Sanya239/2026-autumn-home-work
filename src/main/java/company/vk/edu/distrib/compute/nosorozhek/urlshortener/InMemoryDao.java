package company.vk.edu.distrib.compute.nosorozhek.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDao<T> implements Dao<T> {
    private final Map<String, T> entries = new ConcurrentHashMap<>();

    @Override
    public T get(String key) throws NoSuchElementException, IllegalArgumentException {
        T value = entries.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void upsert(String key, T value) throws IllegalArgumentException {
        entries.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException {
        entries.remove(key);
    }

    @Override
    public void close() {
        // skip
    }
}
