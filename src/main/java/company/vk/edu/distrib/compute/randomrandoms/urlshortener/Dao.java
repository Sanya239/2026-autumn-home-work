package company.vk.edu.distrib.compute.randomrandoms.urlshortener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Dao<T> implements company.vk.edu.distrib.compute.Dao<T> {
    private final Map<String, T> map;

    public Dao() {
        map = new HashMap<>();
    }

    @Override
    public T get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            throw new NoSuchElementException();
        }
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
    public void close() throws IOException {
        // skibidi dop dop yes yes
    }
}
