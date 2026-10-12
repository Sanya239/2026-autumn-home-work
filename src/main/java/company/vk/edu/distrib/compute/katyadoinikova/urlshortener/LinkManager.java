package company.vk.edu.distrib.compute.katyadoinikova.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

final class LinkManager {
    private static final String ALPHANUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9]{10}");
    private final Dao<String> links;

    LinkManager(Dao<String> links) {
        this.links = links;
    }

    String get(String id) throws IOException {
        validateId(id);
        return links.get(id);
    }

    String create(String target) throws IOException {
        validateUrl(target);
        while (true) {
            String id = randomId();
            try {
                links.get(id);
            } catch (NoSuchElementException e) {
                links.upsert(id, target);
                return id;
            }
        }
    }

    void update(String id, String target) throws IOException {
        validateId(id);
        validateUrl(target);
        links.get(id);
        links.upsert(id, target);
    }

    void delete(String id) throws IOException {
        validateId(id);
        links.delete(id);
    }

    private static String randomId() {
        StringBuilder id = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length());
            id.append(ALPHANUMERIC.charAt(index));
        }
        return id.toString();
    }

    private static void validateId(String id) {
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid link id");
        }
    }

    private static void validateUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Only absolute HTTP(S) URLs are accepted");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }
}
