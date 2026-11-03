package company.vk.edu.distrib.compute.nixxx11.kv.dao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

import static java.net.HttpURLConnection.*;

public class RemoteDao implements Dao<String> {
  private final HttpClient client;
  private final URI baseUri;

  public RemoteDao(final URI baseUri) {
    this.baseUri = baseUri;
    this.client = HttpClient.newHttpClient();
  }

  @Override
  public String get(final String key) throws NoSuchElementException, IllegalArgumentException, IOException {
    final HttpRequest request = HttpRequest.newBuilder(uriForKey(key))
        .GET()
        .build();
    final HttpResponse<String> response = send(request);
    if (response.statusCode() == HTTP_NOT_FOUND) {
      throw new NoSuchElementException();
    }
    if (response.statusCode() != HTTP_OK) {
      throw unexpectedCode(response);
    }
    return response.body();
  }

  @Override
  public void upsert(final String key, final String value) throws IllegalArgumentException, IOException {
    final HttpRequest request = HttpRequest.newBuilder(uriForKey(key))
        .PUT(HttpRequest.BodyPublishers.ofString(value))
        .build();
    final HttpResponse<String> response = send(request);
    if (response.statusCode() != HTTP_CREATED) {
      throw unexpectedCode(response);
    }
  }

  @Override
  public void delete(final String key) throws IllegalArgumentException, IOException {
    final HttpRequest request = HttpRequest.newBuilder(uriForKey(key))
        .DELETE()
        .build();
    final HttpResponse<String> response = send(request);
    if (response.statusCode() != HTTP_ACCEPTED) {
      throw unexpectedCode(response);
    }
  }

  private URI uriForKey(final String key) {
    return URI.create(baseUri + "?id=" + key);
  }

  private HttpResponse<String> send(final HttpRequest request) throws IOException {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (final InterruptedException e) {
      throw new IOException("Interrupted", e);
    }
  }

  private static IOException unexpectedCode(final HttpResponse<String> response) {
    final int statusCode = response.statusCode();
    if (statusCode >= HTTP_INTERNAL_ERROR) {
      return new IOException("Got server error: " + statusCode);
    }
    return new IOException("Got unexpected status code: " + statusCode);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
