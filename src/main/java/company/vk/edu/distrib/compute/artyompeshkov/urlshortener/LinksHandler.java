package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.NO_BODY;
import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendMethodNotAllowed;
import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendText;
import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendUnauthorized;

class LinksHandler extends BaseHandler {
    static final String PATH = "/v0/links";
    private static final String LINK_PATH_PREFIX = PATH + "/";

    private final Dao<String> links;
    private final SimpleAuth auth;
    private final String baseUrl;
    private final LinkIdGenerator idGenerator = new LinkIdGenerator();

    LinksHandler(Dao<String> links, SimpleAuth auth, String baseUrl) {
        super();
        this.links = links;
        this.auth = auth;
        this.baseUrl = baseUrl;
    }

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException {
        if (!auth.isAuthorized(exchange)) {
            sendUnauthorized(exchange);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (PATH.equals(path)) {
            handleCreate(exchange);
        } else if (path.startsWith(LINK_PATH_PREFIX)) {
            handleIndividualOps(exchange, path.substring(LINK_PATH_PREFIX.length()));
        } else {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, NO_BODY);
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        if (!POST.equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, POST);
            return;
        }
        createLink(exchange);
    }

    private void handleIndividualOps(HttpExchange exchange, String rawId) throws IOException {
        switch (exchange.getRequestMethod()) {
            case GET -> getLink(exchange, new LinkId(rawId));
            case PUT -> updateLink(exchange, new LinkId(rawId));
            case DELETE -> deleteLink(exchange, new LinkId(rawId));
            default -> sendMethodNotAllowed(exchange, "GET, PUT, DELETE");
        }
    }

    private void createLink(HttpExchange exchange) throws IOException {
        String link = readLink(exchange);
        LinkId id = idGenerator.generate();
        links.upsert(id.value(), link);
        sendText(exchange, HttpURLConnection.HTTP_CREATED, baseUrl + id.value());
    }

    private void getLink(HttpExchange exchange, LinkId id) throws IOException {
        sendText(exchange, HttpURLConnection.HTTP_OK, links.get(id.value()));
    }

    private void updateLink(HttpExchange exchange, LinkId id) throws IOException {
        String link = readLink(exchange);
        links.get(id.value());
        links.upsert(id.value(), link);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_BODY);
    }

    private void deleteLink(HttpExchange exchange, LinkId id) throws IOException {
        links.delete(id.value());
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_ACCEPTED, NO_BODY);
    }

    private static String readLink(HttpExchange exchange) throws IOException {
        URI uri = URI.create(HttpUtils.readBody(exchange));
        boolean isHttp = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        if (!isHttp || uri.getHost() == null) {
            throw new IllegalArgumentException("Not an http(s) link: " + uri);
        }
        return uri.toASCIIString();
    }
}
