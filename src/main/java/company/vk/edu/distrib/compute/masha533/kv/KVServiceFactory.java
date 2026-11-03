package company.vk.edu.distrib.compute.masha533.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.nio.file.Path;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {

    @Override
    protected KVService doCreate(int port) throws IOException {
        var dao = new PersistentByteDao(Path.of(System.getProperty("java.io.tmpdir"), "masha533-kv" + port + ".bin"));
        return new KVServiceImpl(port, dao);
    }
}
