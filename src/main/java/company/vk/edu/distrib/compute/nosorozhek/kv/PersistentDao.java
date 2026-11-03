package company.vk.edu.distrib.compute.nosorozhek.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;

public class PersistentDao implements Dao<byte[]> {
    private final Path directory;

    PersistentDao(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    private Path fileFor(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        try {
            return Files.readAllBytes(fileFor(key));
        } catch (NoSuchFileException e) {
            throw new NoSuchElementException(e);
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }

        Path target = fileFor(key);
        Path temporary = Files.createTempFile(directory, "entry-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap(value.clone());
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                channel.force(true);
            }
            Files.move(temporary, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(fileFor(key));
    }

    @Override
    public void close() throws IOException {
        // skip this
    }
}
