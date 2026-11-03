package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.UrlShortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GetLinkHandler implements Handler {
    private final UrlShortener urlShortener;

    public GetLinkHandler(UrlShortener urlShortener) {
        this.urlShortener = urlShortener;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String linkId = parameters.required(RouteParameters.ID);

        String link = urlShortener.get(linkId);
        byte[] responseBody = link.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
