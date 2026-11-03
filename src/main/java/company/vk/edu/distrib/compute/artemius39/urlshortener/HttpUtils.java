package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    public static final int AUTH_FIELD_COUNT = 2;

    private HttpUtils() {
        // utility class
    }

    public static String getRequestBodyAsString(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        return new String(requestBytes, StandardCharsets.UTF_8);
    }

    public static String getPathParam(HttpExchange exchange, String prefix) {
        return exchange.getRequestURI().getPath().substring(prefix.length());
    }

    public static void unauthorized(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().set(
            "WWW-Authenticate", "Basic realm=\"urlshortener\", charset=\"UTF-8\""
        );
        httpExchange.sendResponseHeaders(401, -1);
    }

    public static void notFound(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(404, -1);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    public static void unprocessableEntity(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(422, -1);
    }

    public static void sendResponse(HttpExchange exchange, int responseCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(responseCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    public static @Nullable Credentials parseAuth(HttpExchange httpExchange) {
        List<String> authorizationHeaders = httpExchange.getRequestHeaders().get("Authorization");
        if (authorizationHeaders == null || authorizationHeaders.size() != 1) {
            return null;
        }
        String authorization = authorizationHeaders.getFirst();
        if (!authorization.startsWith(BASIC_AUTH_PREFIX)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(authorization.substring(BASIC_AUTH_PREFIX.length()).strip());
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            String[] split = credentials.split(":");
            if (split.length != AUTH_FIELD_COUNT) {
                return null;
            }
            String username = split[0];
            String password = split[1];
            return new Credentials(username, password);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return null;
        }
    }

    public static final class Methods {
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";

        private Methods() {
            // utility class
        }
    }
}
