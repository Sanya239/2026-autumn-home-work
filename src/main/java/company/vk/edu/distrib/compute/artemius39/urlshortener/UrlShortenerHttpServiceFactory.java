package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import org.jspecify.annotations.NullMarked;

@UrlShortenerTest
@UrlShortenerAuthTest
@NullMarked
public class UrlShortenerHttpServiceFactory extends AbstractHttpServiceFactory<UrlShortenerController> {
    @Override
    protected UrlShortenerController doCreate(int port) throws IOException {
        Path dataRoot = Path.of(System.getProperty(
            "artemius39.urlshortener.dataDir",
            Path.of(System.getProperty("user.home"), ".urlshortener", "artemius39").toString()
        ));
        Path directory = dataRoot.resolve(Integer.toString(port));
        return new UrlShortenerController(
            new InetSocketAddress("localhost", port),
            new UrlShortenerHandler(
                new PersistentDao(directory.resolve("links")),
                new PersistentDao(directory.resolve("users")),
                new SecureRandom()
            )
        );
    }
}
