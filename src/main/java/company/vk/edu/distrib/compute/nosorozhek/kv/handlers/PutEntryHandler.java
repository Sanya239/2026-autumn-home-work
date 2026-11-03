package company.vk.edu.distrib.compute.nosorozhek.kv.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;

public class PutEntryHandler implements Handler {
    private static final int BODY_SIZE_LIMIT = 8192;

    private final Dao<byte[]> dao;

    public PutEntryHandler(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String entryId = RouteParameters.requiredId(exchange);

        byte[] entry = exchange.getRequestBody().readNBytes(BODY_SIZE_LIMIT + 1);
        if (entry.length > BODY_SIZE_LIMIT) {
            exchange.sendResponseHeaders(413, -1);
            exchange.close();
            return;
        }

        dao.upsert(entryId, entry);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(201, -1);
        exchange.close();
    }
}
