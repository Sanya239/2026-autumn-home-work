package company.vk.edu.distrib.compute.artyompeshkov.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public class RemoteDaoFactoryImpl implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) {
        return new RemoteDao(ports[0]);
    }
}
