package company.vk.edu.distrib.compute.annapiatkova.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.PrimitiveIterator;
import java.util.Base64;

class HandlerImpl implements HttpHandler {
    static final int ID_LENGTH = 10;
    static final String ALPHA_NUMERIC_PATTERN = "[a-zA-Z0-9]*";
    static final int HTTP_OK = 200;
    static final int HTTP_CREATED = 201;
    static final int HTTP_ACCEPTED = 202;
    static final int HTTP_MOVED_PERMANENTLY = 301;
    static final int HTTP_UNAUTHORIZED = 401;
    static final int HTTP_NOT_FOUND = 404;
    static final int HTTP_UNPROCESSABLE_CONTENT = 422;
    static final String STATUS_PATH = "/v0/status";
    static final String LINKS_PATH = "/v0/links";
    static final String LINKS_PREFIX = "/v0/links/";
    static final String USERS_PATH = "/internal/users";
    String linkPrefix;
    Dao<String> dao;
    AlphaNumericStringGenerator gen;
    Authenticator auth;

    HandlerImpl(Dao<String> dao, String linkPrefix) {
        this.dao = dao;
        this.linkPrefix = linkPrefix;
        this.gen = new AlphaNumericStringGenerator();
        this.auth = new Authenticator();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        if (needsAuthentication(method, path) && !authenticate(exchange)) {
            sendResponse(exchange, HTTP_UNAUTHORIZED);
            return;
        }
        switch (method) {
        case "GET":
            handleGet(exchange, path);
            break;
        case "POST":
            handlePost(exchange, path);
            break;
        case "PUT":
            handlePut(exchange, path);
            break;
        case "DELETE":
            handleDelete(exchange, path);
            break;
        default:
            sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
            break;
        }
    }

    private boolean needsAuthentication(String method, String path) {
        /*
        No authentication for GET /v0/status, GET /<ID>, and POST /internal/users
        */
        if ("GET".equals(method)) {
            return path.startsWith(LINKS_PREFIX);
        } else if ("POST".equals(method)) {
            return !USERS_PATH.equals(path);
        }
        return true;
    }

    private boolean authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.matches("Basic\s.*")) {
            return false;
        }
        String[] split = authHeader.split("\s", 2);
        String credentials = split[1];
        try {
            String decoded = new String(Base64.getDecoder().decode(credentials));
            if (!decoded.matches(".*:.*")) {
                return false;
            }
            String[] userPass = decoded.split(":", 2);
            String username = userPass[0];
            String password = userPass[1];
            return auth.checkCredentials(username, password);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (STATUS_PATH.equals(path)) {
            sendResponse(exchange, HTTP_OK);
            return;
        } else if (path.startsWith(LINKS_PREFIX)) {
            String id = path.substring(LINKS_PREFIX.length());
            if (!validateIdString(id)) {
                sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
                return;
            }
            String link = dao.get(id);
            if (link == null) {
                sendResponse(exchange, HTTP_NOT_FOUND);
                return;
            }
            sendResponse(exchange, HTTP_OK, link);
            return;
        }
        String id = path.substring(1);
        if (!validateIdString(id)) {
            sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
            return;
        }
        String link = dao.get(id);
        if (link == null) {
            sendResponse(exchange, HTTP_NOT_FOUND);
            return;
        }
        exchange.getResponseHeaders().add("Location", link);
        sendResponse(exchange, HTTP_MOVED_PERMANENTLY);
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (USERS_PATH.equals(path)) { 
            try (InputStream is = exchange.getRequestBody()) {
                String credentials = new String(is.readAllBytes());
                String[] splitResult = credentials.split(":");
                String username = splitResult[0];
                String password = splitResult[1];
                auth.addUser(username, password);
                sendResponse(exchange, HTTP_OK);
            }
            return;
        }
        if (LINKS_PATH.equals(path)) {
            try (InputStream is = exchange.getRequestBody()) {
                String longLink = new String(is.readAllBytes());
                if (!isValidURL(longLink)) {
                    sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
                    return;
                }
                String id = gen.generate();
                dao.upsert(id, longLink);
                sendResponse(exchange, HTTP_CREATED, linkPrefix + "/" + id);
            }
            return;
        }
        sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        if (path.startsWith(LINKS_PREFIX)) {
            String id = path.substring(LINKS_PREFIX.length());
            if (!validateIdString(id)) {
                sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
                return;
            }
            String link = dao.get(id);
            if (link == null) {
                sendResponse(exchange, HTTP_NOT_FOUND);
                return;
            }
            try (InputStream is = exchange.getRequestBody()) {
                String newLink = new String(is.readAllBytes());
                if (!isValidURL(newLink)) {
                    sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
                    return;
                }
                dao.upsert(id, newLink);
                sendResponse(exchange, HTTP_OK);
            }
            return;
        }
        sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.startsWith(LINKS_PREFIX)) {
            String id = path.substring(LINKS_PREFIX.length());
            if (!validateIdString(id)) {
                sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
                return;
            }
            dao.delete(id);
            sendResponse(exchange, HTTP_ACCEPTED);
            return;
        }
        sendResponse(exchange, HTTP_UNPROCESSABLE_CONTENT);
    }

    private void sendResponse(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write("".getBytes());
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String content) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        byte[] contentBytes = content.getBytes();
        exchange.sendResponseHeaders(code, contentBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(contentBytes);
        }
    }

    private static boolean isValidURL(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateIdString(String id) {
        return id.length() == ID_LENGTH && id.matches(ALPHA_NUMERIC_PATTERN);
    }

    private class AlphaNumericStringGenerator {
        static final String ALPHA_NUMERIC_SYMBOLS = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890";
        final PrimitiveIterator.OfInt iter;

        AlphaNumericStringGenerator() {
            Random random = new Random();
            iter = random.ints(0, ALPHA_NUMERIC_SYMBOLS.length()).iterator();
        }

        String generate() {
            char[] symbols = new char[ID_LENGTH];
            for (int i = 0; i < ID_LENGTH; i++) {
                symbols[i] = ALPHA_NUMERIC_SYMBOLS.charAt(iter.next());
            }
            return new String(symbols);
        }
    }
}
