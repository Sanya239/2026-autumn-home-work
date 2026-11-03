package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import company.vk.edu.distrib.compute.Dao;

public final class InFileDaoFactory implements DaoFactory<byte[]> {

    @Override
    public Dao<byte[]> createDao(String key) throws IOException {
        InFileDao dao = new InFileDao(temporaryDirectory().resolve(key));
        dao.read();
        return dao;
    }

    private Path temporaryDirectory() throws IOException {
        return Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir"), "vfedorov_urlshortener"));
    }
}
