package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import company.vk.edu.distrib.compute.Dao;

public final class Daos<T> implements Closeable {

    private final DaoFactory<T> factory;
    private final Map<String, Dao<T>> created = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Daos(DaoFactory<T> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public Dao<T> getOrCreate(String key) {
        return created.computeIfAbsent(key, this::tryCreate);
    }

    public Dao<T> put(String key, Dao<T> dao) {
        return created.put(key, dao);
    }

    private Dao<T> tryCreate(String key) {
        try {
            return factory.createDao(key);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start DAO at %s".formatted(key), e);
        }
    }

    @Override
    public void close() {
        created.values().forEach(this::closeDao);
    }

    private void closeDao(Dao<?> dao) {
        try {
            dao.close();
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
