package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import company.vk.edu.distrib.compute.Dao;

class RedirectHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(RedirectHandler.class);

    private final Dao<String> links;

    RedirectHandler(Dao<String> links) {
        this.links = links;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                redirect(exchange);
            } catch (NoSuchElementException expected) {
                HttpResponses.sendEmpty(exchange, HttpConstants.NOT_FOUND);
            } catch (IOException | RuntimeException e) {
                log.error("Failed to handle redirect request", e);
                HttpResponses.sendEmpty(exchange, HttpConstants.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void redirect(HttpExchange exchange) throws IOException {
        if (!HttpConstants.GET_METHOD.equals(exchange.getRequestMethod())) {
            HttpResponses.sendEmpty(exchange, HttpConstants.METHOD_NOT_ALLOWED);
            return;
        }
        String id = exchange.getRequestURI().getPath().substring(1);
        if (id.isEmpty() || id.contains("/")) {
            HttpResponses.sendEmpty(exchange, HttpConstants.NOT_FOUND);
            return;
        }
        if (!Ids.isValid(id)) {
            HttpResponses.sendEmpty(exchange, HttpConstants.UNPROCESSABLE_CONTENT);
            return;
        }
        exchange.getResponseHeaders().set("Location", links.get(id));
        HttpResponses.sendEmpty(exchange, HttpConstants.MOVED_PERMANENTLY);
    }
}
