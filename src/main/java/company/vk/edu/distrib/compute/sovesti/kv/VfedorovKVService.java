package company.vk.edu.distrib.compute.sovesti.kv;

import java.util.Objects;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.sovesti.urlshortener.auth.AuthenticationScheme;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.Daos;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.InFileDaoFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.HttpContexts;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.ServerExecutor;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.EntitiesRoute;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.StatusRoute;

public final class VfedorovKVService implements KVService {

    private final HttpServer server;
    private final Daos<byte[]> daos;

    public VfedorovKVService(HttpServer server) {
        this.server = Objects.requireNonNull(server);
        this.daos = new Daos<>(new InFileDaoFactory());
    }

    @Override
    public void start() {
        Dao<byte[]> entities = daos.getOrCreate("entities");
        HttpContexts contexts = new HttpContexts(server, new AuthenticationScheme.Transient());
        contexts.create(new StatusRoute());
        contexts.create(new EntitiesRoute(entities));
        new ServerExecutor(server).fromEnv();
        server.start();
    }

    @Override
    public void stop() {
        daos.close();
        server.stop(1);
    }

}
