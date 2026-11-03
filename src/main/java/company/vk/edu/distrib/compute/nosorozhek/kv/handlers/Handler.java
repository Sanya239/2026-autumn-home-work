package company.vk.edu.distrib.compute.nosorozhek.kv.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(HttpExchange exchange, RouteParameters parameters) throws IOException;
}

