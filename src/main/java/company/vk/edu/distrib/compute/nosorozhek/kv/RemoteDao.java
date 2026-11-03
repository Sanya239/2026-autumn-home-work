package company.vk.edu.distrib.compute.nosorozhek.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.Function;

public final class RemoteDao<T> implements Dao<T> {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

    private final HttpClient client = HttpClient.newHttpClient();
    private final String host;
    private final int port;
    private final Function<T, byte[]> encode;
    private final Function<byte[], T> decode;

    public RemoteDao(String host, int port, Function<T, byte[]> encode, Function<byte[], T> decode) {
        this.host = host;
        this.port = port;
        this.encode = encode;
        this.decode = decode;
    }

    private static void expect(HttpResponse<?> response, int expected) throws IOException {
        int status = response.statusCode();
        if (status == expected) {
            return;
        }
        if (status == HTTP_BAD_REQUEST || status == HTTP_UNPROCESSABLE_ENTITY) {
            throw new IllegalArgumentException("KV service returned status code " + status);
        }
        throw new IOException("KV service returned sattus code " + status);
    }

    private URI entityUri(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException();
        }
        String id = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create(host + ":" + port + "/v0/entity?id=" + id);
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling KV service", e);
        }
    }

    @Override
    public T get(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(entityUri(key)).GET().build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() == HTTP_NOT_FOUND) {
            throw new NoSuchElementException("No entry for key: " + key);
        }
        expect(response, 200);
        return decode.apply(response.body());
    }

    @Override
    public void upsert(String key, T value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        HttpRequest request = HttpRequest.newBuilder(entityUri(key))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(encode.apply(value)))
                .build();
        expect(send(request), 201);
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(entityUri(key)).DELETE().build();
        expect(send(request), 202);
    }

    @Override
    public void close() {
        client.close();
    }
}
