package company.vk.edu.distrib.compute.kv;

import java.lang.annotation.*;

/**
 * Marks an {@link RemoteDaoFactory} implementation to be picked up
 * by the Sharding integration test suite.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClusterDaoFactoryTest {
}
