package company.vk.edu.distrib.compute.annapiatkova.urlshortener;

import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.io.IOException;

public class UrlShortenerServiceImpl implements UrlShortenerService {
    HttpServer server;
    HandlerImpl handler;
    String linkPrefix;

    UrlShortenerServiceImpl(int port) throws IOException {
        linkPrefix = "http://localhost:" + port;
        handler = new HandlerImpl(new DaoImpl(), linkPrefix);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", handler);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);
    }
}
