package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
@UrlShortenerAuthTest
public final class UrlShortenerServiceFactoryImpl extends AbstractHttpServiceFactory<UrlShortenerService> {
    private static final Path DATA_DIR = Path.of(System.getProperty("java.io.tmpdir", "/tmp"), "virogg-urlshortener");

    @Override
    protected UrlShortenerService doCreate(int port) {
        return new UrlShortenerServiceImpl(port, DATA_DIR);
    }
}
