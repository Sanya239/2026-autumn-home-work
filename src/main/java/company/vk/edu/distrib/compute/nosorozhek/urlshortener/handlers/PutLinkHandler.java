package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.UrlShortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PutLinkHandler implements Handler {
    private final UrlShortener urlShortener;

    public PutLinkHandler(UrlShortener urlShortener) {
        this.urlShortener = urlShortener;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String linkId = parameters.required(RouteParameters.ID);

        String link = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        urlShortener.update(linkId, link);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }
}
