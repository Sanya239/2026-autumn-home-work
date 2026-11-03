package company.vk.edu.distrib.compute.aartchik.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

public final class UrlShortenerHandler implements HttpHandler {
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String STATUS_PATH = "/v0/status";
    private static final String USERS_PATH = "/internal/users";
    private static final String LINKS_PATH = "/v0/links";
    private static final String LINK_PREFIX = LINKS_PATH + "/";

    private final LinkStore links;
    private final BasicAuthentication authentication;
    private final String shortLinkPrefix;
    private final BooleanSupplier storageAvailable;

    public UrlShortenerHandler(
            int port,
            LinkStore links,
            BasicAuthentication authentication,
            BooleanSupplier storageAvailable) {
        this.links = links;
        this.authentication = authentication;
        shortLinkPrefix = "http://localhost:" + port + "/";
        this.storageAvailable = storageAvailable;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            HttpResponseData response = process(exchange);
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            if (body.length == 0) {
                exchange.sendResponseHeaders(response.status(), -1);
            } else {
                exchange.sendResponseHeaders(response.status(), body.length);
                exchange.getResponseBody().write(body);
            }
        }
    }

    private HttpResponseData process(HttpExchange exchange) {
        try {
            return route(exchange);
        } catch (BodyTooLargeException tooLarge) {
            exchange.getResponseHeaders().set("Connection", "close");
            return new HttpResponseData(413);
        } catch (NoSuchElementException missing) {
            return new HttpResponseData(404);
        } catch (IllegalArgumentException | CharacterCodingException invalidRequest) {
            return new HttpResponseData(422);
        } catch (IOException storageFailure) {
            return new HttpResponseData(503);
        } catch (RuntimeException unexpectedFailure) {
            return new HttpResponseData(500);
        }
    }

    private HttpResponseData route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        if (STATUS_PATH.equals(path)) {
            return status(exchange);
        }
        if (USERS_PATH.equals(path)) {
            return registerUser(exchange);
        }
        if (isRedirectRequest(exchange, path)) {
            return redirect(exchange, path.substring(1));
        }
        if (!authentication.authenticate(exchange.getRequestHeaders())) {
            exchange.getResponseHeaders().set("WWW-Authenticate", BasicAuthentication.CHALLENGE);
            return new HttpResponseData(401);
        }
        if (LINKS_PATH.equals(path)) {
            return create(exchange);
        }
        if (path.startsWith(LINK_PREFIX)) {
            return accessLink(exchange, path.substring(LINK_PREFIX.length()));
        }
        return new HttpResponseData(404);
    }

    private HttpResponseData status(HttpExchange exchange) {
        if (!GET.equals(exchange.getRequestMethod())) {
            return methodNotAllowed(exchange, GET);
        }
        return new HttpResponseData(storageAvailable.getAsBoolean() ? 200 : 503);
    }

    private HttpResponseData create(HttpExchange exchange) throws IOException {
        if (!POST.equals(exchange.getRequestMethod())) {
            return methodNotAllowed(exchange, POST);
        }
        String id = links.create(readBody(exchange));
        return new HttpResponseData(201, shortLinkPrefix + id);
    }

    private HttpResponseData registerUser(HttpExchange exchange) throws IOException {
        if (!POST.equals(exchange.getRequestMethod())) {
            return methodNotAllowed(exchange, POST);
        }
        authentication.register(readBody(exchange));
        return new HttpResponseData(200);
    }

    private HttpResponseData accessLink(HttpExchange exchange, String id) throws IOException {
        return switch (exchange.getRequestMethod()) {
            case GET -> new HttpResponseData(200, links.get(id));
            case PUT -> {
                links.update(id, readBody(exchange));
                yield new HttpResponseData(200);
            }
            case DELETE -> {
                links.delete(id);
                yield new HttpResponseData(202);
            }
            default -> methodNotAllowed(exchange, GET + ", " + PUT + ", " + DELETE);
        };
    }

    private HttpResponseData redirect(HttpExchange exchange, String id) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            return methodNotAllowed(exchange, GET);
        }
        String longLink = links.get(id);
        exchange.getResponseHeaders().set("Location", URI.create(longLink).toASCIIString());
        return new HttpResponseData(301);
    }

    private static boolean isRedirectRequest(HttpExchange exchange, String path) {
        return GET.equals(exchange.getRequestMethod())
                && path.startsWith("/")
                && path.indexOf('/', 1) == -1;
    }

    private static HttpResponseData methodNotAllowed(HttpExchange exchange, String allowedMethods) {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        return new HttpResponseData(405);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null && Long.parseLong(contentLength) > MAX_BODY_BYTES) {
            throw new BodyTooLargeException();
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            throw new BodyTooLargeException();
        }
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }
}
