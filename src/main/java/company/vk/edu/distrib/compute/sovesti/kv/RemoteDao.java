package company.vk.edu.distrib.compute.sovesti.kv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;
import java.util.Objects;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.sovesti.urlshortener.auth.AuthenticationException;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.BadRequestException;
import company.vk.edu.distrib.compute.sovesti.urlshortener.http.StatusCodeConstants;

record RemoteDao(int port, HttpClient client) implements Dao<byte[]> {

    RemoteDao {
        Objects.requireNonNull(port);
        Objects.requireNonNull(client);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return send(key, request(key).GET().build());
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        send(key, request(key).PUT(BodyPublishers.ofByteArray(value)).build());
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        send(key, request(key).DELETE().build());
    }

    private byte[] send(String key, HttpRequest request) throws IOException {
        try {
            return checkCode(key, client.send(request, HttpResponse.BodyHandlers.ofByteArray()));
        } catch (InterruptedException e) {
            throw new IOException(failed(key), e);
        }
    }

    private byte[] checkCode(String key, HttpResponse<byte[]> response) throws IOException {
        return switch (response.statusCode()) {
            case StatusCodeConstants.OK -> response.body();
            case StatusCodeConstants.CREATED -> response.body();
            case StatusCodeConstants.ACCEPTED -> response.body();
            case StatusCodeConstants.NOT_FOUND -> throw new NoSuchElementException(failed(key));
            case StatusCodeConstants.BAD_REQUEST -> throw new BadRequestException(failed(key));
            case StatusCodeConstants.UNPROCESSABLE_CONTENT -> throw new IllegalArgumentException(failed(key));
            case StatusCodeConstants.UNATHORIZED -> throw new AuthenticationException();
            default -> throw new IOException(failed(key));
        };
    }

    private String failed(String key) {
        return "Failed to send request to %s".formatted(uri(key));
    }

    private HttpRequest.Builder request(String key) {
        return HttpRequest.newBuilder(uri(key));
    }

    private URI uri(String key) {
        return URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, key));
    }

}
