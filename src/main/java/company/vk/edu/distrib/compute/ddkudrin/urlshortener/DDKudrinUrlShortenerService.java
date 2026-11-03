package company.vk.edu.distrib.compute.ddkudrin.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public class DDKudrinUrlShortenerService implements UrlShortenerService {

    private final HttpServer server;
    private final Dao<String> userDao;
    private final Lock lock = new ReentrantLock();
    private Dao<String> linkDao;
    private boolean configurationLocked;

    public DDKudrinUrlShortenerService(int port) throws IOException {
        this.linkDao = new DDKudrinPersistentDao(
                Path.of(System.getProperty("user.home"), "data", "links.wal")
        );
        this.userDao = new DDKudrinPersistentDao(
                Path.of(System.getProperty("user.home"), "data", "users.wal")
        );
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/v0/status", new DDKudrinStatusHandler());
        this.server.createContext("/internal/users", new DDKudrinUserHandler(userDao));
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        lock.lock();
        try {
            if (configurationLocked) {
                throw new IllegalStateException("Cannot replace links Dao after start or stop");
            }
            this.linkDao = Objects.requireNonNull(dao);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void start() {
        lock.lock();
        try {
            if (configurationLocked) {
                throw new IllegalStateException("Service already started or stopped");
            }
            configurationLocked = true;
            server.createContext(
                "/v0/links",
                new DDKudrinAuthMiddleware(new DDKudrinLinksHandler(linkDao), userDao)
            );
            server.createContext("/", new DDKudrinRedirectHandler(linkDao));
            server.start();
        } finally {
            lock.unlock();
        }
    }

    @Override 
    public void stop() {
        lock.lock();
        try {
            configurationLocked = true;
            Dao<String> links = linkDao;
            try (links; userDao) {
                server.stop(1);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot close service storage", e);
            }
        } finally {
            lock.unlock();
        }
    }
}
