package company.vk.edu.distrib.compute.nosorozhek.kv.handlers;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;

public class DeleteEntryHandler implements Handler {
    private final Dao<byte[]> dao;

    public DeleteEntryHandler(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange, RouteParameters parameters) throws IOException {
        String entryId = RouteParameters.requiredId(exchange);

        dao.delete(entryId);

        exchange.sendResponseHeaders(202, -1);
        exchange.close();
    }
}
