package company.vk.edu.distrib.compute.sovesti.kv;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

@KVServiceTest
public final class VfedorovKVServiceFactory extends AbstractHttpServiceFactory<VfedorovKVService> {

    @Override
    protected VfedorovKVService doCreate(int port) throws IOException {
        return new VfedorovKVService(HttpServer.create(new InetSocketAddress(port), 0));
    }

}
