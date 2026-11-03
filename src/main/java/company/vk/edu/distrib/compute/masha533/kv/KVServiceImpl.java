package company.vk.edu.distrib.compute.masha533.kv;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KVServiceImpl implements KVService {
    private final HttpServer server;
    private final Dao<byte[]> dao;
    private static final String GET_METHOD = "GET";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";
    private static final int PARTS_COUNT = 2;
    private static final String ID_PARAMETER = "id";
    private final ExecutorService executor;
    private static final int MAX_BODY_SIZE = 1024 * 1024;

    private String extractId(String query) {
        if (query == null) {
            throw new IllegalArgumentException();
        }
        var params = query.split("&");
        for (String param : params) {
            var parts = param.split("=", PARTS_COUNT);
            if (parts.length == PARTS_COUNT && ID_PARAMETER.equals(parts[0])) {
                String id = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                if (id.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                return id;
            }
        }
        throw new IllegalArgumentException();
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        final var method = exchange.getRequestMethod();
        final String id;
        try {
            id = extractId(exchange.getRequestURI().getRawQuery());
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }
        if (GET_METHOD.equals(method)) {
            try {
                byte[] value = dao.get(id);
                exchange.sendResponseHeaders(200, value.length);
                exchange.getResponseBody().write(value);
                exchange.close();
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        } else if (PUT_METHOD.equals(method)) {
            byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);
            if (body.length > MAX_BODY_SIZE) {
                exchange.sendResponseHeaders(413, -1);
                exchange.close();
                return;
            }
            dao.upsert(id, body);
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        } else if (DELETE_METHOD.equals(method)) {
            dao.delete(id);
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        }
    }

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        this.dao = dao;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v0/status", this::handleStatus);
        server.createContext("/v0/entity", this::handleEntity);
        executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        server.setExecutor(executor);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(5);
        executor.shutdown();
        try {
            dao.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
