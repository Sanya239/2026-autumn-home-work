package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

final class StorageOutputStream implements AutoCloseable {

    private final DataOutputStream inner;

    StorageOutputStream(OutputStream inner) {
        this.inner = new DataOutputStream(Objects.requireNonNull(inner));
    }

    void write(byte[] bytes) throws IOException {
        inner.writeInt(bytes.length);
        inner.write(bytes);
    }

    void write(String text) throws IOException {
        write(text.getBytes());
    }

    void writeNiche() throws IOException {
        inner.writeInt(-1);
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }
}
