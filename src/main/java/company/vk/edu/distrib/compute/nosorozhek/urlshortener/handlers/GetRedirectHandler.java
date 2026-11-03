package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.UrlShortener;

import java.io.IOException;

public class GetRedirectHandler implements Handler {
    private final UrlShortener urlShortener;

    public GetRedirectHandler(UrlShortener urlShortener) {
        this.urlShortener = urlShortener;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String linkId = parameters.required(RouteParameters.ID);

        String link = urlShortener.get(linkId);

        exchange.getResponseHeaders().set("Location", link);
        exchange.sendResponseHeaders(301, -1);
        exchange.close();
    }
}
