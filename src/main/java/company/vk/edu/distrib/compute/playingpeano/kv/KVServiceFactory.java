package company.vk.edu.distrib.compute.playingpeano.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

@KVServiceTest
public final class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
    private static final String STORAGE_PROPERTY = "playingpeano.kv.storage";

    @Override
    protected KVService doCreate(int port) throws IOException {
        Path serviceDirectory = storageRoot().resolve(Integer.toString(port));
        PersistentByteArrayDao dao = new PersistentByteArrayDao(serviceDirectory);
        try {
            return new KVServiceImpl(port, dao);
        } catch (IOException | RuntimeException | Error exception) {
            dao.close();
            throw exception;
        }
    }

    private static Path storageRoot() {
        @Nullable String configuredRoot = System.getProperty(STORAGE_PROPERTY);
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot);
        }
        String temporaryDirectory = Objects.requireNonNull(System.getProperty("java.io.tmpdir"));
        return Path.of(temporaryDirectory, "playingpeano-kv");
    }
}
