package company.vk.edu.distrib.compute.kl1dd.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

public class MyRemoteDao implements Dao<String> {
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NOT_FOUND = 404;

    private static final String LOCALHOST_URL = "http://localhost:";
    private static final String ENTITY_PATH = "/v0/entity?id=";
    private static final String UNEXPECTED_STATUS_CODE = "Unexpected status code: ";

    private final int port;
    private final HttpClient httpClient;

    public MyRemoteDao(int port) {
        this.port = port;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        URI uri = URI.create(LOCALHOST_URL + port + ENTITY_PATH + key);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == HTTP_OK) {
                return response.body();
            }

            if (statusCode == HTTP_NOT_FOUND) {
                throw new NoSuchElementException();
            }

            throw new IOException(UNEXPECTED_STATUS_CODE + statusCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        URI uri = URI.create(LOCALHOST_URL + port + ENTITY_PATH + key);
        HttpRequest request = HttpRequest.newBuilder(uri).PUT(HttpRequest.BodyPublishers.ofString(value)).build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == HTTP_CREATED) {
                return;
            }

            throw new IOException(UNEXPECTED_STATUS_CODE + statusCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        URI uri = URI.create(LOCALHOST_URL + port + ENTITY_PATH + key);
        HttpRequest request = HttpRequest.newBuilder(uri).DELETE().build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == HTTP_ACCEPTED) {
                return;
            }

            throw new IOException(UNEXPECTED_STATUS_CODE + statusCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // later
    }
}
