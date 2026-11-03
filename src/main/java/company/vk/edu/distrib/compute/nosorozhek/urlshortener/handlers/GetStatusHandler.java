package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GetStatusHandler implements Handler {
    private static final String UP = "UP";

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        exchange.sendResponseHeaders(200, UP.length());
        exchange.getResponseBody().write(UP.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }
}
