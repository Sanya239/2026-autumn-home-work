package company.vk.edu.distrib.compute.masha533.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

import java.io.IOException;

@RemoteDaoFactoryTest
public class RemoteDaoFactoryImpl implements RemoteDaoFactory<String> {
    private static final int PORT_COUNT = 1;

    @Override
    public Dao<String> create(int... ports) throws IOException {
        if (ports.length != PORT_COUNT) {
            throw new IllegalArgumentException();
        }
        return new RemoteDao(ports[0]);
    }
}
