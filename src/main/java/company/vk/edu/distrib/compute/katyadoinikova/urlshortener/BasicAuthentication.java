package company.vk.edu.distrib.compute.katyadoinikova.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

final class BasicAuthentication {
    static final String CHALLENGE = "Basic realm=\"url-shortener\", charset=\"UTF-8\"";
    private final Dao<String> users;

    BasicAuthentication(Dao<String> users) {
        this.users = users;
    }

    boolean authenticate(Map<String, List<String>> headers) throws IOException {
        List<String> values = headers.get("Authorization");
        if (values == null || values.size() != 1) {
            return false;
        }
        String header = values.getFirst();
        if (!header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String encoded = header.substring(6).trim();
            String decoded = new String(
                    Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            Credentials credentials = parse(decoded);
            return users.get(credentials.username()).equals(credentials.password());
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return false;
        }
    }

    void register(String value) throws IOException {
        Credentials credentials = parse(value);
        users.upsert(credentials.username(), credentials.password());
    }

    private static Credentials parse(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Expected username:password on one line");
        }
        return new Credentials(value.substring(0, separator), value.substring(separator + 1));
    }

    private record Credentials(String username, String password) {
    }
}
