package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.DaoOperation.Delete;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.DaoOperation.Upsert;

final class DaoOperations {

    private final Collection<DaoOperation> operations = new ConcurrentLinkedQueue<>();

    void fill(StorageInputStream raw) throws IOException {
        for (Optional<String> key = raw.readString(); key.isPresent(); key = raw.readString()) {
            add(parseOperation(raw, key.get()));
        }
    }

    private DaoOperation parseOperation(StorageInputStream raw, String key) throws IOException {
        return raw.read()
            .map(bytes -> new Upsert(key, bytes))
            .map(DaoOperation.class::cast)
            .orElseGet(() -> new Delete(key));
    }

    void add(DaoOperation operation) {
        operations.add(operation);
    }

    void execute(Dao<byte[]> dao) throws IOException {
        for (DaoOperation op : new ArrayList<>(operations)) {
            op.execute(dao);
        }
    }

}
