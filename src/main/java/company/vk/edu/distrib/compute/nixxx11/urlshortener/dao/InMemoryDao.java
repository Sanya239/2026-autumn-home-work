package company.vk.edu.distrib.compute.nixxx11.urlshortener.dao;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import company.vk.edu.distrib.compute.Dao;

public class InMemoryDao<T> implements Dao<T> {
  private final ConcurrentMap<String, T> map = new ConcurrentHashMap<>();

  @Override
  public T get(final String key) {
    final T value = map.get(key);
    if (value == null) {
      throw new NoSuchElementException();
    }
    return value;
  }

  @Override
  public void upsert(final String key, final T value) {
    map.put(key, value);
  }

  @Override
  public void delete(final String key) {
    map.remove(key);
  }

  @Override
  public void close() {
    //noop
  }
}
