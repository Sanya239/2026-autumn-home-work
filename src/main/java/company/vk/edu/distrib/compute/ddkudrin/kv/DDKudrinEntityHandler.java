package company.vk.edu.distrib.compute.ddkudrin.kv;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import company.vk.edu.distrib.compute.Dao;

public class DDKudrinEntityHandler implements HttpHandler {
    private static final String ENTITY_PATH = "/v0/entity";
    private final Dao<byte[]> dao;

    public DDKudrinEntityHandler(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(400, -1);
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(404, -1);
            } catch (IOException e) {
                if (exchange.getResponseCode() != -1) {
                    throw e;
                }
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        if (!Objects.equals(exchange.getRequestURI().getPath(), ENTITY_PATH)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String key = readKey(exchange.getRequestURI().getRawQuery());
        if (Objects.equals(exchange.getRequestMethod(), "GET")) {
            byte[] value = dao.get(key);
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, value.length == 0 ? -1 : value.length);
            if (value.length != 0) {
                exchange.getResponseBody().write(value);
            }
        } else if (Objects.equals(exchange.getRequestMethod(), "PUT")) {
            dao.upsert(key, exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(201, -1);
        } else if (Objects.equals(exchange.getRequestMethod(), "DELETE")) {
            dao.delete(key);
            exchange.sendResponseHeaders(202, -1);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private static String readKey(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Missing query");
        }
        String key = null;
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (Objects.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), "id")) {
                if (key != null || parts.length != 2) {
                    throw new IllegalArgumentException("Invalid id parameter");
                }
                key = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return key;
    }
}
