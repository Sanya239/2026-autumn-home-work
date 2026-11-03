package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpExchange;

import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.KeyValuePair;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.ExchangeAttribute.QueryAttribute;

public final class HttpQuery implements Consumer<HttpExchange> {

    @Override
    public void accept(HttpExchange exchange) {
        new ExchangeAttributes(exchange).put(new QueryAttribute(), new HttpQueryFields(parse(exchange)));
    }

    private Map<String, String> parse(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestURI().getQuery())
            .stream()
            .map(q -> q.split("&"))
            .flatMap(Stream::of)
            .map(part -> new KeyValuePair(part, '='))
            .filter(KeyValuePair::valid)
            .collect(Collectors.toMap(KeyValuePair::key, KeyValuePair::value));
    }

}
