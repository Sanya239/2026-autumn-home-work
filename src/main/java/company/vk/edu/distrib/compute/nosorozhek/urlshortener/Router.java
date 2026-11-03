package company.vk.edu.distrib.compute.nosorozhek.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication.InvalidCredentialsException;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication.UnauthorizedException;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.Handler;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.HttpMethod;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.RouteParameters;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation.InvalidLinkException;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation.InvalidLinkIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Router implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final List<Route> routes = new ArrayList<>();

    private HttpMethod parseMethod(String method) {
        return switch (method) {
            case "GET" -> HttpMethod.GET;
            case "POST" -> HttpMethod.POST;
            case "PUT" -> HttpMethod.PUT;
            case "DELETE" -> HttpMethod.DELETE;
            default -> null;
        };
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        HttpMethod method = parseMethod(requestMethod);
        if (method == null) {
            log.warn("Unexpected request method: {}", requestMethod);
        }

        String path = exchange.getRequestURI().getPath();
        try {
            for (Route route : routes) {
                Optional<RouteParameters> parameters = route.matches(method, path);
                if (parameters.isPresent()) {
                    route.handle(exchange, parameters.get());
                    return;
                }
            }
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        } catch (InvalidLinkException | InvalidLinkIdException | InvalidCredentialsException e) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        } catch (UnauthorizedException e) {
            exchange.getResponseHeaders().set(
                    "WWW-Authenticate",
                    "Basic realm=\"url-shortener\", charset=\"UTF-8\""
            );
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        }
    }

    public Router add(HttpMethod method, String pattern, Handler handler) {
        routes.add(Route.create(method, pattern, handler));
        return this;
    }
}
