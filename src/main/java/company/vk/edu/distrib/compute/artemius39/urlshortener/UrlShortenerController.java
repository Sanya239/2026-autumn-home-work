package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public class UrlShortenerController implements UrlShortenerService {
    public static final String LINKS_URI = "/v0/links/";
    private static final String ROOT_URI = "/";
    private static final int SERVER_STOP_DELAY = 1;

    private final HttpServer server;
    private final UrlShortenerHandler handler;

    public UrlShortenerController(InetSocketAddress address, UrlShortenerHandler handler) throws IOException {
        this.handler = handler;
        this.server = createServer(address);
    }

    private HttpServer createServer(InetSocketAddress address) throws IOException {
        HttpServer server = HttpServer.create(address, 0);
        setStatusMappings(server);
        setLinksMappings(server);
        setAuthMappings(server);
        return server;
    }

    private void setStatusMappings(HttpServer server) {
        server.createContext(
            "/v0/status", exchange -> {
                try (exchange) {
                    if (HttpUtils.Methods.GET.equals(exchange.getRequestMethod())) {
                        handler.getStatus(exchange);
                    } else {
                        HttpUtils.methodNotAllowed(exchange);
                    }
                }
            }
        );
    }

    private void setLinksMappings(HttpServer server) {
        server.createContext(
            LINKS_URI, exchange -> {
                try (exchange) {
                    switch (exchange.getRequestMethod()) {
                        case HttpUtils.Methods.GET -> handler.getLink(exchange);
                        case HttpUtils.Methods.PUT -> handler.changeLink(exchange);
                        case HttpUtils.Methods.DELETE -> handler.deleteLink(exchange);
                        default -> HttpUtils.methodNotAllowed(exchange);
                    }
                }
            }
        );
        server.createContext(
            "/v0/links", exchange -> {
                try (exchange) {
                    if (HttpUtils.Methods.POST.equals(exchange.getRequestMethod())) {
                        handler.createLink(exchange);
                    } else {
                        HttpUtils.methodNotAllowed(exchange);
                    }
                }
            }
        );
        server.createContext(
            ROOT_URI, exchange -> {
                try (exchange) {
                    if (HttpUtils.Methods.GET.equals(exchange.getRequestMethod())) {
                        handler.redirectLink(exchange);
                    } else {
                        HttpUtils.methodNotAllowed(exchange);
                    }
                }
            }
        );
    }

    private void setAuthMappings(HttpServer server) {
        server.createContext(
            "/internal/users", exchange -> {
                try (exchange) {
                    if (HttpUtils.Methods.POST.equals(exchange.getRequestMethod())) {
                        handler.registerUser(exchange);
                    } else {
                        HttpUtils.methodNotAllowed(exchange);
                    }
                }
            }
        );
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        try {
            server.stop(SERVER_STOP_DELAY);
            handler.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
