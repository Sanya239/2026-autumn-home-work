package company.vk.edu.distrib.compute.rsmt98.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public final class HttpStringDao implements Dao<String> {
    private final String entityPrefix;
    private final HttpClient client;

    public HttpStringDao(int port) {
        entityPrefix = "http://localhost:" + port + "/v0/entity?id=";
        client = HttpClient.newHttpClient();
    }

    @Override
    public String get(String key) throws IOException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder(buildRequestUri(key)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return switch (response.statusCode()) {
            case 200 -> response.body();
            case 404 -> throw new NoSuchElementException("No value exists for the key");
            default ->
                    throw new IOException(
                            "Unexpected KV response status: " + response.statusCode());
        };
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest request =
                HttpRequest.newBuilder(buildRequestUri(key))
                        .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                        .build();
        expectStatus(send(request, HttpResponse.BodyHandlers.discarding()), 201);
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(buildRequestUri(key)).DELETE().build();
        expectStatus(send(request, HttpResponse.BodyHandlers.discarding()), 202);
    }

    @Override
    public void close() {
        client.close();
    }

    private URI buildRequestUri(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return URI.create(entityPrefix + URLEncoder.encode(key, StandardCharsets.UTF_8));
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException {
        try {
            return client.send(request, bodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while accessing remote storage", e);
        } catch (IllegalStateException e) {
            throw new IOException("Remote HTTP client is closed", e);
        }
    }

    private static void expectStatus(HttpResponse<?> response, int expected) throws IOException {
        if (response.statusCode() != expected) {
            throw new IOException("Unexpected KV response status: " + response.statusCode());
        }
    }
}
