package company.vk.edu.distrib.compute.nixxx11.kv.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public class DiskDao implements Dao<byte[]> {
  private final Path path;

  public DiskDao(final Path path) throws IOException {
    this.path = path;
    Files.createDirectories(path);
  }

  @Override
  public byte[] get(final String key) throws IOException {
    try {
      return Files.readAllBytes(path.resolve(key));
    } catch (final NoSuchFileException e) {
      throw new NoSuchElementException(e);
    }
  }

  @Override
  public void upsert(final String key, final byte[] value) throws IllegalArgumentException, IOException {
    Files.write(path.resolve(key), value);
  }

  @Override
  public void delete(final String key) throws IllegalArgumentException, IOException {
    Files.deleteIfExists(path.resolve(key));
  }

  @Override
  public void close() {
    //noop
  }
}
