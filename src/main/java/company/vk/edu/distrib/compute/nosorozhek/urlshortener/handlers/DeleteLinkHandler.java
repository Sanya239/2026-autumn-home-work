package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.UrlShortener;

import java.io.IOException;

public class DeleteLinkHandler implements Handler {
    private final UrlShortener urlShortener;

    public DeleteLinkHandler(UrlShortener urlShortener) {
        this.urlShortener = urlShortener;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String linkId = parameters.required(RouteParameters.ID);

        urlShortener.delete(linkId);

        exchange.sendResponseHeaders(202, -1);
        exchange.close();
    }
}
