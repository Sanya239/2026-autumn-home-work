package company.vk.edu.distrib.compute.kl1dd.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class MyDao implements Dao<String> {
    private final Map<String, String> myHashMap;

    public MyDao() {
        this.myHashMap = new ConcurrentHashMap<>();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        String result = myHashMap.get(key);
        if (result == null) {
            throw new NoSuchElementException();
        } else {
            return result;
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        myHashMap.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        myHashMap.remove(key);
    }

    @Override
    public void close() throws IOException {
        myHashMap.clear();
    }
}
