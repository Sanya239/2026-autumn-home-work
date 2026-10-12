package company.vk.edu.distrib.compute.katyadoinikova.urlshortener;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UrlShortenerServiceImpl implements UrlShortenerService {
    private static final Path STORAGE_ROOT =
            Path.of(System.getProperty("java.io.tmpdir"), "katyadoinikova-url-shortener");

    private final int port;
    private final Lock lifecycleLock = new ReentrantLock();
    @Nullable private HttpServer server;
    @Nullable private ExecutorService executor;
    @Nullable private FileDao links;
    @Nullable private FileDao users;
    private boolean started;
    private boolean stopped;

    public UrlShortenerServiceImpl(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        lifecycleLock.lock();
        try {
            if (started) {
                throw new IllegalStateException("Service has already been started");
            }
            started = true;
            try {
                Path storage = STORAGE_ROOT.resolve(Integer.toString(port));
                FileDao linkDao = new FileDao(storage.resolve("links"));
                links = linkDao;
                FileDao userDao = new FileDao(storage.resolve("users"));
                users = userDao;
                HttpServer httpServer =
                        HttpServer.create(new InetSocketAddress("localhost", port), 0);
                server = httpServer;
                ExecutorService serviceExecutor = Executors.newVirtualThreadPerTaskExecutor();
                executor = serviceExecutor;
                httpServer.setExecutor(serviceExecutor);
                httpServer.createContext("/", new UrlShortenerHandler(
                        port, linkDao, userDao,
                        () -> linkDao.isAvailable() && userDao.isAvailable()));
                httpServer.start();
            } catch (IOException | RuntimeException e) {
                closeResources();
                stopped = true;
                throw new IllegalStateException("Unable to start service", e);
            }
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

    private void closeResources() {
        HttpServer httpServer = server;
        if (httpServer != null) {
            httpServer.stop(0);
        }
        ExecutorService serviceExecutor = executor;
        if (serviceExecutor != null) {
            serviceExecutor.close();
        }
        FileDao linkDao = links;
        if (linkDao != null) {
            linkDao.close();
        }
        FileDao userDao = users;
        if (userDao != null) {
            userDao.close();
        }
    }
}
