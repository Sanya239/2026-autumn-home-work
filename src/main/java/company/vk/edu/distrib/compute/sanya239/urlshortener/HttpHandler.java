package company.vk.edu.distrib.compute.sanya239.urlshortener;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@SuppressWarnings("PMD.GodClass")
public class HttpHandler implements com.sun.net.httpserver.HttpHandler {
    private static final String LINKS_PATH = "/v0/links";
    private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9]{10}");

    private final Dao linkDao;
    private final Dao accountDao;

    public HttpHandler(Dao linkDao, Dao accountDao) {
        this.linkDao = linkDao;
        this.accountDao = accountDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/v0/status".equals(path)) {
            respond(exchange, "GET".equals(method) ? 200 : 405, null);
            return;
        }
        if ("/internal/users".equals(path)) {
            if ("POST".equals(method)) {
                handleUsers(exchange);
            } else {
                respond(exchange, 405, null);
            }
            return;
        }
        if (LINKS_PATH.equals(path) || path.startsWith(LINKS_PATH + "/")) {
            if (!isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set(
                    "WWW-Authenticate", "Basic realm=\"url-shortener\", charset=\"UTF-8\""
                );
                respond(exchange, 401, null);
                return;
            }
            handleLinks(exchange, path, method);
            return;
        }
        if ("GET".equals(method) && path.length() > 1 && path.indexOf('/', 1) < 0) {
            redirect(exchange, path.substring(1));
            return;
        }
        respond(exchange, 404, null);
    }

    private void handleLinks(HttpExchange exchange, String path, String method) throws IOException {
        if (LINKS_PATH.equals(path)) {
            if ("POST".equals(method)) {
                createLink(exchange);
            } else {
                respond(exchange, 405, null);
            }
            return;
        }

        String id = path.substring(LINKS_PATH.length() + 1);
        if (!isValidId(id)) {
            respond(exchange, 422, null);
            return;
        }

        switch (method) {
            case "GET" -> getLink(exchange, id);
            case "PUT" -> updateLink(exchange, id);
            case "DELETE" -> {
                linkDao.delete(id);
                respond(exchange, 202, null);
            }
            default -> respond(exchange, 405, null);
        }
    }

    private void createLink(HttpExchange exchange) throws IOException {
        String link = readBody(exchange);
        if (!isValidLink(link)) {
            respond(exchange, 422, null);
            return;
        }

        String id;
        do {
            id = randomId();
        } while (linkDao.get(id) != null);
        linkDao.upsert(id, link);

        String shortLink = "http://localhost:" + exchange.getLocalAddress().getPort() + "/" + id;
        respond(exchange, 201, shortLink);
    }

    private void getLink(HttpExchange exchange, String id) throws IOException {
        String link = linkDao.get(id);
        if (link == null) {
            respond(exchange, 404, null);
        } else {
            respond(exchange, 200, link);
        }
    }

    private void updateLink(HttpExchange exchange, String id) throws IOException {
        String link = readBody(exchange);
        if (!isValidLink(link)) {
            respond(exchange, 422, null);
            return;
        }
        if (linkDao.get(id) == null) {
            respond(exchange, 404, null);
        } else {
            linkDao.upsert(id, link);
            respond(exchange, 200, null);
        }
    }

    private void redirect(HttpExchange exchange, String id) throws IOException {
        if (!isValidId(id)) {
            respond(exchange, 422, null);
            return;
        }
        String link = linkDao.get(id);
        if (link == null) {
            respond(exchange, 404, null);
        } else {
            exchange.getResponseHeaders().set("Location", link);
            respond(exchange, 301, null);
        }
    }

    private void handleUsers(HttpExchange exchange) throws IOException {
        String credentials = readBody(exchange);
        int separator = credentials.indexOf(':');
        if (separator <= 0 || separator == credentials.length() - 1) {
            respond(exchange, 422, null);
            return;
        }
        accountDao.upsert(credentials.substring(0, separator), credentials.substring(separator + 1));
        respond(exchange, 200, null);
    }

    private boolean isAuthenticated(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String credentials = new String(
                Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8
            );
            int separator = credentials.indexOf(':');
            return separator > 0
                && credentials.substring(separator + 1).equals(accountDao.get(credentials.substring(0, separator)));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isValidId(String id) {
        return ID_PATTERN.matcher(id).matches();
    }

    private static boolean isValidLink(String link) {
        try {
            URI uri = new URI(link);
            return link.equals(link.trim())
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String randomId() {
        StringBuilder id = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            id.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
        }
        return id.toString();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (var body = exchange.getRequestBody()) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
        if (bytes != null) {
            exchange.getResponseHeaders().set("Content-Type", TEXT_CONTENT_TYPE);
        }
        exchange.sendResponseHeaders(status, bytes == null ? -1 : bytes.length);
        if (bytes != null) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
