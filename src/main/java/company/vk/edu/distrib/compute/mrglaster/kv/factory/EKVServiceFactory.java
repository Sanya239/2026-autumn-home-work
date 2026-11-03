package company.vk.edu.distrib.compute.mrglaster.kv.factory;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import company.vk.edu.distrib.compute.mrglaster.kv.service.EKVServiceImpl;

import java.io.IOException;

@KVServiceTest
public class EKVServiceFactory extends AbstractHttpServiceFactory<EKVServiceImpl> {
    @Override
    protected EKVServiceImpl doCreate(int port) throws IOException {
        return new EKVServiceImpl(port);
    }
}
