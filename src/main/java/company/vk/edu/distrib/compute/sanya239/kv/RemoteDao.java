package company.vk.edu.distrib.compute.sanya239.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class RemoteDao implements Dao<String> {
    private static final String ENTITY_PATH = "/v0/entity?id=";
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_NOT_FOUND = 404;

    private final int port;
    private final HttpClient client = HttpClient.newHttpClient();

    public RemoteDao(int port) {
        this.port = port;
    }

    @Override
    public String get(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(endpoint(key)).GET().build();
        HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == STATUS_OK) {
            return new String(response.body(), StandardCharsets.UTF_8);
        }
        if (response.statusCode() == STATUS_NOT_FOUND) {
            throw new NoSuchElementException(key);
        }
        throw responseException(response.statusCode());
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(endpoint(key))
                .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                .build();
        int status = send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status != STATUS_CREATED) {
            throw responseException(status);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(endpoint(key)).DELETE().build();
        int status = send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status != STATUS_ACCEPTED) {
            throw responseException(status);
        }
    }

    private URI endpoint(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create("http://localhost:" + port + ENTITY_PATH + encodedKey);
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return client.send(request, handler);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(exception);
        }
    }

    private static IOException responseException(int status) {
        return new IOException("KV service responded with status " + status);
    }

    @Override
    public void close() {
        client.close();
    }
}
