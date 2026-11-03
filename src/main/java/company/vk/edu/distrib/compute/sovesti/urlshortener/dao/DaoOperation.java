package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.IOException;

import company.vk.edu.distrib.compute.Dao;

public sealed interface DaoOperation {

    void execute(Dao<byte[]> dao) throws IOException;

    void serialize(StorageOutputStream out) throws IOException;

    record Upsert(String key, byte[] value) implements DaoOperation {

        @Override
        public void execute(Dao<byte[]> dao) throws IOException {
            dao.upsert(key, value);
        }

        @Override
        public void serialize(StorageOutputStream out) throws IOException {
            out.write(key);
            out.write(value);
        }

    }

    record Delete(String key) implements DaoOperation {

        @Override
        public void execute(Dao<byte[]> dao) throws IOException {
            dao.delete(key);
        }

        @Override
        public void serialize(StorageOutputStream out) throws IOException {
            out.write(key);
            out.writeNiche();
        }

    }
}
