package company.vk.edu.distrib.compute.kl1dd.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import java.io.IOException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.NoSuchElementException;

public class MyUrlShortenerService implements UrlShortenerService {
    private static final int STOP_DELAY_SECONDS = 2;
    private static final int SUPPOSED_LENGTH_AFTER_SPLIT = 2;
    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";
    private static final String LINKS_PATH = "/v0/links/";
    private static final String SLASH_STRING = "/";
    private static final String HTTP_STRING = "http";
    private static final String HTTPS_STRING = "https";
    private static final String COLON_SPLITTER = ":";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();
    private final HttpServer httpServer;
    private Dao<String> linksDao;
    private final AuthService authService;
    private boolean startedOrStopped;

    public MyUrlShortenerService(HttpServer httpServer) {
        this.httpServer = httpServer;
        this.linksDao = new MyDao();
        this.authService = new AuthService();

        httpServer.createContext("/v0/status", this::handleStatus);
        httpServer.createContext("/v0/links", this::handleLinks);
        httpServer.createContext("/", this::handleRedirect);
        httpServer.createContext("/internal/users", this::handleUsers);
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        if (startedOrStopped) {
            throw new IllegalStateException();
        }

        this.linksDao = dao;
    }

    private void handleUsers(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!(POST_METHOD.equals(method))) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String reqBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String[] reqSplit = reqBody.split(COLON_SPLITTER, 2);
        if (reqSplit.length != SUPPOSED_LENGTH_AFTER_SPLIT) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }
        String username = reqSplit[0];
        String password = reqSplit[1];

        authService.createUser(username, password);

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private String generateRandomID(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
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

    private void handleLinks(HttpExchange exchange) throws IOException {
        if (!authService.isAuthed(exchange)) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        String method = exchange.getRequestMethod();
        switch (method) {
            case "POST" -> {
                handlePost(exchange);
            }
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

    private void handlePost(HttpExchange exchange) throws IOException {
        String linkBefore = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (!isValidURL(linkBefore)) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }

        String id = generateRandomID(10);
        linksDao.upsert(id, linkBefore);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");

        String linkAfter = "http://localhost:" + httpServer.getAddress().getPort() + "/" + id;
        byte[] responseBytes = linkAfter.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(201, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);

        exchange.close();
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String reqPath = exchange.getRequestURI().getPath();
        String id = reqPath.substring(LINKS_PATH.length());

        if (!isValidID(id)) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }

        try {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");

            String linkByID = linksDao.get(id);
            byte[] responseBytes = linkByID.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String reqPath = exchange.getRequestURI().getPath();
        String id = reqPath.substring(LINKS_PATH.length());
        if (!isValidID(id)) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }

        try {
            linksDao.get(id);

            String newLink = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!isValidURL(newLink)) {
                exchange.sendResponseHeaders(422, -1);
                exchange.close();
                return;
            }

            linksDao.upsert(id, newLink);

            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String reqPath = exchange.getRequestURI().getPath();
        String id = reqPath.substring(LINKS_PATH.length());

        if (!isValidID(id)) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }

        linksDao.delete(id);
        exchange.sendResponseHeaders(202, -1);
        exchange.close();
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!GET_METHOD.equals(method)) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String reqPath = exchange.getRequestURI().getPath();
        String id = reqPath.substring(SLASH_STRING.length());
        if (!isValidID(id)) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }

        try {
            String linkByID = linksDao.get(id);

            exchange.getResponseHeaders().set("Location", linkByID);
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private boolean isValidID(String id) {
        if (id == null || id.length() != 10) {
            return false;
        }

        for (int i = 0; i < 10; i++) {
            if (!isAlphaNumeric(id.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean isAlphaNumeric(char current) {
        return ('A' <= current && current <= 'Z')
                || ('a' <= current && current <= 'z')
                || ('0' <= current && current <= '9');
    }

    private boolean isValidURL(String url) {
        try {
            URI tryUri = URI.create(url);
            String connectionType = tryUri.getScheme();
            String site = tryUri.getHost();

            if (connectionType == null || site == null) {
                return false;
            }

            return HTTP_STRING.equals(connectionType) || HTTPS_STRING.equals(connectionType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void start() {
        startedOrStopped = true;
        httpServer.start();
    }

    @Override
    public void stop() {
        startedOrStopped = true;
        httpServer.stop(STOP_DELAY_SECONDS);
    }
}
