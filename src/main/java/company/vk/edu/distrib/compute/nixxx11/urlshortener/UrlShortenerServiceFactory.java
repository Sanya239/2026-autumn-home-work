package company.vk.edu.distrib.compute.nixxx11.urlshortener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.dao.DiskDao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
@UrlShortenerAuthTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
  @Override
  protected UrlShortenerService doCreate(final int port) throws IOException {
    final Path path = Files.createTempDirectory("urlshortener");
    return new UrlShortenerServiceImpl(
        port,
        new DiskDao(path.resolve("links")),
        new DiskDao(path.resolve("users"))
    );
  }
}
