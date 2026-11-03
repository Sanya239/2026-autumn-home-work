package company.vk.edu.distrib.compute.sh4rrkyyyy.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class DaoString implements Dao<String> {
    private final Map<String, String> shortToLongLinks = new ConcurrentHashMap<>();
    private final Path log;
    private final ReentrantLock writeLock = new ReentrantLock();

    private static final String PUT_OP_LOG = "p";
    private static final String DEL_OP_LOG = "d";

    public DaoString(String file) throws IOException {
        this.log = Path.of(System.getProperty("java.io.tmpdir"), file);
        if (!Files.exists(log)) {
            Files.createFile(log);
        }
        log.toFile().deleteOnExit();
        restore();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (key == null) {
            throw new IllegalArgumentException("get: key is null");
        }
        String val = shortToLongLinks.get(key);
        if (val == null) {
            throw new NoSuchElementException("No link for id " + key);
        }
        return val;
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("upsert: key or value is null");
        }
        writeLock.lock();
        try {
            shortToLongLinks.put(key, value);
            Files.writeString(log, PUT_OP_LOG + " " + key + " " + value + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        if (key == null) {
            throw new IllegalArgumentException("delete: key is null");
        }
        writeLock.lock();
        try {
            shortToLongLinks.remove(key);
            Files.writeString(log, DEL_OP_LOG + " " + key + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // Nothing to do: log file is removed via deleteOnExit
    }

    private void restore() throws IOException {
        for (String line : Files.readAllLines(log, StandardCharsets.UTF_8)) {
            String[] p = line.split(" ", 3);
            if (PUT_OP_LOG.equals(p[0]) && p.length == 3) {
                shortToLongLinks.put(p[1], p[2]);
            } else if (DEL_OP_LOG.equals(p[0]) && p.length == 2) {
                shortToLongLinks.remove(p[1]);
            }
        }
    }
}
