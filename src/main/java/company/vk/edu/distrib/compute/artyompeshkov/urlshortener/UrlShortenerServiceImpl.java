package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public class UrlShortenerServiceImpl implements UrlShortenerService {
    private final int port;
    private final Dao<String> users;
    private final HttpServer server;
    private Dao<String> links;
    private boolean started;

    public UrlShortenerServiceImpl(int port, Dao<String> links, Dao<String> users) throws IOException {
        this.port = port;
        this.links = links;
        this.users = users;
        this.server = HttpServer.create();
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        if (started) {
            throw new IllegalStateException("Links storage can only be set before start");
        }
        links = dao;
    }

    @Override
    public void start() {
        started = true;
        String baseUrl = "http://localhost:" + port + "/";
        server.createContext(StatusHandler.PATH, new StatusHandler());
        server.createContext(LinksHandler.PATH, new LinksHandler(links, new SimpleAuth(users), baseUrl));
        server.createContext(UsersHandler.PATH, new UsersHandler(users));
        server.createContext(RedirectHandler.PATH, new RedirectHandler(links));
        try {
            server.bind(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to bind to port " + port, e);
        }
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
        try {
            users.close();
            links.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close the storage", e);
        }
    }
}
