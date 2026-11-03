package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Objects;

import company.vk.edu.distrib.compute.Dao;

public final class PersistentDao implements Dao<String> {
    private final Path directory;

    public PersistentDao(Path directory) throws IOException {
        this.directory = Objects.requireNonNull(directory).toAbsolutePath();
        Files.createDirectories(this.directory);
    }

    @Override
    public String get(String key) throws IOException {
        try {
            return Files.readString(pathForKey(key), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new NoSuchElementException("Key not found: " + key, e);
        }
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        Path target = pathForKey(key);
        Objects.requireNonNull(value);
        Path temporary = Files.createTempFile(directory, "update-", ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(pathForKey(key));
    }

    private Path pathForKey(String key) {
        Objects.requireNonNull(key);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + ".data");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public void close() {
        // каждая запись в файл сразу его закрывает
    }
}
