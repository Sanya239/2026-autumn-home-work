package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.jspecify.annotations.Nullable;

public final class UrlShortenerServiceImpl implements UrlShortenerService {
    private static final String STATUS_PATH = "/v0/status";

    private final int port;
    private final Path dataDir;
    private final Lock lifecycle = new ReentrantLock();
    @Nullable
    private PersistentDao links;
    @Nullable
    private PersistentDao users;
    @Nullable
    private HttpServer server;
    @Nullable
    private ExecutorService executor;

    public UrlShortenerServiceImpl(int port, Path dataDir) {
        this.port = port;
        this.dataDir = dataDir;
    }

    @Override
    public void start() {
        lifecycle.lock();
        try {
            if (server != null) {
                throw new IllegalStateException("Service has already been started");
            }
            ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
            executor = workers;
            server = openAndCreateServer(workers);
        } finally {
            lifecycle.unlock();
        }
    }

    @Override
    public void stop() {
        lifecycle.lock();
        try {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            releaseResources();
        } finally {
            lifecycle.unlock();
        }
    }

    private HttpServer openAndCreateServer(ExecutorService workers) {
        try {
            PersistentDao linksDao = new PersistentDao(dataDir, "links.log");
            links = linksDao;
            PersistentDao usersDao = new PersistentDao(dataDir, "users.log");
            users = usersDao;
            return createServer(linksDao, usersDao, workers);
        } catch (IOException e) {
            releaseResources();
            throw new UncheckedIOException("Failed to start urlshortener service on port " + port, e);
        } catch (RuntimeException e) {
            releaseResources();
            throw e;
        }
    }

    private HttpServer createServer(PersistentDao linksDao, PersistentDao usersDao, ExecutorService workers)
        throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        LinksHandler linksHandler = new LinksHandler(linksDao, port);
        httpServer.createContext(STATUS_PATH, HttpUtils.safe(exchange -> status(exchange, linksDao, usersDao)));
        httpServer.createContext(LinksHandler.BASE_PATH, HttpUtils.safe(linksHandler))
            .setAuthenticator(new UsersAuthenticator(usersDao));
        httpServer.createContext(UsersHandler.PATH, HttpUtils.safe(new UsersHandler(usersDao)));
        httpServer.createContext("/", HttpUtils.safe(linksHandler::redirect));
        httpServer.setExecutor(workers);
        httpServer.start();
        return httpServer;
    }

    private void releaseResources() {
        if (executor != null) {
            executor.close();
            executor = null;
        }
        closeStorage();
    }

    private void closeStorage() {
        try (PersistentDao _ = links) {
            if (users != null) {
                users.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close storage", e);
        } finally {
            links = null;
            users = null;
        }
    }

    private static void status(HttpExchange exchange, PersistentDao linksDao, PersistentDao usersDao)
        throws IOException {
        if (!HttpUtils.accepts(exchange, STATUS_PATH, HttpUtils.GET)) {
            return;
        }
        boolean healthy = linksDao.isWritable() && usersDao.isWritable();
        HttpUtils.sendEmpty(exchange, healthy ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_UNAVAILABLE);
    }
}
