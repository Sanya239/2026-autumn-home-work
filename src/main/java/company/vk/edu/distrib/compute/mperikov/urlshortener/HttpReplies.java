package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HttpReplies {
    private static final Logger log = LoggerFactory.getLogger(HttpReplies.class);
    private static final String CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String BASIC_CHALLENGE = "Basic realm=\"urlshortener\", charset=\"UTF-8\"";

    private HttpReplies() {
    }

    static String requestPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path;
    }

    static boolean isHtmlUtf8(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).replace(" ", "");
        return normalized.startsWith("text/html;") && normalized.contains("charset=utf-8");
    }

    static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void sendText(HttpExchange exchange, HttpStatus status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(status.value(), bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    static void sendEmpty(HttpExchange exchange, HttpStatus status) throws IOException {
        exchange.sendResponseHeaders(status.value(), -1);
    }

    static void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("WWW-Authenticate", BASIC_CHALLENGE);
        sendEmpty(exchange, HttpStatus.UNAUTHORIZED);
    }

    static void sendServerError(HttpExchange exchange) {
        try {
            exchange.sendResponseHeaders(HttpStatus.SERVER_ERROR.value(), -1);
        } catch (Exception ex) {
            log.debug("Could not write the error response", ex);
        }
    }
}
