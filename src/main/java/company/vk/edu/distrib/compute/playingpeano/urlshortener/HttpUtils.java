package company.vk.edu.distrib.compute.playingpeano.urlshortener;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.jspecify.annotations.Nullable;

final class HttpUtils {
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String AUTHENTICATE_VALUE =
        "Basic realm=\"url-shortener\", charset=\"UTF-8\"";
    private static final String ALLOW_HEADER = "Allow";
    private static final String ID_ALPHABET =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int ID_LENGTH = 10;
    private static final int MAX_BODY_SIZE = 65_536;
    private static final int MAX_PORT = 65_535;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;

    private HttpUtils() {
    }

    static String readBody(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);
        if (bytes.length > MAX_BODY_SIZE) {
            throw new IllegalArgumentException("Request body is too large");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static boolean hasTextContentType(HttpExchange exchange) {
        @Nullable String contentType = exchange.getRequestHeaders().getFirst(CONTENT_TYPE_HEADER);
        return contentType != null && TEXT_CONTENT_TYPE.equalsIgnoreCase(contentType.trim());
    }

    static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(CONTENT_TYPE_HEADER, TEXT_CONTENT_TYPE);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    static void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    static void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set(AUTHENTICATE_HEADER, AUTHENTICATE_VALUE);
        sendEmpty(exchange, STATUS_UNAUTHORIZED);
    }

    static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set(ALLOW_HEADER, allowedMethods);
        sendEmpty(exchange, STATUS_METHOD_NOT_ALLOWED);
    }

    static String randomId(SecureRandom random) {
        StringBuilder result = new StringBuilder(ID_LENGTH);
        for (int index = 0; index < ID_LENGTH; index++) {
            result.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
        }
        return result.toString();
    }

    static void validateId(String id) {
        if (id.length() != ID_LENGTH) {
            throw new IllegalArgumentException("ID must contain exactly ten characters");
        }
        for (int index = 0; index < id.length(); index++) {
            if (ID_ALPHABET.indexOf(id.charAt(index)) < 0) {
                throw new IllegalArgumentException("ID must be ASCII alphanumeric");
            }
        }
    }

    static void validateLongLink(String longLink) {
        try {
            URI uri = new URI(longLink);
            boolean supportedScheme = HTTP_SCHEME.equalsIgnoreCase(uri.getScheme())
                || HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme());
            if (!supportedScheme || uri.getHost() == null || uri.getPort() > MAX_PORT) {
                throw new IllegalArgumentException("Invalid HTTP link");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid HTTP link", exception);
        }
    }
}
