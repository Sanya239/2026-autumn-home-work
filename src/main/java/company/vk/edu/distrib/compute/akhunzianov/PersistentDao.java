package company.vk.edu.distrib.compute.akhunzianov;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public class PersistentDao implements Dao<String> {

    private static final String UPSERT = "+";
    private static final String DELETE = "-";
    private static final String SEPARATOR = "\t";
    private static final int FIELDS = 3;

    private final InMemoryDao mem = new InMemoryDao();
    private final BufferedWriter log;

    public PersistentDao(Path path) throws IOException {
        load(path);
        log = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public String get(String key) throws NoSuchElementException {
        return mem.get(key);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        append(UPSERT, key, value);
        mem.upsert(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        append(DELETE, key, "");
        mem.delete(key);
    }

    @Override
    public void close() throws IOException {
        log.close();
        mem.close();
    }

    private void load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        for (var line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            var record = line.split(SEPARATOR, FIELDS);
            if (record.length < FIELDS) {
                continue;
            }
            if (UPSERT.equals(record[0])) {
                mem.upsert(record[1], record[2]);
            } else {
                mem.delete(record[1]);
            }
        }
    }

    private void append(String op, String key, String value) throws IOException {
        log.write(op + SEPARATOR + key + SEPARATOR + value);
        log.newLine();
        log.flush();
    }
}
