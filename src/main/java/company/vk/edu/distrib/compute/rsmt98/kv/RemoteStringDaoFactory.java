package company.vk.edu.distrib.compute.rsmt98.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public final class RemoteStringDaoFactory implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) {
        return new HttpStringDao(ports[0]);
    }
}
