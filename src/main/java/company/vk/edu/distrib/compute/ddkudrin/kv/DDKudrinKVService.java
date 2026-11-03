package company.vk.edu.distrib.compute.ddkudrin.kv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.ddkudrin.urlshortener.DDKudrinStatusHandler;
import company.vk.edu.distrib.compute.kv.KVService;

public class DDKudrinKVService implements KVService {
    private static final int MIN_THREAD_COUNT = 1;
    private final HttpServer server;
    private final Dao<byte[]> dao;
    private final ExecutorService executor;

    public DDKudrinKVService(int port) throws IOException {
        this(port, Path.of(System.getProperty("user.home"), "data", "kv", port + ".wal"),
                getThreads());
    }

    public DDKudrinKVService(int port, Path file, int threads) throws IOException {
        this.dao = new DDKudrinKVDao(file);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/v0/status", new DDKudrinStatusHandler());
        this.server.createContext("/", new DDKudrinEntityHandler(dao));
        this.executor = Executors.newFixedThreadPool(threads);
        this.server.setExecutor(executor);
    }

    private static int getThreads() {
        String value = System.getenv("KV_THREADS");
        if (value == null) {
            return 1;
        }
        try {
            int threads = Integer.parseInt(value);
            if (threads < MIN_THREAD_COUNT) {
                throw new IllegalArgumentException("KV_THREADS must be a positive integer");
            }
            return threads;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("KV_THREADS must be a positive integer", e);
        }
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);
        if (executor != null) {
            executor.close();
        }
        try {
            dao.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot close KV storage", e);
        }
    }
}
