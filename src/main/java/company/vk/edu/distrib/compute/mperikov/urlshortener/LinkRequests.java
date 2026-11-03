package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

final class LinkRequests {
    private final int port;
    private final Dao<String> links;

    LinkRequests(int port, Dao<String> links) {
        this.port = port;
        this.links = links;
    }

    void create(HttpExchange exchange) throws IOException {
        Optional<String> link = readValidLink(exchange);
        if (link.isEmpty()) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        String id = allocateId();
        links.upsert(id, link.orElseThrow());
        HttpReplies.sendText(exchange, HttpStatus.CREATED, "http://localhost:%d/%s".formatted(port, id));
    }

    void handleItem(HttpExchange exchange, String method, String id) throws IOException {
        switch (method) {
            case "GET" -> read(exchange, id);
            case "PUT" -> update(exchange, id);
            case "DELETE" -> delete(exchange, id);
            default -> HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
        }
    }

    void redirect(HttpExchange exchange, String id) throws IOException {
        if (!LinkIds.isValid(id)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        try {
            exchange.getResponseHeaders().set("Location", links.get(id));
            HttpReplies.sendEmpty(exchange, HttpStatus.MOVED);
        } catch (NoSuchElementException expected) {
            HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
        }
    }

    private void read(HttpExchange exchange, String id) throws IOException {
        if (!LinkIds.isValid(id)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        try {
            HttpReplies.sendText(exchange, HttpStatus.OK, links.get(id));
        } catch (NoSuchElementException expected) {
            HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
        }
    }

    private void update(HttpExchange exchange, String id) throws IOException {
        if (!LinkIds.isValid(id)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        Optional<String> link = readValidLink(exchange);
        if (link.isEmpty()) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        if (!exists(id)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
            return;
        }
        links.upsert(id, link.orElseThrow());
        HttpReplies.sendEmpty(exchange, HttpStatus.OK);
    }

    private void delete(HttpExchange exchange, String id) throws IOException {
        if (!LinkIds.isValid(id)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.UNPROCESSABLE);
            return;
        }
        links.delete(id);
        HttpReplies.sendEmpty(exchange, HttpStatus.ACCEPTED);
    }

    private Optional<String> readValidLink(HttpExchange exchange) throws IOException {
        if (!HttpReplies.isHtmlUtf8(exchange)) {
            return Optional.empty();
        }
        String link = HttpReplies.readBody(exchange);
        if (!RequestChecks.isValidLink(link)) {
            return Optional.empty();
        }
        return Optional.of(link);
    }

    private String allocateId() throws IOException {
        for (int attempt = 0; attempt < 8; attempt++) {
            String id = LinkIds.randomId();
            if (!exists(id)) {
                return id;
            }
        }
        throw new IOException("Could not allocate a short id");
    }

    private boolean exists(String id) throws IOException {
        try {
            links.get(id);
            return true;
        } catch (NoSuchElementException expected) {
            return false;
        }
    }
}
