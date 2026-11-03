package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;

import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.NO_BODY;
import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendMethodNotAllowed;

class StatusHandler extends BaseHandler {
    static final String PATH = "/v0/status";

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, GET);
            return;
        }
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_BODY);
    }
}
