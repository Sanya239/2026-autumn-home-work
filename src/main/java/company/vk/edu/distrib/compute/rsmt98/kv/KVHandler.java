package company.vk.edu.distrib.compute.rsmt98.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

final class KVHandler implements HttpHandler {
    private static final String GET_METHOD = "GET";
    private static final String KEY_QUERY_PREFIX = "id=";
    private static final int MAX_VALUE_BYTES = 1024 * 1024;
    private static final byte[] EMPTY_BODY = new byte[0];

    private final FileByteDao dao;

    KVHandler(FileByteDao dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            switch (exchange.getRequestURI().getRawPath()) {
                case "/v0/status" -> sendResponse(exchange, status(exchange), EMPTY_BODY);
                case "/v0/entity" -> handleEntity(exchange);
                default -> sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, EMPTY_BODY);
            }
        }
    }

    private int status(HttpExchange exchange) {
        if (!GET_METHOD.equals(exchange.getRequestMethod())) {
            return methodNotAllowed(exchange, GET_METHOD);
        }
        return dao.isAvailable() ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_UNAVAILABLE;
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            int status = HttpURLConnection.HTTP_OK;
            byte[] body = EMPTY_BODY;
            try {
                switch (exchange.getRequestMethod()) {
                    case GET_METHOD ->
                            body = dao.get(readKey(exchange.getRequestURI().getRawQuery()));
                    case "PUT" -> status = putEntity(exchange, requestBody);
                    case "DELETE" -> {
                        dao.delete(readKey(exchange.getRequestURI().getRawQuery()));
                        status = HttpURLConnection.HTTP_ACCEPTED;
                    }
                    default -> status = methodNotAllowed(exchange, "GET, PUT, DELETE");
                }
            } catch (NoSuchElementException e) {
                status = HttpURLConnection.HTTP_NOT_FOUND;
            } catch (IllegalArgumentException e) {
                status = HttpURLConnection.HTTP_BAD_REQUEST;
            } catch (IOException e) {
                status = HttpURLConnection.HTTP_UNAVAILABLE;
            }
            sendResponse(exchange, status, body);
        }
    }

    private int putEntity(HttpExchange exchange, InputStream requestBody) throws IOException {
        String key = readKey(exchange.getRequestURI().getRawQuery());
        byte[] value = requestBody.readNBytes(MAX_VALUE_BYTES);
        if (value.length == MAX_VALUE_BYTES && requestBody.read() != -1) {
            return HttpURLConnection.HTTP_ENTITY_TOO_LARGE;
        }
        dao.upsert(key, value);
        return HttpURLConnection.HTTP_CREATED;
    }

    private static void sendResponse(HttpExchange exchange, int status, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        if (body.length == 0) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static String readKey(@Nullable String query) {
        if (query == null) {
            throw new IllegalArgumentException("Missing key");
        }
        int offset = 0;
        while (offset < query.length()) {
            int end = query.indexOf('&', offset);
            if (end == -1) {
                end = query.length();
            }
            if (query.startsWith(KEY_QUERY_PREFIX, offset)) {
                String key =
                        URLDecoder.decode(
                                query.substring(offset + KEY_QUERY_PREFIX.length(), end),
                                StandardCharsets.UTF_8);
                if (key.isEmpty()) {
                    throw new IllegalArgumentException("Key must be non-empty");
                }
                return key;
            }
            offset = end + 1;
        }
        throw new IllegalArgumentException("Missing key");
    }

    private static int methodNotAllowed(HttpExchange exchange, String methods) {
        exchange.getResponseHeaders().set("Allow", methods);
        return HttpURLConnection.HTTP_BAD_METHOD;
    }
}
