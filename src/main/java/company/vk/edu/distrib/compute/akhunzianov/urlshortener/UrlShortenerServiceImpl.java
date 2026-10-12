package company.vk.edu.distrib.compute.akhunzianov.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);

    private static final String ROOT_PATH = "/";
    private static final String STATUS_PATH = "/v0/status";
    private static final String LINKS_PATH = "/v0/links";
    private static final String LINKS_PREFIX = LINKS_PATH + ROOT_PATH;
    private static final String USERS_PATH = "/internal/users";

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";

    private static final String TEXT_HTML = "text/html; charset=utf-8";
    private static final Pattern GOOD_ID = Pattern.compile("[a-zA-Z0-9]{10}");
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_SIZE = 10;
    private static final int TRIES = 100;

    private static final int OK_CODE = 200;
    private static final int CREATED_CODE = 201;
    private static final int ACCEPTED_CODE = 202;
    private static final int MOVED_PERMANENTLY_CODE = 301;
    private static final int NOT_FOUND_CODE = 404;
    private static final int METHOD_NOT_ALLOWED_CODE = 405;
    private static final int UNPROCESSABLE_CODE = 422;
    private static final int INTERNAL_ERROR_CODE = 500;
    private static final int NO_BODY = -1;

    private final SecureRandom random = new SecureRandom();
    private final int port;
    private final Dao<String> links;

    @Nullable
    private final Dao<String> users;

    @Nullable
    private HttpServer server;

    private boolean stopped;

    public UrlShortenerServiceImpl(int port, Dao<String> links, @Nullable Dao<String> users) {
        this.port = port;
        this.links = links;
        this.users = users;
    }

    @Override
    public void start() {
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        HttpServer http;
        try {
            http = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot bind port " + port, e);
        }
        http.createContext(STATUS_PATH, guarded(exchange -> reply(exchange, OK_CODE, "")));
        http.createContext(ROOT_PATH, guarded(this::onRedirect));

        var links = http.createContext(LINKS_PATH, guarded(this::onLinks));
        if (users != null) {
            links.getFilters().add(new BasicAuthFilter(users));
            http.createContext(USERS_PATH, guarded(this::onUsers));
        }

        http.setExecutor(null);
        http.start();
        server = http;
    }

    @Override
    public void stop() {
        if (server == null || stopped) {
            throw new IllegalStateException("Not started");
        }
        server.stop(0);
        stopped = true;
        try {
            links.close();
            if (users != null) {
                users.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot close storage", e);
        }
    }

    private void onLinks(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (LINKS_PATH.equals(path)) {
            if (POST.equals(exchange.getRequestMethod())) {
                addLink(exchange);
            } else {
                reply(exchange, METHOD_NOT_ALLOWED_CODE, "");
            }
            return;
        }
        if (!path.startsWith(LINKS_PREFIX)) {
            reply(exchange, NOT_FOUND_CODE, "");
            return;
        }
        var id = path.substring(LINKS_PREFIX.length());
        if (!GOOD_ID.matcher(id).matches()) {
            reply(exchange, UNPROCESSABLE_CODE, "");
            return;
        }
        switch (exchange.getRequestMethod()) {
            case GET -> showLink(exchange, id);
            case PUT -> changeLink(exchange, id);
            case DELETE -> dropLink(exchange, id);
            default -> reply(exchange, METHOD_NOT_ALLOWED_CODE, "");
        }
    }

    private void addLink(HttpExchange exchange) throws IOException {
        var longLink = body(exchange);
        if (!isUrl(longLink)) {
            reply(exchange, UNPROCESSABLE_CODE, "");
            return;
        }
        var id = pickId();
        links.upsert(id, longLink);
        reply(exchange, CREATED_CODE, "http://localhost:" + port + ROOT_PATH + id);
    }

    private void showLink(HttpExchange exchange, String id) throws IOException {
        try {
            reply(exchange, OK_CODE, links.get(id));
        } catch (NoSuchElementException e) {
            reply(exchange, NOT_FOUND_CODE, "");
        }
    }

    private void changeLink(HttpExchange exchange, String id) throws IOException {
        var longLink = body(exchange);
        if (!isUrl(longLink)) {
            reply(exchange, UNPROCESSABLE_CODE, "");
            return;
        }
        try {
            links.get(id);
        } catch (NoSuchElementException e) {
            reply(exchange, NOT_FOUND_CODE, "");
            return;
        }
        links.upsert(id, longLink);
        reply(exchange, OK_CODE, "");
    }

    private void dropLink(HttpExchange exchange, String id) throws IOException {
        links.delete(id);
        reply(exchange, ACCEPTED_CODE, "");
    }

    private void onRedirect(HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            reply(exchange, METHOD_NOT_ALLOWED_CODE, "");
            return;
        }
        var id = exchange.getRequestURI().getPath().substring(ROOT_PATH.length());
        if (!GOOD_ID.matcher(id).matches()) {
            reply(exchange, UNPROCESSABLE_CODE, "");
            return;
        }
        try {
            exchange.getResponseHeaders().add("Location", links.get(id));
            reply(exchange, MOVED_PERMANENTLY_CODE, "");
        } catch (NoSuchElementException e) {
            reply(exchange, NOT_FOUND_CODE, "");
        }
    }

    private void onUsers(HttpExchange exchange) throws IOException {
        if (!POST.equals(exchange.getRequestMethod())) {
            reply(exchange, METHOD_NOT_ALLOWED_CODE, "");
            return;
        }
        var creds = body(exchange);
        var colon = creds.indexOf(':');
        if (colon <= 0 || colon == creds.length() - 1) {
            reply(exchange, UNPROCESSABLE_CODE, "");
            return;
        }
        usersOrFail().upsert(creds.substring(0, colon), creds.substring(colon + 1));
        reply(exchange, OK_CODE, "");
    }

    private String pickId() throws IOException {
        for (int attempt = 0; attempt < TRIES; attempt++) {
            var id = makeId();
            try {
                links.get(id);
            } catch (NoSuchElementException free) {
                return id;
            }
        }
        throw new IllegalStateException("No free id found in " + TRIES + " attempts");
    }

    private String makeId() {
        var id = new StringBuilder(ID_SIZE);
        for (int i = 0; i < ID_SIZE; i++) {
            id.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return id.toString();
    }

    private Dao<String> usersOrFail() {
        if (users == null) {
            throw new IllegalStateException("Authentication is disabled");
        }
        return users;
    }

    private static HttpHandler guarded(HttpHandler handler) {
        return exchange -> {
            try {
                handler.handle(exchange);
            } catch (RuntimeException | IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
                }
                if (exchange.getResponseCode() == NO_BODY) {
                    reply(exchange, INTERNAL_ERROR_CODE, "");
                } else {
                    exchange.close();
                }
            }
        };
    }

    private static boolean isUrl(String link) {
        try {
            var uri = new URI(link);
            return uri.isAbsolute() && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
    }

    private static void reply(HttpExchange exchange, int code, String text) throws IOException {
        var bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", TEXT_HTML);
        exchange.sendResponseHeaders(code, bytes.length == 0 ? NO_BODY : bytes.length);
        if (bytes.length > 0) {
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }
}
