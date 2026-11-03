package company.vk.edu.distrib.compute.netheer.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        Dao<String> linksDao = new MemoryDao();
        Dao<String> usersDao = new MemoryDao();

        return new UrlShortenerServiceImplementation(port, linksDao, usersDao);
    }
}
