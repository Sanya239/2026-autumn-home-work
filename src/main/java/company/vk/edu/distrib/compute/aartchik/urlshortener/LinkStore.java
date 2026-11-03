package company.vk.edu.distrib.compute.aartchik.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LinkStore {
    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int ID_LENGTH = 10;

    private final Dao<String> links;
    private final SecureRandom random = new SecureRandom();
    private final Lock mutationLock = new ReentrantLock();

    public LinkStore(Dao<String> links) {
        this.links = links;
    }

    public String get(String id) throws IOException {
        validateId(id);
        return links.get(id);
    }

    public String create(String longLink) throws IOException {
        validateLink(longLink);
        mutationLock.lock();
        try {
            String id;
            do {
                id = randomId();
            } while (exists(id));
            links.upsert(id, longLink);
            return id;
        } finally {
            mutationLock.unlock();
        }
    }

    public void update(String id, String longLink) throws IOException {
        validateId(id);
        validateLink(longLink);
        mutationLock.lock();
        try {
            links.get(id);
            links.upsert(id, longLink);
        } finally {
            mutationLock.unlock();
        }
    }

    public void delete(String id) throws IOException {
        validateId(id);
        mutationLock.lock();
        try {
            links.delete(id);
        } finally {
            mutationLock.unlock();
        }
    }

    private boolean exists(String id) throws IOException {
        try {
            links.get(id);
            return true;
        } catch (NoSuchElementException missing) {
            return false;
        }
    }

    private String randomId() {
        char[] id = new char[ID_LENGTH];
        for (int index = 0; index < id.length; index++) {
            id[index] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(id);
    }

    private static void validateId(String id) {
        if (id.length() != ID_LENGTH) {
            throw new IllegalArgumentException("ID must contain exactly 10 alphanumeric characters");
        }
        for (int index = 0; index < id.length(); index++) {
            if (ALPHABET.indexOf(id.charAt(index)) < 0) {
                throw new IllegalArgumentException("ID must contain exactly 10 alphanumeric characters");
            }
        }
    }

    private static void validateLink(String longLink) {
        URI uri;
        try {
            uri = new URI(longLink);
        } catch (URISyntaxException invalidUri) {
            throw new IllegalArgumentException("Invalid link", invalidUri);
        }
        String scheme = uri.getScheme();
        boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!validScheme || uri.getHost() == null || uri.getPort() > 65535) {
            throw new IllegalArgumentException("Link must use HTTP or HTTPS and contain a host");
        }
    }
}
