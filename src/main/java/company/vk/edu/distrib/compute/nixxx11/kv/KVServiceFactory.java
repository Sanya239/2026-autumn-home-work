package company.vk.edu.distrib.compute.nixxx11.kv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import company.vk.edu.distrib.compute.nixxx11.kv.dao.DiskDao;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
  @Override
  protected KVService doCreate(final int port) throws IOException {
    final Path path = Files.createTempDirectory("kv");
    return new KVServiceImpl(
        port,
        new DiskDao(path)
    );
  }
}
