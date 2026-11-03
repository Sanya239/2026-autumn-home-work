package company.vk.edu.distrib.compute.mrglaster.kv.service;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.mrglaster.kv.controller.EntityController;
import company.vk.edu.distrib.compute.mrglaster.kv.controller.KVStatusController;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.manager.ControllerManager;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.dao.PersistentDao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.exception.DataSavingException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class EKVServiceImpl implements KVService {

    private final HttpServer httpServer;
    private static final int SHUTDOWN_DELAY = 5;
    private final Dao<byte[]> entityDao;
    private static final String ENTITY_STORAGE_FILE = "/tmp/entity";

    public EKVServiceImpl(int port) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        ControllerManager routingManager = new ControllerManager(null);
        this.entityDao = new PersistentDao<>(ENTITY_STORAGE_FILE, PersistentDao.byteArraySerializer());
        routingManager.addController(new EntityController(entityDao));
        routingManager.addController(new KVStatusController());
        routingManager.register(this.httpServer);
    }

    @Override
    public void start() {
        httpServer.start();
    }

    @Override
    public void stop() {
        try {
            entityDao.close();
        } catch (IOException e) {
            throw new DataSavingException("Unable to save the data", e);
        } finally {
            httpServer.stop(SHUTDOWN_DELAY);
        }
    }

    @Override
    public CompletableFuture<Void> awaitTermination() {
        return KVService.super.awaitTermination();
    }
}
