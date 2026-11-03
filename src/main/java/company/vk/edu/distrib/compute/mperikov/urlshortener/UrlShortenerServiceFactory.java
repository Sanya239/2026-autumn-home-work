package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
@UrlShortenerAuthTest
public final class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        Path root = storageDirectory();
        Dao<String> links = new FileStringDao(root.resolve("links.properties"));
        Dao<String> users = new FileStringDao(root.resolve("users.properties"));
        return new UrlShortenerHttpService(port, links, users);
    }

    private static Path storageDirectory() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null || tmpDir.isBlank()) {
            throw new IOException("java.io.tmpdir is not set");
        }
        return Path.of(tmpDir, "mperikov-urlshortener");
    }
}
