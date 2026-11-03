package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

final class BasicAuthentication {
    private static final String PREFIX = "Basic ";

    private final Dao<String> usersDao;

    BasicAuthentication(Dao<String> usersDao) {
        this.usersDao = usersDao;
    }

    boolean authenticate(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        boolean authenticated = false;

        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            try {
                String encoded = header.substring(PREFIX.length()).trim();
                String credentials = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                int separator = credentials.indexOf(':');
                if (separator > 0) {
                    String username = credentials.substring(0, separator);
                    String password = credentials.substring(separator + 1);
                    authenticated = password.equals(usersDao.get(username));
                }
            } catch (IllegalArgumentException | NoSuchElementException e) {
                authenticated = false;
            }
        }

        if (!authenticated) {
            exchange.getResponseHeaders().set(
                    "WWW-Authenticate",
                    "Basic realm=\"url-shortener\", charset=\"UTF-8\""
            );
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        }

        return authenticated;
    }
}
