package company.vk.edu.distrib.compute.katyadoinikova.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

final class UrlShortenerHandler implements HttpHandler {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String LINKS = "/v0/links";
    private static final String USERS = "/internal/users";
    private static final String STATUS = "/v0/status";

    private final int port;
    private final LinkManager links;
    private final BasicAuthentication authentication;
    private final BooleanSupplier available;

    UrlShortenerHandler(
            int port, Dao<String> links, Dao<String> users, BooleanSupplier available) {
        this.port = port;
        this.links = new LinkManager(links);
        this.authentication = new BasicAuthentication(users);
        this.available = available;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Response response;
            try {
                response = route(exchange);
            } catch (NoSuchElementException e) {
                response = new Response(404, "");
            } catch (IllegalArgumentException e) {
                response = new Response(422, "");
            } catch (IOException e) {
                response = new Response(503, "");
            }
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);
            if (body.length != 0) {
                exchange.getResponseBody().write(body);
            }
        }
    }

    private Response route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if (STATUS.equals(path)) {
            return status(exchange, method);
        }
        if (USERS.equals(path)) {
            return registerUser(exchange, method);
        }
        if (isRedirectRequest(method, path)) {
            String id = path.substring(1);
            exchange.getResponseHeaders().set("Location", links.get(id));
            return new Response(301, "");
        }
        if (!authentication.authenticate(exchange.getRequestHeaders())) {
            exchange.getResponseHeaders().set("WWW-Authenticate", BasicAuthentication.CHALLENGE);
            return new Response(401, "");
        }
        if (LINKS.equals(path)) {
            if (!POST.equals(method)) {
                return methodNotAllowed(exchange, POST);
            }
            String id = links.create(readBody(exchange));
            return new Response(201, "http://localhost:" + port + '/' + id);
        }
        if (path.startsWith(LINKS + '/')) {
            String id = path.substring(LINKS.length() + 1);
            return accessLink(exchange, method, id);
        }
        return new Response(404, "");
    }

    private Response status(HttpExchange exchange, String method) {
        if (!GET.equals(method)) {
            return methodNotAllowed(exchange, GET);
        }
        return new Response(available.getAsBoolean() ? 200 : 503, "");
    }

    private Response registerUser(HttpExchange exchange, String method) throws IOException {
        if (!POST.equals(method)) {
            return methodNotAllowed(exchange, POST);
        }
        authentication.register(readBody(exchange));
        return new Response(200, "");
    }

    private Response accessLink(HttpExchange exchange, String method, String id)
            throws IOException {
        return switch (method) {
            case GET -> new Response(200, links.get(id));
            case PUT -> {
                links.update(id, readBody(exchange));
                yield new Response(200, "");
            }
            case DELETE -> {
                links.delete(id);
                yield new Response(202, "");
            }
            default -> methodNotAllowed(exchange, "GET, PUT, DELETE");
        };
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static boolean isRedirectPath(String path) {
        return path.startsWith("/") && path.length() > 1 && path.indexOf('/', 1) < 0;
    }

    private static boolean isRedirectRequest(String method, String path) {
        return GET.equals(method) && isRedirectPath(path);
    }

    private static Response methodNotAllowed(HttpExchange exchange, String allowed) {
        exchange.getResponseHeaders().set("Allow", allowed);
        return new Response(405, "");
    }

    private record Response(int status, String body) {
    }
}
