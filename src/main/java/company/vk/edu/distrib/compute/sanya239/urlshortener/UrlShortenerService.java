package company.vk.edu.distrib.compute.sanya239.urlshortener;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;

public class UrlShortenerService implements company.vk.edu.distrib.compute.urlshortener.UrlShortenerService {
    private static final Path DATA_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"), "sanya239-url-shortener"
    );

    private final int port;
    private HttpServer server;
    private company.vk.edu.distrib.compute.Dao<String> linksDao;
    private company.vk.edu.distrib.compute.Dao<String> accountsDao;

    public UrlShortenerService(int port) {
        this.port = port;
    }

    @Override
    public void setLinksDao(company.vk.edu.distrib.compute.Dao<String> dao) {
        if (server != null) {
            throw new IllegalStateException("Service is already started or stopped");
        }
        linksDao = Objects.requireNonNull(dao);
    }

    @Override
    public void start() {
        if (server != null) {
            throw new IllegalStateException("Service is already started");
        }
        try {
            accountsDao = new Dao(DATA_DIRECTORY.resolve("accounts").toString());
            if (linksDao == null) {
                linksDao = new Dao(DATA_DIRECTORY.resolve("links").toString());
            }
            server = HttpServer.create();
            server.bind(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", new HttpHandler(linksDao, accountsDao));
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        server.stop(0);
        try {
            accountsDao.close();
            linksDao.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
