package company.vk.edu.distrib.compute.sanya239.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import company.vk.edu.distrib.compute.kv.ReplicatedKVServiceTest;

import java.io.IOException;

@KVServiceTest
@ReplicatedKVServiceTest
public class HttpServiceFactory extends AbstractHttpServiceFactory<KVService> {
    @Override
    protected KVService doCreate(int port) throws IOException {
        return new KVService(port);
    }
}
