package company.vk.edu.distrib.compute.ruavee.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;

class RemoteStringDao implements Dao<String> {
    private final HttpClient client;
    private final int port;

    public RemoteStringDao(int port) {
        this.client = HttpClient.newBuilder().build();
        this.port = port;
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        URI uri = URI.create("http://localhost:" + port + "/v0/entity?id=" + URLEncoder.encode(key, UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return switch (response.statusCode()) {
                case 200 -> new String(response.body(), UTF_8);
                case 404 -> throw new NoSuchElementException();
                default -> throw new IOException("Unexpected HTTP status: " + response.statusCode());
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        URI uri = URI.create("http://localhost:" + port + "/v0/entity?id=" + URLEncoder.encode(key, UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .PUT(HttpRequest.BodyPublishers.ofString(value, UTF_8)).build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != HttpURLConnection.HTTP_CREATED) {
                throw new IOException("Unexpected HTTP status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        URI uri = URI.create("http://localhost:" + port + "/v0/entity?id=" + URLEncoder.encode(key, UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).DELETE().build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != HttpURLConnection.HTTP_ACCEPTED) {
                throw new IOException("Unexpected HTTP status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
