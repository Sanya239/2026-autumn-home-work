package company.vk.edu.distrib.compute.akhunzianov.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

public class BasicAuthFilter extends Filter {

    private static final String BASIC = "Basic ";
    private static final int UNAUTHORIZED_CODE = 401;
    private static final int NO_BODY = -1;

    private final Dao<String> users;

    public BasicAuthFilter(Dao<String> users) {
        super();
        this.users = users;
    }

    @Override
    public String description() {
        return "Basic authentication";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (knowsUser(exchange)) {
            chain.doFilter(exchange);
            return;
        }
        exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"url-shortener\", charset=\"UTF-8\"");
        exchange.sendResponseHeaders(UNAUTHORIZED_CODE, NO_BODY);
        exchange.close();
    }

    private boolean knowsUser(HttpExchange exchange) throws IOException {
        var header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.regionMatches(true, 0, BASIC, 0, BASIC.length())) {
            return false;
        }
        String creds;
        try {
            creds = new String(Base64.getDecoder().decode(header.substring(BASIC.length()).strip()),
                StandardCharsets.UTF_8);
        } catch (IllegalArgumentException notBase64) {
            return false;
        }
        var colon = creds.indexOf(':');
        if (colon < 0) {
            return false;
        }
        try {
            return users.get(creds.substring(0, colon)).equals(creds.substring(colon + 1));
        } catch (NoSuchElementException unknownUser) {
            return false;
        }
    }
}
