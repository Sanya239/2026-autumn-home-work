package company.vk.edu.distrib.compute.nosorozhek.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<company.vk.edu.distrib.compute.kv.KVService> {
    private static final String DATA_DIR_ENV = "SERVICE_DATA_DIR";
    private static final String NUM_THREADS_ENV = "NUM_THREADS";
    private static final int MIN_THREADS = 1;

    private static Path getDataDirectory() {
        String configuredDirectory = System.getenv(DATA_DIR_ENV);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return Path.of(System.getProperty("java.io.tmpdir"));
        }
        return Path.of(configuredDirectory);
    }

    private static int getNumThreads() {
        String configured = System.getenv(NUM_THREADS_ENV);
        if (configured == null || configured.isBlank()) {
            return MIN_THREADS;
        }
        try {
            int numThreads = Integer.parseInt(configured.strip());
            if (numThreads < MIN_THREADS) {
                throw new IllegalArgumentException(NUM_THREADS_ENV + " must be positive");
            }
            return numThreads;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(NUM_THREADS_ENV + " must be a positive integer", e);
        }
    }

    @Override
    protected company.vk.edu.distrib.compute.kv.KVService doCreate(int port) throws IOException {
        int numThreads = getNumThreads();
        Path dataDirectory = getDataDirectory();
        Files.createDirectories(dataDirectory);

        Dao<byte[]> dao = new PersistentDao(dataDirectory);

        return new KVService(dao, port, numThreads);
    }
}
