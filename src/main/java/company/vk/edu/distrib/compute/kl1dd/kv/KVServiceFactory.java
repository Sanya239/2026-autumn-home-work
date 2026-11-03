package company.vk.edu.distrib.compute.kl1dd.kv;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.net.InetSocketAddress;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
    @Override
    protected KVService doCreate(int port) throws IOException {
        HttpServer httpService = HttpServer.create(new InetSocketAddress(port), 0);
        return new MyKVService(httpService);
    }
}
