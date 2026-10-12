package company.vk.edu.distrib.compute.akhunzianov.urlshortener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.akhunzianov.InMemoryDao;
import company.vk.edu.distrib.compute.akhunzianov.PersistentDao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {

    static final String STORAGE_DIR = "SHORTENER_STORAGE_DIR";

    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        return new UrlShortenerServiceImpl(port, storage("links"), null);
    }

    static Dao<String> storage(String name) throws IOException {
        var dir = System.getenv(STORAGE_DIR);
        if (dir == null || dir.isBlank()) {
            return new InMemoryDao();
        }
        var directory = Path.of(dir);
        Files.createDirectories(directory);
        return new PersistentDao(directory.resolve(name + ".log"));
    }
}
