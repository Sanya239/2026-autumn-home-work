package company.vk.edu.distrib.compute.artyompeshkov.kv;

import java.io.IOException;
import java.nio.file.Path;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
    private static final Path STORAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "artyompeshkov-kv");

    @Override
    protected KVService doCreate(int port) throws IOException {
        return new KVServiceImpl(port, new BitcaskDao(STORAGE_DIR));
    }
}
