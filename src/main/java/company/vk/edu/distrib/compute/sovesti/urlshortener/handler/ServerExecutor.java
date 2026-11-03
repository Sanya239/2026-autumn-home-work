package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public final class ServerExecutor {

    private final HttpServer server;

    public ServerExecutor(HttpServer server) {
        this.server = Objects.requireNonNull(server);
    }

    public void fromEnv() {
        server.setExecutor(Optional.ofNullable(System.getenv("SERVICE_THREADS"))
            .flatMap(this::parse)
            .map(Number::intValue)
            .map(Executors::newFixedThreadPool)
            .orElseGet(Executors::newSingleThreadExecutor));
    }

    private Optional<Number> parse(String raw) {
        return Optional.ofNullable(new DecimalFormat().parse(raw, new ParsePosition(0)));
    }
}
