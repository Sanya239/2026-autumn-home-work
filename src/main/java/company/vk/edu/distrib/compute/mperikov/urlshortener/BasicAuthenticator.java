package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

final class BasicAuthenticator {
    private static final String BASIC_PREFIX = "Basic ";

    private final Dao<String> users;

    BasicAuthenticator(Dao<String> users) {
        this.users = users;
    }

    boolean authorized(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return false;
        }
        String token = header.substring(BASIC_PREFIX.length()).strip();
        final String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException expected) {
            return false;
        }
        int separator = decoded.indexOf(':');
        return separator > 0
            && passwordMatches(decoded.substring(0, separator), decoded.substring(separator + 1));
    }

    void createUser(HttpExchange exchange) throws IOException {
        if (!HttpReplies.isHtmlUtf8(exchange)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        String body = HttpReplies.readBody(exchange);
        int separator = body.indexOf(':');
        if (!RequestChecks.isCredentialLine(body, separator)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        users.upsert(body.substring(0, separator), body.substring(separator + 1));
        HttpReplies.sendEmpty(exchange, HttpStatus.OK);
    }

    private boolean passwordMatches(String username, String password) throws IOException {
        try {
            return users.get(username).equals(password);
        } catch (NoSuchElementException | IllegalArgumentException expected) {
            return false;
        }
    }
}
