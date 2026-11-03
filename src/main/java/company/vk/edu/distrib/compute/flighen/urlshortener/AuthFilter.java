package company.vk.edu.distrib.compute.flighen.urlshortener;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;

public class AuthFilter extends Filter {
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int AUTH_HEADER_PARTS_COUNT = 2;

    private final Logger log =
            LoggerFactory.getLogger(AuthFilter.class);

    private static final String REALM = "url-shortener";

    private final Dao<String> usersDao;

    public AuthFilter(Dao<String> dao) {
        super();
        usersDao = dao;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (isAuthorized(exchange)) {
            chain.doFilter(exchange);
        } else {
            returnUnAuthorized(exchange);
        }
    }

    @Override
    public String description() {
        return "Basic auth filter";
    }

    private boolean isAuthorized(HttpExchange exchange) throws IOException {
        String rawAuth = exchange.getRequestHeaders().getFirst("Authorization");

        if (rawAuth == null) {
            log.warn("Authorization header is missing");
            return false;
        }

        String[] userInfo = parseCredentials(rawAuth);

        if (userInfo.length != AUTH_HEADER_PARTS_COUNT) {
            log.warn("Invalid authorization credentials format");
            return false;
        }

        String login = userInfo[0];
        String password = userInfo[1];

        if (login.isBlank() || password.isBlank()) {
            log.warn("Login or password is blank");
            return false;
        }

        if (!credentialsMatch(login, password)) {
            log.warn("Authentication failed for user: {}", login);
            return false;
        }

        return true;
    }

    private boolean credentialsMatch(String login, String password) throws IOException {
        try {
            String rightPass = usersDao.get(login);
            return Objects.equals(rightPass, password);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            return false;
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to read user credentials from DAO", e);
            }
            throw e;
        }
    }

    private String[] parseCredentials(String rawAuth) {
        String strippedAuth = rawAuth.strip();

        String[] args = strippedAuth.split(" ");

        if (args.length != AUTH_HEADER_PARTS_COUNT || !Objects.equals(args[0], "Basic")) {
            return new String[] {};
        }

        byte[] decoded = Base64.getDecoder().decode(args[1]);

        String userPass = new String(decoded, StandardCharsets.UTF_8);

        if (!userPass.contains(":")) {
            return new String[] {};
        }

        return userPass.split(":");
    }

    private void returnUnAuthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders()
                .add("WWW-Authenticate", "Basic REALM=\"%s\", charset=\"UTF-8\"".formatted(REALM));
        exchange.sendResponseHeaders(HTTP_UNAUTHORIZED, -1);
    }
}
