package company.vk.edu.distrib.compute.ddkudrin.kv;

import java.io.IOException;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

@KVServiceTest
public class DDKudrinKVServiceFactory extends AbstractHttpServiceFactory<KVService> {

    @Override
    protected KVService doCreate(int port) throws IOException {
        return new DDKudrinKVService(port);
    }
}
