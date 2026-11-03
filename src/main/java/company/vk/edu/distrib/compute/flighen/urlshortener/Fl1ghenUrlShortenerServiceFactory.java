package company.vk.edu.distrib.compute.flighen.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;

import java.io.IOException;

import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;

@UrlShortenerAuthTest
@UrlShortenerTest
public class Fl1ghenUrlShortenerServiceFactory extends AbstractHttpServiceFactory<Fl1ghenUrlShortenerService> {

    @Override
    protected Fl1ghenUrlShortenerService doCreate(int port) throws IOException {
        return new Fl1ghenUrlShortenerService(port);
    }
}
