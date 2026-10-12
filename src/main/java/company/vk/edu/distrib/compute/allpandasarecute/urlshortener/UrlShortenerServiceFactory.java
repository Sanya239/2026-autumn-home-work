package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.io.IOException;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
@UrlShortenerAuthTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    private static final String STORAGE_ROOT = "allpandasarecute-url-shortener";

    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), STORAGE_ROOT, Integer.toString(port));
        return new UrlShortenerServiceImpl(
            port,
            new FileStringDao(root.resolve("links")),
            new FileStringDao(root.resolve("users"))
        );
    }
}
