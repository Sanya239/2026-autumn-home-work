package company.vk.edu.distrib.compute.nosorozhek.kv.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;

public class GetEntryHandler implements Handler {
    private final Dao<byte[]> dao;

    public GetEntryHandler(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String entryId = RouteParameters.requiredId(exchange);

        byte[] entry = dao.get(entryId);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, entry.length);
        exchange.getResponseBody().write(entry);
        exchange.close();
    }
}
