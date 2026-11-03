package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import company.vk.edu.distrib.compute.Dao;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InMemoryDao<T> implements Dao<T> {
    private final ConcurrentMap<String, T> map = new ConcurrentHashMap<>();

    @Override
    public T get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        T value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void upsert(String key, T value) throws IllegalArgumentException, IOException {
        map.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        map.remove(key);
    }

    @Override
    public void close() {
        map.clear();
    }
}
