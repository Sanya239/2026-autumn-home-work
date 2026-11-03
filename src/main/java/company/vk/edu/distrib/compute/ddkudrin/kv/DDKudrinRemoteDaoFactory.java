package company.vk.edu.distrib.compute.ddkudrin.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public class DDKudrinRemoteDaoFactory implements RemoteDaoFactory<String> {
    private static final int MIN_PORT_COUNT = 1;

    @Override
    public Dao<String> create(int... ports) {
        if (ports.length < MIN_PORT_COUNT) {
            throw new IllegalArgumentException("Expected at least one KV service port");
        }
        return new DDKudrinRemoteDao(ports[0]);
    }
}
