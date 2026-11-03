package company.vk.edu.distrib.compute.sh4rrkyyyy.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AuthHandler implements HttpHandler {
    private final HttpHandler handler;
    private final DaoString usersDao;
    private static final String AUTH_HEADER = "Authorization";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String BASIC_AUTH_CHALLENGE = "Basic realm=\"url-shortener\"";

    public AuthHandler(HttpHandler handler, DaoString usersDao) {
        this.handler = handler;
        this.usersDao = usersDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        boolean isAuth;
        try {
            isAuth = isAuth(exchange);
        } catch (Exception e) {
            isAuth = false;
        }
        if (!isAuth) {
            exchange.getResponseHeaders().set(WWW_AUTHENTICATE, BASIC_AUTH_CHALLENGE);
            HttpUtils.sendEmptyRsp(exchange, HttpStatus.UNAUTHORIZED);
            return;
        }
        handler.handle(exchange);
    }

    private boolean isAuth(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst(AUTH_HEADER);
        BasicCredentials credentials = BasicCredentials.fromAuthHeader(authHeader);
        return credentials != null && usersDao.get(credentials.username()).equals(credentials.password());
    }
}
