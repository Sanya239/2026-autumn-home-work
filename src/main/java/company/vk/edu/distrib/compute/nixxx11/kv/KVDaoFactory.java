package company.vk.edu.distrib.compute.nixxx11.kv;

import java.io.IOException;
import java.net.URI;

import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;
import company.vk.edu.distrib.compute.nixxx11.kv.dao.RemoteDao;

@RemoteDaoFactoryTest
public class KVDaoFactory implements RemoteDaoFactory<String> {
  @Override
  public RemoteDao create(final int... ports) throws IOException {
    return new RemoteDao(URI.create("http://localhost:" + ports[0] + "/v0/entity"));
  }
}
