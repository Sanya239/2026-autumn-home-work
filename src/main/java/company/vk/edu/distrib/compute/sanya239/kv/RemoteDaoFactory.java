package company.vk.edu.distrib.compute.sanya239.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

import java.io.IOException;

@RemoteDaoFactoryTest
public class RemoteDaoFactory implements company.vk.edu.distrib.compute.kv.RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) throws IOException {
        if (ports.length == 0) {
            throw new IllegalArgumentException("Need a port for the RemoteDao");
        }
        return new RemoteDao(ports[0]);
    }
}
