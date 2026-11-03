package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.internal;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.security.PasswordHasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UserController implements NetworkInteractable {

    private final Dao<String> userDao;

    public UserController(Dao<String> userDao) {
        this.userDao = userDao;
    }

    @Route(method = "POST", path = "/internal/users", requiresAuthorization = false)
    public void createOrUpdateUser(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();

        String[] parts = body.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank()) {
            this.sendStatusCodeResponse(exchange, StatusCode.HTTP_BAD_REQUEST);
            return;
        }

        String username = parts[0].trim();
        String password = parts[1];
        String passwordHash = PasswordHasher.hash(password);

        try {
            userDao.upsert(username, passwordHash);
            sendStringResponse(exchange, "OK", StatusCode.HTTP_OK);
        } catch (IllegalArgumentException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_BAD_REQUEST);
        }
    }
}
