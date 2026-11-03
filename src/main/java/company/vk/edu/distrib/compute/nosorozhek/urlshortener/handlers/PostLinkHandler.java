package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.UrlShortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PostLinkHandler implements Handler {
    private final UrlShortener urlShortener;
    private final int port;

    public PostLinkHandler(UrlShortener urlShortener, int port) {
        this.urlShortener = urlShortener;
        this.port = port;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String link = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
        String linkId = urlShortener.create(link);

        String shortLink = "http://localhost:%d/%s".formatted(port, linkId);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(201, shortLink.length());
        exchange.getResponseBody().write(shortLink.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }
}
