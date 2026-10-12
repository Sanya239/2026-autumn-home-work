package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class StatusHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (HttpConstants.GET_METHOD.equals(exchange.getRequestMethod())) {
                HttpResponses.sendEmpty(exchange, HttpConstants.OK);
            } else {
                HttpResponses.sendEmpty(exchange, HttpConstants.METHOD_NOT_ALLOWED);
            }
        }
    }
}
