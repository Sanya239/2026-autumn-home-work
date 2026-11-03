package company.vk.edu.distrib.compute.mrglaster.urlshortener.factory;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.service.EPUrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class EPUrlShortenerServiceFactory extends AbstractHttpServiceFactory<EPUrlShortenerService> {

    @Override
    protected EPUrlShortenerService doCreate(int port) throws IOException {
        return new EPUrlShortenerService(port);
    }
}
