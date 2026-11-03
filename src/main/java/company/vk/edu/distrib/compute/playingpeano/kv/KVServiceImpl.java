package company.vk.edu.distrib.compute.playingpeano.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.kv.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.Nullable;

final class KVServiceImpl implements KVService {
    private static final String WORKERS_ENVIRONMENT_VARIABLE = "KV_WORKERS";
    private static final String STATUS_PATH = "/v0/status";
    private static final String ENTITY_PATH = "/v0/entity";
    private static final String GET_METHOD = "GET";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";
    private static final String ID_PARAMETER = "id";
    private static final char PARAMETER_SEPARATOR = '=';
    private static final int PARAMETER_SEPARATOR_NOT_FOUND = -1;
    private static final int STOP_DELAY_SECONDS = 1;

    private final PersistentByteArrayDao dao;
    private final HttpServer server;
    private final @Nullable ExecutorService executor;

    KVServiceImpl(int port, PersistentByteArrayDao dao) throws IOException {
        this.dao = dao;
        server = HttpServer.create(new InetSocketAddress(bindHost(), port), 0);
        server.createContext("/", this::handle);
        executor = createExecutor();
        if (executor != null) {
            server.setExecutor(executor);
        }
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        try (dao) {
            server.stop(STOP_DELAY_SECONDS);
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private static String bindHost() {
        @Nullable String configuredHost = System.getenv("KV_BIND");
        if (configuredHost == null || configuredHost.isBlank()) {
            return "localhost";
        }
        return configuredHost;
    }

    private static @Nullable ExecutorService createExecutor() {
        @Nullable String configuredWorkers = System.getenv(WORKERS_ENVIRONMENT_VARIABLE);
        if (configuredWorkers == null || configuredWorkers.isBlank()) {
            return null;
        }
        int workers = Integer.parseInt(configuredWorkers);
        if (workers <= 0) {
            throw new IllegalArgumentException("KV_WORKERS must be positive");
        }
        return Executors.newFixedThreadPool(workers);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (NoSuchElementException exception) {
                sendEmpty(exchange, 404);
            } catch (IllegalArgumentException exception) {
                sendEmpty(exchange, 400);
            } catch (IOException exception) {
                sendEmpty(exchange, 503);
            } catch (RuntimeException exception) {
                sendEmpty(exchange, 500);
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (STATUS_PATH.equals(path)) {
            handleStatus(exchange);
        } else if (ENTITY_PATH.equals(path)) {
            handleEntity(exchange);
        } else {
            sendEmpty(exchange, 404);
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!GET_METHOD.equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, GET_METHOD);
            return;
        }
        sendEmpty(exchange, dao.isAvailable() ? 200 : 503);
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        String key = extractKey(exchange);
        switch (exchange.getRequestMethod()) {
            case GET_METHOD -> sendValue(exchange, dao.get(key));
            case PUT_METHOD -> {
                dao.upsert(key, exchange.getRequestBody().readAllBytes());
                sendEmpty(exchange, 201);
            }
            case DELETE_METHOD -> {
                dao.delete(key);
                sendEmpty(exchange, 202);
            }
            default -> sendMethodNotAllowed(exchange, "GET, PUT, DELETE");
        }
    }

    private static String extractKey(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null) {
            throw new IllegalArgumentException("Missing id");
        }
        for (String parameter : rawQuery.split("&")) {
            int separator = parameter.indexOf(PARAMETER_SEPARATOR);
            if (separator != PARAMETER_SEPARATOR_NOT_FOUND
                && ID_PARAMETER.equals(parameter.substring(0, separator))) {
                String key = URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
                if (!key.isEmpty()) {
                    return key;
                }
                break;
            }
        }
        throw new IllegalArgumentException("Missing id");
    }

    private static void sendValue(HttpExchange exchange, byte[] value) throws IOException {
        exchange.sendResponseHeaders(200, value.length == 0 ? -1 : value.length);
        if (value.length > 0) {
            exchange.getResponseBody().write(value);
        }
    }

    private static void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    private static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendEmpty(exchange, 405);
    }
}
