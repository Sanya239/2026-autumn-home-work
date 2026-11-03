package company.vk.edu.distrib.compute.randomrandoms.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class Factory extends AbstractHttpServiceFactory<Service> {
    @Override
    protected Service doCreate(int port) throws IOException {
        return new Service(port);
    }
}
