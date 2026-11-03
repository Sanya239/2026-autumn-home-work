package company.vk.edu.distrib.compute.mrglaster.urlshortener.service;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.manager.ControllerManager;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.external.LinkController;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.external.StatusController;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.internal.UserController;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.dao.PersistentDao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.exception.DaoCreationException;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.exception.DataSavingException;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;

public class EPUrlShortenerService implements UrlShortenerService {

    private static final int SHUTDOWN_DELAY = 5;
    private static final String USERS_STORAGE_FILE = "/tmp/users";
    private static final String URL_STORAGE_FILE = "/tmp/urls";

    private final HttpServer httpServer;
    private final Dao<String> userDao;
    private final ReentrantLock urlDaoLock = new ReentrantLock();
    private Dao<String> urlDao;
    private boolean started;

    public EPUrlShortenerService(int port) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        final String baseUrl = "http://127.0.0.1:" + port;

        this.userDao = new PersistentDao<>(USERS_STORAGE_FILE, PersistentDao.stringSerializer());
        this.started = false;

        AuthorizationService authService = new AuthorizationService(userDao);

        ControllerManager routingManager = new ControllerManager(authService);
        routingManager.addController(new StatusController());
        routingManager.addController(new LinkController(this::getOrCreateUrlDao, baseUrl));
        routingManager.addController(new UserController(userDao));
        routingManager.register(httpServer);
    }

    private Dao<String> getOrCreateUrlDao() {
        urlDaoLock.lock();
        try {
            if (urlDao == null) {
                urlDao = new PersistentDao<>(URL_STORAGE_FILE, PersistentDao.stringSerializer());
            }
            return urlDao;
        } catch (IOException e) {
            throw new DaoCreationException(e);
        } finally {
            urlDaoLock.unlock();
        }
    }

    @Override
    public void start() {
        getOrCreateUrlDao();
        started = true;
        httpServer.start();
    }

    @Override
    public void stop() {
        urlDaoLock.lock();
        try {
            userDao.close();
            if (urlDao != null) {
                urlDao.close();
            }
        } catch (Exception e) {
            throw new DataSavingException("Unable to save data to files", e);
        } finally {
            urlDaoLock.unlock();
            httpServer.stop(SHUTDOWN_DELAY);
        }
    }

    @Override
    public void setLinksDao(Dao<String> urlDao) {
        urlDaoLock.lock();
        try {
            if (started) {
                throw new IllegalStateException("Links storage can only be set before start");
            }
            if (this.urlDao != null) {
                try {
                    this.urlDao.close();
                } catch (Exception e) {
                    throw new DataSavingException("Unable to close previous links storage", e);
                }
            }
            this.urlDao = urlDao;
        } finally {
            urlDaoLock.unlock();
        }
    }
}
