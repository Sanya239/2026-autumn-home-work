package company.vk.edu.distrib.compute.flighen.urlshortener;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fl1ghenUrlShortenerService implements UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(Fl1ghenUrlShortenerService.class);

    private final HttpServer server;
    private final Dao<String> shortLinksDao;
    private final Dao<String> usersDao;

    public Fl1ghenUrlShortenerService(int port) throws IOException {
        Path storageDir = Path.of(
                System.getProperty("java.io.tmpdir"),
                "fl1ghen-urlshortener"
        );

        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to create temp directory {}", storageDir, e);
            }
            throw e;
        }

        usersDao = new PersistentDao(
                storageDir.resolve("usersDb.txt")
        );
        shortLinksDao = new PersistentDao(
                storageDir.resolve("shortLinks.txt")
        );

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v0/status", new GetStatusHandler());
        server.createContext("/", new RedirectHandler(shortLinksDao));
        server.createContext("/internal/users", new UsersHandler(usersDao));
        HttpContext ctxLink = server.createContext("/v0/links", new LinkHandler(shortLinksDao, port));

        AuthFilter authFilter = new AuthFilter(usersDao);
        ctxLink.getFilters().add(authFilter);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);

        try {
            shortLinksDao.close();
        } catch (IOException e) {
            log.error("Failed to close shortLinksDao", e);
        }

        try {
            usersDao.close();
        } catch (IOException e) {
            log.error("Failed to close usersDao", e);
        }
    }
}
