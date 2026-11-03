package company.vk.edu.distrib.compute.nixxx11.urlshortener.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;

public interface Response extends HttpHandler {
  int status();

  record Empty(int status) implements Response {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.sendResponseHeaders(status, 0);
    }
  }

  record Basic(int status, String body) implements Response {
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
      final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("content-type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    }
  }

  record Redirect(int status, String url) implements Response {
    public Redirect(String url) {
      this(HTTP_MOVED_PERM, url);
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().add("location", url);
      exchange.sendResponseHeaders(status(), 0);
    }
  }
}
