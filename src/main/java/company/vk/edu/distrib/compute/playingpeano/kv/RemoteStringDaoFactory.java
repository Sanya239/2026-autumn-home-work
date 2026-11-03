package company.vk.edu.distrib.compute.playingpeano.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public final class RemoteStringDaoFactory implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) {
        if (ports.length == 0) {
            throw new IllegalArgumentException("At least one KV service port is required");
        }
        int port = ports[0];
        if (port <= 0 || port >= 65_536) {
            throw new IllegalArgumentException("Port out of range");
        }
        return new RemoteStringDao(port);
    }
}
