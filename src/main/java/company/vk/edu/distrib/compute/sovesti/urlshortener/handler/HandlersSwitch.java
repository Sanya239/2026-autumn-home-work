package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Request;

import company.vk.edu.distrib.compute.sovesti.urlshortener.http.MethodConstants;
import company.vk.edu.distrib.compute.sovesti.urlshortener.http.StatusCodeConstants;

public final class HandlersSwitch implements HttpHandler {

    private final Map<Predicate<Request>, HttpHandler> handlers = new ConcurrentHashMap<>();

    public HandlersSwitch withPost(HttpHandler handler) {
        return with(MethodConstants.POST, handler);
    }

    public HandlersSwitch withGet(HttpHandler handler) {
        return with(MethodConstants.GET, handler);
    }

    public HandlersSwitch withPut(HttpHandler handler) {
        return with(MethodConstants.PUT, handler);
    }

    public HandlersSwitch withDelete(HttpHandler handler) {
        return with(MethodConstants.DELETE, handler);
    }

    public HandlersSwitch with(String method, HttpHandler handler) {
        return with(new MethodIs(method), handler);
    }

    public HandlersSwitch with(Predicate<Request> condition, HttpHandler handler) {
        handlers.put(condition, handler);
        return this;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        handlers.entrySet()
            .stream()
            .filter(entry -> entry.getKey().test(exchange))
            .map(Entry::getValue)
            .findFirst()
            .orElseGet(() -> new Response(StatusCodeConstants.METHOD_NOT_ALLOWED))
            .handle(exchange);
    }

}
