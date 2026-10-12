package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpUtils {
    public static final int HTTP_UNPROCESSABLE_CONTENT = 422;
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    private static final int MAX_BODY_BYTES = 8 * 1024;
    private static final int NO_BODY = -1;
    private static final int NOT_SENT = -1;
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils() {
    }

    public static void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, NO_BODY);
    }

    public static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length == 0 ? NO_BODY : body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    public static boolean accepts(HttpExchange exchange, String path, String method) throws IOException {
        if (!path.equals(exchange.getRequestURI().getPath())) {
            sendEmpty(exchange, HttpURLConnection.HTTP_NOT_FOUND);
            return false;
        }
        if (!method.equals(exchange.getRequestMethod())) {
            sendEmpty(exchange, HttpURLConnection.HTTP_BAD_METHOD);
            return false;
        }
        return true;
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readNBytes(MAX_BODY_BYTES + 1);
            return bytes.length > MAX_BODY_BYTES ? "" : new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static HttpHandler safe(HttpHandler handler) {
        return exchange -> {
            try (exchange) {
                try {
                    handler.handle(exchange);
                } catch (IOException | RuntimeException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
                    }
                    if (exchange.getResponseCode() == NOT_SENT) {
                        sendEmpty(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
                    }
                }
            }
        };
    }
}
