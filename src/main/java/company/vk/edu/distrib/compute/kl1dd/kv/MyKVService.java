package company.vk.edu.distrib.compute.kl1dd.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.kv.KVService;

import java.io.IOException;
import java.util.NoSuchElementException;

public class MyKVService implements KVService {
    private static final String GET_METHOD = "GET";
    private static final String ID_QUERY_PREFIX = "id=";
    private static final int STOP_DELAY_SECONDS = 2;
    private static final int MAX_REQUEST_BODY_SIZE = 5 * 1024 * 1024;

    private final HttpServer httpServer;
    private final MyKVDao dao;

    public MyKVService(HttpServer httpServer) throws IOException {
        this.httpServer = httpServer;
        this.dao = new MyKVDao();

        httpServer.createContext("/v0/status", this::handleStatus);
        httpServer.createContext("/v0/entity", this::handleEntity);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!GET_METHOD.equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET" -> {
                handleGet(exchange);
            }
            case "PUT" -> {
                handlePut(exchange);
            }
            case "DELETE" -> {
                handleDelete(exchange);
            }
            default -> {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String id = getId(exchange);
        if (id == null) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        try {
            dao.delete(id);

            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String id = getId(exchange);
        if (id == null) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        byte[] value;
        try {
            value = exchange.getRequestBody().readNBytes(MAX_REQUEST_BODY_SIZE + 1);
            if (value.length > MAX_REQUEST_BODY_SIZE) {
                exchange.sendResponseHeaders(413, -1);
                exchange.close();
                return;
            }
        } catch (IOException e) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }

        try {
            dao.upsert(id, value);

            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String id = getId(exchange);
        if (id == null) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        try {
            byte[] value = dao.get(id);

            exchange.sendResponseHeaders(200, value.length);
            exchange.getResponseBody().write(value);
            exchange.close();
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        }
    }

    private String getId(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        if (!query.startsWith(ID_QUERY_PREFIX)) {
            return null;
        }

        String id = query.substring(ID_QUERY_PREFIX.length());
        if (id.isEmpty()) {
            return null;
        }

        return id;
    }

    @Override
    public void start() {
        httpServer.start();
    }

    @Override
    public void stop() {
        httpServer.stop(STOP_DELAY_SECONDS);
    }
}
