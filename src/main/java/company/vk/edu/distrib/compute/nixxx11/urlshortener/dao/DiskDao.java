package company.vk.edu.distrib.compute.nixxx11.urlshortener.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public class DiskDao implements Dao<String> {
  private final Path path;

  public DiskDao(final Path path) throws IOException {
    this.path = path;
    Files.createDirectories(path);
  }

  @Override
  public String get(final String key) throws IOException {
    try {
      return Files.readString(path.resolve(key), StandardCharsets.UTF_8);
    } catch (final NoSuchFileException e) {
      throw new NoSuchElementException(e);
    }
  }

  @Override
  public void upsert(final String key, final String value) throws IllegalArgumentException, IOException {
    Files.writeString(path.resolve(key), value, StandardCharsets.UTF_8);
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
