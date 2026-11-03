package company.vk.edu.distrib.compute.ddkudrin.kv;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public final class DDKudrinRemoteDao implements Dao<String> {
    private static final int HTTP_NOT_FOUND = 404;

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private final HttpClient client;
    private final String entityUrl;

    public DDKudrinRemoteDao(int port) {
        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("Port out of range");
        }
        this.entityUrl = "http://localhost:" + port + "/v0/entity?id=";
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String get(String key) throws IOException {
        HttpResponse<byte[]> response = send(request(key).GET().build());
        if (response.statusCode() == HTTP_NOT_FOUND) {
            throw new NoSuchElementException("Unknown key: " + key);
        }
        checkStatus(response, 200);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest request = request(key)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                .build();
        checkStatus(send(request), 201);
    }

    @Override
    public void delete(String key) throws IOException {
        checkStatus(send(request(key).DELETE().build()), 202);
    }

    @Override
    public void close() {
        client.close();
    }

    private HttpRequest.Builder request(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        URI uri = URI.create(entityUrl + URLEncoder.encode(key, StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(uri).timeout(TIMEOUT);
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("KV request interrupted", e);
        }
    }

    private static void checkStatus(HttpResponse<?> response, int expected) throws IOException {
        if (response.statusCode() != expected) {
            throw new IOException("Unexpected KV response: " + response.statusCode());
        }
    }
}
