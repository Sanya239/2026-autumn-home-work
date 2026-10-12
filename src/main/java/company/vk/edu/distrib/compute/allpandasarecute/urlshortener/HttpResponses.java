package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

final class HttpResponses {
    private static final String CONTENT_TYPE_TEXT = "text/html; charset=utf-8";

    private HttpResponses() {
    }

    static String readBody(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void sendEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
    }

    static void sendBody(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_TEXT);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
