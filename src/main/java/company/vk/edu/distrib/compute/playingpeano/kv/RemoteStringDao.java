package company.vk.edu.distrib.compute.playingpeano.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;

final class RemoteStringDao implements Dao<String> {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_NOT_FOUND = 404;

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
    private final String entityUrl;

    RemoteStringDao(int port) {
        entityUrl = "http://localhost:" + port + "/v0/entity?id=";
    }

    @Override
    public String get(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uriFor(key))
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .build();
        HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == STATUS_OK) {
            return new String(response.body(), StandardCharsets.UTF_8);
        }
        if (response.statusCode() == STATUS_NOT_FOUND) {
            throw new NoSuchElementException("No value for key: " + key);
        }
        throw unexpectedStatus(response.statusCode());
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uriFor(key))
            .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
            .timeout(REQUEST_TIMEOUT)
            .build();
        HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != STATUS_CREATED) {
            throw unexpectedStatus(response.statusCode());
        }
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uriFor(key))
            .DELETE()
            .timeout(REQUEST_TIMEOUT)
            .build();
        HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != STATUS_ACCEPTED) {
            throw unexpectedStatus(response.statusCode());
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private URI uriFor(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return URI.create(entityUrl + URLEncoder.encode(key, StandardCharsets.UTF_8));
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return client.send(request, handler);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling KV service", exception);
        }
    }

    private static IOException unexpectedStatus(int status) {
        return new IOException("Unexpected KV service response: " + status);
    }
}
