package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

final class HttpUtils {
    static final long NO_BODY = -1;
    private static final int MAX_BODY_SIZE = 8 * 1024;

    private HttpUtils() {
    }

    static String readBody(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);
        if (body.length > MAX_BODY_SIZE) {
            throw new IllegalArgumentException("Request body is too large");
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    static void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
    }
    
    static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_BODY);
    }

    static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_PERM, NO_BODY);
    }

    static void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"shortener\", charset=\"UTF-8\"");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, NO_BODY);
    }
}
