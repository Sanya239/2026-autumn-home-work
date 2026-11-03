package company.vk.edu.distrib.compute.ruavee.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

import java.io.IOException;

@RemoteDaoFactoryTest
public class RemoteStringDaoFactory implements RemoteDaoFactory<String> {
    private static final int EXPECTED_PORT_COUNT = 1;

    @Override
    public Dao<String> create(int... ports) throws IOException {
        if (ports.length != EXPECTED_PORT_COUNT) {
            throw new IllegalArgumentException("RemoteStringDaoFactory takes exactly one port");
        }
        int port = ports[0];
        return new RemoteStringDao(port);
    }
}
