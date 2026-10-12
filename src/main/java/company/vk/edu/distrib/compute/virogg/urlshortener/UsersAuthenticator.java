package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UsersAuthenticator extends BasicAuthenticator {
    private static final String REALM = "urlshortener";
    private static final String CHALLENGE = "Basic realm=\"" + REALM + "\", charset=\"UTF-8\"";
    private static final Logger log = LoggerFactory.getLogger(UsersAuthenticator.class);

    private final Dao<String> users;

    public UsersAuthenticator(Dao<String> users) {
        super(REALM, StandardCharsets.UTF_8);
        this.users = users;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        Result result;
        try {
            result = super.authenticate(exchange);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            result = new Failure(HttpURLConnection.HTTP_UNAUTHORIZED);
        }
        if (!(result instanceof Success)) {
            exchange.getResponseHeaders().set("WWW-Authenticate", CHALLENGE);
        }
        return result;
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        String stored;
        try {
            stored = users.get(username);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            return false;
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to read credentials for {}", username, e);
            }
            return false;
        }
        byte[] expected = stored.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, password.getBytes(StandardCharsets.UTF_8));
    }
}
