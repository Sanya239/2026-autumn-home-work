package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.NO_BODY;

abstract class BaseHandler implements HttpHandler {
    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final Logger log = LoggerFactory.getLogger(BaseHandler.class);

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                doHandle(exchange);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(HTTP_UNPROCESSABLE, NO_BODY);
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, NO_BODY);
            } catch (Exception e) {
                log.error("Failed to handle request", e);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, NO_BODY);
            }
        }
    }

    protected abstract void doHandle(HttpExchange exchange) throws IOException;
}
