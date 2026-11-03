package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import static com.sun.net.httpserver.Filter.beforeHandler;

import java.util.Objects;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.sovesti.urlshortener.auth.Authentication;
import company.vk.edu.distrib.compute.sovesti.urlshortener.auth.AuthenticationScheme;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.HttpRoute;

public final class HttpContexts {

    private final HttpServer server;
    private final AuthenticationScheme authentication;

    public HttpContexts(HttpServer server, AuthenticationScheme authentication) {
        this.server = Objects.requireNonNull(server);
        this.authentication = Objects.requireNonNull(authentication);
    }

    public void createAuthenticated(HttpRoute route) {
        create(route).getFilters().add(new Authentication(authentication));
    }

    public HttpContext create(HttpRoute route) {
        HttpContext context = server.createContext(route.prefix(), route.handler());
        context.getFilters().add(beforeHandler("Parse request query", new HttpQuery()));
        context.getFilters().add(beforeHandler("Parse request path", route::parsePath));
        context.getFilters().add(new WriteResponse(authentication));
        return context;
    }
}
