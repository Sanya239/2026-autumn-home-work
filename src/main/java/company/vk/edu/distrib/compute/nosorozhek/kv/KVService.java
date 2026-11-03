package company.vk.edu.distrib.compute.nosorozhek.kv;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.nosorozhek.kv.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KVService implements company.vk.edu.distrib.compute.kv.KVService {
    private static final Logger log = LoggerFactory.getLogger(KVService.class);
    private final HttpServer server;
    private final Dao<byte[]> dao;
    private final ExecutorService executor;

    public KVService(Dao<byte[]> dao, int port, int numThreads) throws IOException {
        this.dao = dao;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", createRouter());
        executor = Executors.newFixedThreadPool(numThreads);
        server.setExecutor(executor);
    }

    private Router createRouter() {
        return new Router()
                .add(HttpMethod.GET, "/v0/status", new GetStatusHandler())
                .add(HttpMethod.GET, "/v0/entity", new GetEntryHandler(dao))
                .add(HttpMethod.PUT, "/v0/entity", new PutEntryHandler(dao))
                .add(HttpMethod.DELETE, "/v0/entity", new DeleteEntryHandler(dao));
    }

    @Override
    public void start() {
        log.info("Started");
        server.start();
    }

    @Override
    public void stop() {
        log.info("Stopping");
        server.stop(1);
        executor.close();

        try {
            dao.close();
        } catch (IOException e) {
            log.error("Failed to close KV dao", e);
        }
    }
}
