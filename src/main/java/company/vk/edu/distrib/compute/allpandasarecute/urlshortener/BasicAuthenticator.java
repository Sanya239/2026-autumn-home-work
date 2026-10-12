package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;

import company.vk.edu.distrib.compute.Dao;

class BasicAuthenticator {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    private final Dao<String> users;

    BasicAuthenticator(Dao<String> users) {
        this.users = users;
    }

    boolean authenticate(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BASIC_PREFIX)) {
            return false;
        }
        String credentials;
        try {
            byte[] decoded = Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()));
            credentials = new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        int separator = credentials.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        try {
            return users.get(credentials.substring(0, separator)).equals(credentials.substring(separator + 1));
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
