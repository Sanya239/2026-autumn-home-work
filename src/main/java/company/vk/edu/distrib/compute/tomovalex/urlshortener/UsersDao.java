package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class UsersDao implements Dao<String> {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    private static void validateUsername(String username) {
        if (username == null || username.isEmpty() || username.indexOf(':') >= 0) {
            throw new IllegalArgumentException("invalid username");
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException {
        validateUsername(key);
        String value = users.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }

        return value;
    }

    @Override
    public void upsert(String key, String value) {
        validateUsername(key);
        users.put(key, value);
    }

    @Override
    public void delete(String key) {
        validateUsername(key);
        users.remove(key);
    }

    @Override
    public void close() {
        //
    }
}
