package company.vk.edu.distrib.compute.mrglaster.kv.dao;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

public class EPRemoteDao implements Dao<String> {

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NOT_FOUND = 404;

    private final int port;
    private final HttpClient client;

    public EPRemoteDao(HttpClient client, int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("Port must be positive");
        }
        this.port = port;
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        requireKey(key);
        HttpResponse<byte[]> response = send(key, request(key).GET().build());
        return switch (response.statusCode()) {
            case HTTP_OK -> new String(response.body(), StandardCharsets.UTF_8);
            case HTTP_NOT_FOUND -> throw new NoSuchElementException(failed(key));
            default -> throw new IOException(unexpectedStatus(key, response.statusCode()));
        };
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        requireKey(key);
        Objects.requireNonNull(value, "value");
        HttpResponse<byte[]> response = send(
                key,
                request(key).PUT(BodyPublishers.ofString(value, StandardCharsets.UTF_8)).build()
        );
        int status = response.statusCode();
        if (status != HTTP_CREATED) {
            throw new IOException(unexpectedStatus(key, status));
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        requireKey(key);
        HttpResponse<byte[]> response = send(key, request(key).DELETE().build());
        int status = response.statusCode();
        if (status != HTTP_ACCEPTED) {
            throw new IOException(unexpectedStatus(key, status));
        }
    }

    private HttpResponse<byte[]> send(String key, HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(failed(key), e);
        }
    }

    private HttpRequest.Builder request(String key) {
        return HttpRequest.newBuilder(uri(key));
    }

    private URI uri(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, encodedKey));
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
    }

    private String failed(String key) {
        return "Failed to send request to %s".formatted(uri(key));
    }

    private String unexpectedStatus(String key, int status) {
        return "Unexpected status %d for %s".formatted(status, uri(key));
    }
}
