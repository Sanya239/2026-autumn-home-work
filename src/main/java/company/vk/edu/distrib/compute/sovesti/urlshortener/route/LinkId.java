package company.vk.edu.distrib.compute.sovesti.urlshortener.route;

import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.ExchangeAttribute;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.ExchangeAttributes;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.PathElements;

final class LinkId implements ExchangeAttribute<String> {

    @Override
    public String key() {
        return "link_id";
    }

    @Override
    public Class<String> type() {
        return String.class;
    }

    String find(HttpExchange exchange) {
        return new RandomId().throwIfInvalid(new ExchangeAttributes(exchange).find(this).get());
    }

    void put(String prefix, HttpExchange exchange) {
        fromQuery(exchange)
            .or(() -> fromPath(prefix, exchange))
            .ifPresent(id -> new ExchangeAttributes(exchange).put(this, id));
    }

    private Optional<String> fromQuery(HttpExchange exchange) {
        return new ExchangeAttributes(exchange).find(new QueryAttribute()).flatMap(q -> q.get("id"));
    }

    private Optional<String> fromPath(String prefix, HttpExchange exchange) {
        return new PathElements(prefix).apply(exchange).findFirst();
    }
}
