package company.vk.edu.distrib.compute.annapiatkova.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class HttpServiceFactoryImpl extends AbstractHttpServiceFactory<UrlShortenerServiceImpl> {
    @Override
    protected UrlShortenerServiceImpl doCreate(int port) throws IOException {
        return new UrlShortenerServiceImpl(port);
    }
}
