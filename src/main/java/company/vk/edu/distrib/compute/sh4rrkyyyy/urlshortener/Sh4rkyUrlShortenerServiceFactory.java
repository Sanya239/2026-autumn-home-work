package company.vk.edu.distrib.compute.sh4rrkyyyy.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class Sh4rkyUrlShortenerServiceFactory extends AbstractHttpServiceFactory<Sh4rkyUrlShortenerService> {
    @Override
    protected Sh4rkyUrlShortenerService doCreate(int port) throws IOException {
        return new Sh4rkyUrlShortenerService(port);
    }
}
