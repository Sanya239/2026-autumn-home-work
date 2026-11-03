package company.vk.edu.distrib.compute.playingpeano.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

@UrlShortenerTest
@UrlShortenerAuthTest
public final class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    private static final String STORAGE_PROPERTY = "playingpeano.urlshortener.storage";

    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        Path serviceDirectory = storageRoot().resolve(Integer.toString(port));
        PersistentStringDao links = new PersistentStringDao(serviceDirectory.resolve("links"));
        try {
            PersistentStringDao users = new PersistentStringDao(serviceDirectory.resolve("users"));
            return new UrlShortenerServiceImpl(port, links, users);
        } catch (IOException | RuntimeException | Error exception) {
            links.close();
            throw exception;
        }
    }

    private static Path storageRoot() {
        @Nullable String configuredRoot = System.getProperty(STORAGE_PROPERTY);
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot);
        }
        String temporaryDirectory = Objects.requireNonNull(System.getProperty("java.io.tmpdir"));
        return Path.of(temporaryDirectory, "playingpeano-url-shortener");
    }
}
