package company.vk.edu.distrib.compute.annapiatkova.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DaoImpl implements Dao<String> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, String> values = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        return values.get(key);
    }

    @Override
    public void upsert(String key, String value) {
        lock.lock();
        try {
            values.put(key, value);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void delete(String key) {
        lock.lock();
        try {
            values.remove(key);
        } finally {
            lock.unlock();
        }        
    }

    @Override
    public void close() {
        /* This DAO is not persistent, it doesn't save any data on close() */
    }
}
