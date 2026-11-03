package company.vk.edu.distrib.compute.sovesti.urlshortener;

import java.util.Objects;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.sovesti.urlshortener.auth.BasicAuthentication;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.Daos;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.DecodingDao;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.InFileDaoFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.HttpContexts;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.InternalUsersRoute;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.LinksRoute;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.RootRoute;
import company.vk.edu.distrib.compute.sovesti.urlshortener.route.StatusRoute;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public final class VfedorovUrlShortenerService implements UrlShortenerService {

    private final HttpServer server;
    private final Daos<String> daos;

    public VfedorovUrlShortenerService(HttpServer server) {
        this.server = Objects.requireNonNull(server);
        this.daos = new Daos<>(key -> new DecodingDao(new InFileDaoFactory().createDao(key)));
    }

    @Override
    public void start() {
        Dao<String> links = daos.getOrCreate(linksKey());
        Dao<String> users = daos.getOrCreate(usersKey());
        HttpContexts contexts = new HttpContexts(server, new BasicAuthentication("URL shortener", users));
        contexts.create(new StatusRoute());
        contexts.create(new RootRoute(links));
        contexts.create(new InternalUsersRoute(users));
        contexts.createAuthenticated(new LinksRoute(links));
        server.start();
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        daos.put(linksKey(), dao);
    }

    private String linksKey() {
        return "links";
    }

    private String usersKey() {
        return "users";
    }

    @Override
    public void stop() {
        daos.close();
        server.stop(1);
    }

}
