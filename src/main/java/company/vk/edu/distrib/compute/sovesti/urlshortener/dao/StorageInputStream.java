package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

final class StorageInputStream implements AutoCloseable {

    private final DataInputStream inner;

    StorageInputStream(InputStream inner) {
        this.inner = new DataInputStream(Objects.requireNonNull(inner));
    }

    Optional<byte[]> read() throws IOException {
        int size = inner.readInt();
        if (size < 0) {
            return Optional.empty();
        }
        return Optional.of(inner.readNBytes(size));
    }

    Optional<String> readString() throws IOException {
        try {
            return read().map(bytes -> new String(bytes, StandardCharsets.UTF_8));
        } catch (EOFException e) {
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }
}
