package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public record DecodingDao(Dao<byte[]> delegate) implements Dao<String> {

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return new String(delegate.get(key), StandardCharsets.UTF_8);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        delegate.upsert(key, value.getBytes());
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        delegate.delete(key);
    }

}
