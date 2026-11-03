package company.vk.edu.distrib.compute.nosorozhek.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.Handler;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.HttpMethod;
import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.RouteParameters;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Route(HttpMethod method, Pattern pattern, Handler handler) {
    static Route create(HttpMethod method, String pattern, Handler handler) {
        return new Route(method, Pattern.compile(pattern), handler);
    }

    public Optional<RouteParameters> matches(HttpMethod requestMethod, String requestPath) {
        if (requestMethod != method) {
            return Optional.empty();
        }

        Matcher matcher = pattern.matcher(requestPath);
        if (matcher.matches()) {
            return Optional.of(new RouteParameters(matcher));
        }
        return Optional.empty();
    }

    public void handle(HttpExchange httpExchange, RouteParameters parameters) throws IOException {
        handler.handle(httpExchange, parameters);
    }
}
