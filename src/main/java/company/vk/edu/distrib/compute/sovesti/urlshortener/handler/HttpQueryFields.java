package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HttpQueryFields {

    private final Map<String, String> fields;

    public HttpQueryFields(Map<String, String> fields) {
        this.fields = Objects.requireNonNull(fields);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(fields.get(key));
    }
}
