package company.vk.edu.distrib.compute.netheer.urlshortener;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

public class UrlShortenerServiceImplementation implements UrlShortenerService {
    private static final int STOP_DELAY_SECONDS = 1;

    private final Dao<String> linksDao;
    private final Dao<String> usersDao;
    private final HttpServer server;

    public UrlShortenerServiceImplementation(int port, Dao<String> linksDao, Dao<String> usersDao)
            throws IOException {
        this.linksDao = linksDao;
        this.usersDao = usersDao;

        UrlShortenerAuth auth = new UrlShortenerAuth(usersDao);
        UrlShortenerHandlers handlers = new UrlShortenerHandlers(port, linksDao, auth);

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v0/status", handlers::handleStatus);
        server.createContext("/v0/links", handlers::handleLinks);
        server.createContext("/internal/users", auth::handleUsers);
        server.createContext("/", handlers::handleRedirect);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        try (linksDao; usersDao) {
            server.stop(STOP_DELAY_SECONDS);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
