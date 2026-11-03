package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class UserRegistrationHandler implements HttpHandler {
    static final String PATH = "/internal/users";
    private static final String POST = "POST";

    private final Dao<String> usersDao;

    UserRegistrationHandler(Dao<String> usersDao) {
        this.usersDao = usersDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            if (!PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            if (!POST.equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                int separator = body.indexOf(':');
                if (separator <= 0) {
                    exchange.sendResponseHeaders(422, -1);
                    return;
                }

                String username = body.substring(0, separator);
                String password = body.substring(separator + 1);
                usersDao.upsert(username, password);
                exchange.sendResponseHeaders(200, -1);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(422, -1);
            }
        }
    }
}
