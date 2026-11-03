package company.vk.edu.distrib.compute.sh4rrkyyyy.urlshortener;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpUtils {
    private HttpUtils() {
    }

    public static void sendRsp(HttpExchange exchange, int code, String link) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, link.length());
        exchange.getResponseBody().write(link.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }

    public static void sendEmptyRsp(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }

}
