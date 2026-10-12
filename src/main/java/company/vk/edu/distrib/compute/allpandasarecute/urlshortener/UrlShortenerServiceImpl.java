package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public class UrlShortenerServiceImpl implements UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);

    private static final String STATUS_PATH = "/v0/status";
    private static final String LINKS_PATH = "/v0/links";
    private static final String USERS_PATH = "/internal/users";
    private static final int WORKER_THREADS = 4;

    private final int port;
    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean();

    public UrlShortenerServiceImpl(int port, Dao<String> links, Dao<String> users) throws IOException {
        this.port = port;
        this.executor = Executors.newFixedThreadPool(WORKER_THREADS);
        this.server = HttpServer.create();
        server.setExecutor(executor);
        server.createContext(STATUS_PATH, new StatusHandler());
        server.createContext(USERS_PATH, new UsersHandler(users));
        server.createContext(LINKS_PATH, new LinksHandler(port, links, new BasicAuthenticator(users)));
        server.createContext("/", new RedirectHandler(links));
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Service is already started");
        }
        try {
            server.bind(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Can not bind to port " + port, e);
        }
        server.start();
        log.info("URL shortener is listening on port {}", port);
    }

    @Override
    public void stop() {
        if (!started.getAndSet(false)) {
            return;
        }
        server.stop(1);
        executor.shutdownNow();
        log.info("URL shortener on port {} is stopped", port);
    }
}
