package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UrlShortenerHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerHandler.class);

    private final BasicAuthenticator authenticator;
    private final LinkRequests links;

    UrlShortenerHandler(int port, Dao<String> links, Dao<String> users) {
        authenticator = new BasicAuthenticator(users);
        this.links = new LinkRequests(port, links);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try (exchange) {
            try {
                route(exchange);
            } catch (Exception ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), ex);
                }
                HttpReplies.sendServerError(exchange);
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = HttpReplies.requestPath(exchange);
        if (isUnauthorized(exchange, method, path)) {
            HttpReplies.sendUnauthorized(exchange);
            return;
        }
        if (dispatch(exchange, method, path)) {
            return;
        }
        HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
    }

    private boolean isUnauthorized(HttpExchange exchange, String method, String path) throws IOException {
        return !RequestChecks.isPublic(method, path) && !authenticator.authorized(exchange);
    }

    private boolean dispatch(HttpExchange exchange, String method, String path) throws IOException {
        return serveStatus(exchange, method, path)
            || serveUsers(exchange, method, path)
            || serveLinks(exchange, method, path);
    }

    private boolean serveStatus(HttpExchange exchange, String method, String path) throws IOException {
        if (RequestChecks.GET.equals(method) && RequestChecks.STATUS_PATH.equals(path)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.OK);
            return true;
        }
        return false;
    }

    private boolean serveUsers(HttpExchange exchange, String method, String path) throws IOException {
        if (RequestChecks.POST.equals(method) && RequestChecks.USERS_PATH.equals(path)) {
            authenticator.createUser(exchange);
            return true;
        }
        return false;
    }

    private boolean serveLinks(HttpExchange exchange, String method, String path) throws IOException {
        if (RequestChecks.POST.equals(method) && RequestChecks.LINKS_PATH.equals(path)) {
            links.create(exchange);
            return true;
        }
        if (path.startsWith(RequestChecks.LINKS_PREFIX)) {
            links.handleItem(exchange, method, path.substring(RequestChecks.LINKS_PREFIX.length()));
            return true;
        }
        if (RequestChecks.isRedirectPath(method, path)) {
            links.redirect(exchange, path.substring(1));
            return true;
        }
        return false;
    }
}
