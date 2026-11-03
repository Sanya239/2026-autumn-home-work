package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UrlShortenerHttpService implements UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerHttpService.class);

    private final int port;
    private final HttpServer server;
    private final Dao<String> links;
    private final Dao<String> users;
    private boolean started;

    public UrlShortenerHttpService(int port, Dao<String> links, Dao<String> users) throws IOException {
        this.port = port;
        this.links = links;
        this.users = users;
        server = HttpServer.create();
        server.createContext("/", new UrlShortenerHandler(port, links, users));
    }

    @Override
    public void start() {
        if (started) {
            throw new IllegalStateException("Service is already started");
        }
        try {
            server.bind(new InetSocketAddress("localhost", port), 0);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start URL shortener", ex);
        }
        started = true;
        log.info("URL shortener is listening on port {}", port);
    }

    @Override
    public void stop() {
        try {
            if (started) {
                server.stop(5);
            }
        } finally {
            closeDao(links);
            closeDao(users);
            started = false;
        }
        log.info("Stopped URL shortener on port {}", port);
    }

    private static void closeDao(Closeable dao) {
        try {
            dao.close();
        } catch (IOException ex) {
            log.warn("Failed to close storage", ex);
        }
    }
}
