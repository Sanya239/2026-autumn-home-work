package company.vk.edu.distrib.compute.artyompeshkov.kv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.kv.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVServiceImpl implements KVService {
    static final String ENTRY_STR = "/v0/entity";
    static final String ID_STR = "id=";
    private static final int MAX_VALUE_SIZE = 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(KVServiceImpl.class);

    private final BitcaskDao dao;
    private final HttpServer server;

    public KVServiceImpl(int port, BitcaskDao dao) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;
        server.createContext("/v0/status", this::handleStatus);
        server.createContext(ENTRY_STR, this::handleEntity);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
        try {
            dao.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close the storage", e);
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        try (exchange) {
            int code = dao.isAvailable() ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_UNAVAILABLE;
            exchange.sendResponseHeaders(code, -1);
        }
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                String query = exchange.getRequestURI().getRawQuery();
                String id = "";
                if (query != null && query.startsWith(ID_STR)) {
                    id = URLDecoder.decode(query.substring(ID_STR.length()), StandardCharsets.UTF_8);
                }
                serveRecord(exchange, id);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            } catch (IOException e) {
                log.error("Failed to handle request", e);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }
        }
    }

    private void serveRecord(HttpExchange exchange, String id) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> sendValue(exchange, dao.get(id));
            case "PUT" -> {
                byte[] value = exchange.getRequestBody().readNBytes(MAX_VALUE_SIZE + 1);
                if (value.length > MAX_VALUE_SIZE) {
                    throw new IllegalArgumentException("Value is too large");
                }
                dao.upsert(id, value);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, -1);
            }
            case "DELETE" -> {
                dao.delete(id);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_ACCEPTED, -1);
            }
            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }
        }
    }

    private static void sendValue(HttpExchange exchange, byte[] value) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, value.length);
        exchange.getResponseBody().write(value);
    }
}
