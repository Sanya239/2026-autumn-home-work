package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Random;

public class UrlShortenerService implements company.vk.edu.distrib.compute.urlshortener.UrlShortenerService {
    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String STATUS_PATH = "/v0/status";
    private static final String LINKS_PATH = "/v0/links";
    private static final int ID_LEN = 10;
    private static final Random RND = new Random();

    private final int port;
    private final HttpServer server;
    private final Dao<String> linksDao;
    private final BasicAuthentication authentication;

    public UrlShortenerService(int port, Dao<String> linksDao, Dao<String> usersDao) throws IOException {
        this.port = port;
        this.linksDao = linksDao;
        this.authentication = new BasicAuthentication(usersDao);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(STATUS_PATH, this::handleStatus);
        server.createContext(LINKS_PATH, this::handleLinks);
        server.createContext(UserRegistrationHandler.PATH, new UserRegistrationHandler(usersDao));
        server.createContext("/", this::handleRedirect);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!STATUS_PATH.equals(exchange.getRequestURI().getPath())) {
            sendEmptyResponse(exchange, 404);
            return;
        }
        if (!GET.equals(exchange.getRequestMethod())) {
            sendEmptyResponse(exchange, 405);
            return;
        }

        sendEmptyResponse(exchange, 200);
    }

    private void handleLinks(HttpExchange exchange) throws IOException {
        if (!authentication.authenticate(exchange)) {
            return;
        }

        try {
            switch (exchange.getRequestMethod()) {
                case GET -> handleGetLink(exchange);
                case POST -> handleCreateLink(exchange);
                case PUT -> handleUpdateLink(exchange);
                case DELETE -> handleDeleteLink(exchange);
                default -> sendEmptyResponse(exchange, 404);
            }
        } catch (BufferOverflowException e) {
            sendEmptyResponse(exchange, 413);
        }
    }

    private void handleGetLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        try {
            sendResponse(exchange, 200, linksDao.get(id));
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private void handleCreateLink(HttpExchange exchange) throws IOException {
        if (!LINKS_PATH.equals(exchange.getRequestURI().getPath())) {
            sendEmptyResponse(exchange, 404);
            return;
        }

        String url = RequestBodyUtils.read(exchange.getRequestBody());
        if (!isValidUrl(url)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        String id = generateId();
        linksDao.upsert(id, url);
        sendResponse(exchange, 201, "http://localhost:%d/%s".formatted(port, id));
    }

    private void handleUpdateLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        String url = RequestBodyUtils.read(exchange.getRequestBody());
        if (!isValidUrl(url)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        try {
            linksDao.get(id);
            linksDao.upsert(id, url);
            sendEmptyResponse(exchange, 200);
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private void handleDeleteLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        linksDao.delete(id);
        sendEmptyResponse(exchange, 202);
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            if (!authentication.authenticate(exchange)) {
                return;
            }
            sendEmptyResponse(exchange, 405);
            return;
        }

        String id = exchange.getRequestURI().getPath().substring(1);
        if (!isValidId(id)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        try {
            String url = linksDao.get(id);
            exchange.getResponseHeaders().set("Location", url);
            sendEmptyResponse(exchange, 301);
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private static String getValidLinkId(HttpExchange exchange) throws IOException {
        String id = getIdFromLinksPath(exchange);
        if (id == null) {
            sendEmptyResponse(exchange, 404);
            return null;
        }
        if (!isValidId(id)) {
            sendEmptyResponse(exchange, 422);
            return null;
        }
        return id;
    }

    private static String getIdFromLinksPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String pathWithId = LINKS_PATH + "/";
        if (!path.startsWith(pathWithId)) {
            return null;
        }
        return path.substring(pathWithId.length());
    }

    private static boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]{10}");
    }

    private static boolean isValidUrl(String url) {
        try {
            var uri = URI.create(url);
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String generateId() throws IOException {
        String generatedId = createRandomId();
        while (linkExists(generatedId)) {
            generatedId = createRandomId();
        }
        return generatedId;
    }

    private static String createRandomId() {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < ID_LEN; i++) {
            int index = RND.nextInt(ID_CHARS.length());
            id.append(ID_CHARS.charAt(index));
        }
        return id.toString();
    }

    private boolean linkExists(String id) throws IOException {
        try {
            linksDao.get(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private static void sendResponse(HttpExchange exchange, int code,String body) throws IOException {
        final var response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, response.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    private static void sendEmptyResponse(HttpExchange exchange,int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }

}
