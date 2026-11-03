package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

class SimpleAuth {
    private final Dao<String> users;

    SimpleAuth(Dao<String> users) {
        this.users = users;
    }

    boolean isAuthorized(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return false;
        }
        String[] schemeAndData = header.split(" ", 2);
        if (schemeAndData.length != 2 || !"Basic".equalsIgnoreCase(schemeAndData[0])) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(schemeAndData[1].trim());
            User user = parseUser(new String(decoded, StandardCharsets.UTF_8));
            return user.password().equals(users.get(user.name()));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return false;
        }
    }

    static User parseUser(String nameAndPassword) {
        String[] parts = nameAndPassword.split(":", 2);
        if (parts.length != 2 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Expected name:password");
        }
        return new User(parts[0], parts[1]);
    }
}
