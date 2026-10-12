package company.vk.edu.distrib.compute.akhunzianov.urlshortener;

import java.io.IOException;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;

@UrlShortenerAuthTest
public class AuthenticatedUrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {

    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        return new UrlShortenerServiceImpl(port,
            UrlShortenerServiceFactory.storage("links"),
            UrlShortenerServiceFactory.storage("users"));
    }
}
