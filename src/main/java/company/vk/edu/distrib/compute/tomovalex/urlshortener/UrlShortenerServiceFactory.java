package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerAuthTest
@UrlShortenerTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        return new UrlShortenerService(port, new LinksDao(), new UsersDao());
    }
}
