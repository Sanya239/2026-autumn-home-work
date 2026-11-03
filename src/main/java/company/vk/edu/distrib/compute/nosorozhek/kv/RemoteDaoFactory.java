package company.vk.edu.distrib.compute.nosorozhek.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RemoteDaoFactoryTest
public class RemoteDaoFactory implements company.vk.edu.distrib.compute.kv.RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) throws IOException {
        if (ports == null || ports.length != 1 || ports[0] <= 0 || ports[0] > 65535) {
            throw new IllegalArgumentException("Expected exactly one valid port");
        }
        return new RemoteDao<>(
                "http://localhost",
                ports[0],
                value -> value.getBytes(StandardCharsets.UTF_8),
                bytes -> new String(bytes, StandardCharsets.UTF_8)
        );
    }
}
