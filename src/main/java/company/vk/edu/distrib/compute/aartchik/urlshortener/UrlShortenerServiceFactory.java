package company.vk.edu.distrib.compute.aartchik.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;
import java.nio.file.Path;

@UrlShortenerTest
@UrlShortenerAuthTest
public final class UrlShortenerServiceFactory
        extends AbstractHttpServiceFactory<UrlShortenerService> {
    private static final Path DATA_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"),
            "aartchik-url-shortener");

    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        return new UrlShortenerServiceImpl(port, DATA_DIRECTORY);
    }
}
