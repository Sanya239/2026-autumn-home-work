package company.vk.edu.distrib.compute.artyompeshkov.kv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

class RemoteDao implements Dao<String> {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String recordUrl;

    RemoteDao(int port) {
        this.recordUrl = "http://localhost:" + port + KVServiceImpl.ENTRY_STR + "?" + KVServiceImpl.ID_STR;
    }

    @Override
    public String get(String key) throws IOException {
        HttpResponse<byte[]> response = sendRequest(createRequest(key).GET());
        checkStatus(response, HttpURLConnection.HTTP_OK);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest.Builder request = createRequest(key).PUT(HttpRequest.BodyPublishers.ofString(value));
        checkStatus(sendRequest(request), HttpURLConnection.HTTP_CREATED);
    }

    @Override
    public void delete(String key) throws IOException {
        checkStatus(sendRequest(createRequest(key).DELETE()), HttpURLConnection.HTTP_ACCEPTED);
    }

    @Override
    public void close() {
        client.close();
    }

    private HttpRequest.Builder createRequest(String key) {
        return HttpRequest.newBuilder(URI.create(recordUrl + URLEncoder.encode(key, StandardCharsets.UTF_8)));
    }

    private HttpResponse<byte[]> sendRequest(HttpRequest.Builder request) throws IOException {
        try {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the KV service", e);
        }
    }

    private static void checkStatus(HttpResponse<byte[]> response, int expected) throws IOException {
        int code = response.statusCode();
        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NoSuchElementException("Key not found");
        }
        if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new IllegalArgumentException("Bad key");
        }
        if (code != expected) {
            throw new IOException("Unexpected status from the KV service: " + code);
        }
    }
}
