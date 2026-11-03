package company.vk.edu.distrib.compute.netheer.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.NoSuchElementException;

final class UrlShortenerHandlers {
    private static final String ID_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 10;
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    private static final String STATUS_PATH = "/v0/status";
    private static final String LINKS_PATH = "/v0/links";
    private static final String LINKS_PREFIX = "/v0/links/";

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";

    private final int port;
    private final Dao<String> linksDao;
    private final UrlShortenerAuth auth;
    private final SecureRandom random = new SecureRandom();

    UrlShortenerHandlers(int port, Dao<String> linksDao, UrlShortenerAuth auth) {
        this.port = port;
        this.linksDao = linksDao;
        this.auth = auth;
    }

    void handleStatus(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!STATUS_PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                return;
            }
            if (!GET_METHOD.equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, GET_METHOD);
                return;
            }
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
        }
    }

    private void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        if (body.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
    }

    private boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]{10}");
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            int uriPort = uri.getPort();

            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && (uriPort == -1 || (uriPort >= 1 && uriPort <= 65535));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    void handleLinks(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!auth.authenticate(exchange)) {
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (LINKS_PATH.equals(path)) {
                handleLinksCollection(exchange, method);
                return;
            }

            if (!path.startsWith(LINKS_PREFIX)) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                return;
            }

            String id = path.substring(LINKS_PREFIX.length());
            if (!isValidId(id)) {
                exchange.sendResponseHeaders(HTTP_UNPROCESSABLE_ENTITY, -1);
                return;
            }

            switch (method) {
                case GET_METHOD -> getLink(exchange, id);
                case PUT_METHOD -> updateLink(exchange, id);
                case DELETE_METHOD -> deleteLink(exchange, id);
                default -> sendMethodNotAllowed(exchange, GET_METHOD + ", " + PUT_METHOD + ", " + DELETE_METHOD);
            }
        }
    }

    private void getLink(HttpExchange exchange, String id) throws IOException {
        String url;
        try {
            url = linksDao.get(id);
        } catch (NoSuchElementException exception) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            return;
        }

        sendText(exchange, HttpURLConnection.HTTP_OK, url);
    }

    private void createLink(HttpExchange exchange) throws IOException {
        String url = readBody(exchange);

        if (!isValidUrl(url)) {
            exchange.sendResponseHeaders(HTTP_UNPROCESSABLE_ENTITY, -1);
            return;
        }

        String id;
        do {
            id = generateId();
        } while (linkExists(id));

        linksDao.upsert(id, url);

        String shortUrl = "http://localhost:" + port + "/" + id;
        sendText(exchange, HttpURLConnection.HTTP_CREATED, shortUrl);
    }

    private void updateLink(HttpExchange exchange, String id) throws IOException {
        String url = readBody(exchange);

        if (!isValidUrl(url)) {
            exchange.sendResponseHeaders(HTTP_UNPROCESSABLE_ENTITY, -1);
            return;
        }

        boolean updated = linkExists(id);
        if (updated) {
            linksDao.upsert(id, url);
        }

        exchange.sendResponseHeaders(updated ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_NOT_FOUND, -1);
    }

    private void deleteLink(HttpExchange exchange, String id) throws IOException {
        linksDao.delete(id);

        exchange.sendResponseHeaders(HttpURLConnection.HTTP_ACCEPTED, -1);
    }

    void handleRedirect(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!GET_METHOD.equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, GET_METHOD);
                return;
            }

            String id = exchange.getRequestURI().getPath().substring(1);
            if (!isValidId(id)) {
                exchange.sendResponseHeaders(HTTP_UNPROCESSABLE_ENTITY, -1);
                return;
            }

            String url;
            try {
                url = linksDao.get(id);
            } catch (NoSuchElementException exception) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                return;
            }

            exchange.getResponseHeaders().set("Location", url);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_PERM, -1);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
    }

    private String generateId() {
        StringBuilder result = new StringBuilder(ID_LENGTH);
        for (int index = 0; index < ID_LENGTH; index++) {
            int position = random.nextInt(ID_ALPHABET.length());
            result.append(ID_ALPHABET.charAt(position));
        }
        return result.toString();
    }

    private boolean linkExists(String id) throws IOException {
        try {
            linksDao.get(id);
            return true;
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void handleLinksCollection(HttpExchange exchange, String method) throws IOException {
        if (POST_METHOD.equals(method)) {
            createLink(exchange);
        } else {
            sendMethodNotAllowed(exchange, POST_METHOD);
        }
    }
}
