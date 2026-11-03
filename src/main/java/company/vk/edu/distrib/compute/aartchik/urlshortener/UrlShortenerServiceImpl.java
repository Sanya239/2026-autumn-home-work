package company.vk.edu.distrib.compute.aartchik.urlshortener;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UrlShortenerServiceImpl implements UrlShortenerService {
    private final int port;
    private final Path dataDirectory;
    private final Lock lifecycleLock = new ReentrantLock();

    private boolean started;
    private boolean stopped;
    @Nullable private HttpServer server;
    @Nullable private ExecutorService executor;
    @Nullable private PersistentStringDao linkDao;
    @Nullable private PersistentStringDao userDao;

    public UrlShortenerServiceImpl(int port, Path dataDirectory) {
        this.port = port;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void start() {
        lifecycleLock.lock();
        try {
            if (started) {
                throw new IllegalStateException("Service can only be started once");
            }
            started = true;
            startResources();
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            if (!started || stopped) {
                return;
            }
            stopped = true;
            closeResources();
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void startResources() {
        try {
            PersistentStringDao links = new PersistentStringDao(dataDirectory.resolve("links.log"));
            linkDao = links;
            PersistentStringDao users = new PersistentStringDao(dataDirectory.resolve("users.log"));
            userDao = users;
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server = httpServer;
            ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
            executor = workers;
            httpServer.setExecutor(workers);
            httpServer.createContext(
                    "/",
                    new UrlShortenerHandler(
                            port,
                            new LinkStore(links),
                            new BasicAuthentication(users),
                            () -> links.isAvailable() && users.isAvailable()));
            httpServer.start();
        } catch (IOException | RuntimeException failure) {
            try {
                closeResources();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw new IllegalStateException("Unable to start URL shortener", failure);
        }
    }

    private void closeResources() {
        HttpServer httpServer = server;
        ExecutorService workers = executor;
        PersistentStringDao links = linkDao;
        PersistentStringDao users = userDao;
        try (links; users) {
            if (httpServer != null) {
                httpServer.stop(0);
            }
            if (workers != null) {
                workers.close();
            }
        } catch (IOException closeFailure) {
            throw new UncheckedIOException("Unable to close persistent storage", closeFailure);
        }
    }
}
