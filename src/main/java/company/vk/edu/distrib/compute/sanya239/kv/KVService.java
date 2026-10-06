package company.vk.edu.distrib.compute.sanya239.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class KVService implements company.vk.edu.distrib.compute.kv.KVService {
    private static final String METHOD_GET = "GET";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";
    private static final String STATUS_PATH = "/v0/status";
    private static final String ENTITY_PATH = "/v0/entity";
    private static final String ID_PARAMETER = "id";

    private final int port;
    private final FileDao dao;
    private HttpServer server;

    public KVService(int port) throws IOException {
        this.port = port;
        Path file = Path.of(System.getProperty("java.io.tmpdir"), "sanya239-kv", port + ".db");
        dao = new FileDao(file.toString());
    }

    @Override
    public void start() {
        if (server != null) {
            throw new IllegalStateException("Service is already started");
        }
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", this::handle);
            server.start();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (STATUS_PATH.equals(path)) {
            respond(exchange, METHOD_GET.equals(method) ? 200 : 405, null);
            return;
        }
        if (!ENTITY_PATH.equals(path) && !path.startsWith(ENTITY_PATH + "/")) {
            respond(exchange, 404, null);
            return;
        }

        String key;
        try {
            key = extractKey(exchange, path);
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, null);
            return;
        }
        if (key.isEmpty()) {
            respond(exchange, 400, null);
            return;
        }

        serve(exchange, method, key);
    }

    private void serve(HttpExchange exchange, String method, String key) throws IOException {
        switch (method) {
            case METHOD_GET -> get(exchange, key);
            case METHOD_PUT -> {
                dao.upsert(key, exchange.getRequestBody().readAllBytes());
                respond(exchange, 201, null);
            }
            case METHOD_DELETE -> {
                dao.delete(key);
                respond(exchange, 202, null);
            }
            default -> respond(exchange, 405, null);
        }
    }

    private void get(HttpExchange exchange, String key) throws IOException {
        byte[] value = dao.get(key);
        if (value == null) {
            respond(exchange, 404, null);
        } else {
            respond(exchange, 200, value);
        }
    }

    private static String extractKey(HttpExchange exchange, String path) {
        if (path.startsWith(ENTITY_PATH + "/")) {
            return path.substring(ENTITY_PATH.length() + 1);
        }
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return "";
        }
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && ID_PARAMETER.equals(parameter.substring(0, separator))) {
                return URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
        if (body != null) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    @Override
    public void stop() {
        server.stop(0);
        try {
            dao.close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
