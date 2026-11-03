package company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication.AuthenticationMiddleware;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication.Credentials;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication.UserService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PostUserHandler implements Handler {
    private final UserService userService;

    public PostUserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException, IllegalArgumentException {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Credentials credentials = AuthenticationMiddleware.parseColonSeparatedCredentials(body);
        userService.register(credentials);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }
}
