package company.vk.edu.distrib.compute.playingpeano.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

final class BasicAuthenticator implements AutoCloseable {
    private final PersistentStringDao users;

    BasicAuthenticator(PersistentStringDao users) {
        this.users = users;
    }

    void register(String body) throws IOException {
        int separator = body.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("Credentials must contain a username and password");
        }
        String username = body.substring(0, separator);
        String password = body.substring(separator + 1);
        if (containsControlCharacter(username) || containsControlCharacter(password)) {
            throw new IllegalArgumentException("Credentials contain control characters");
        }
        users.upsert(username, password);
    }

    boolean authenticate(@Nullable String authorization) throws IOException {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }

        String decoded;
        try {
            byte[] credentials = Base64.getDecoder().decode(authorization.substring(6).trim());
            decoded = new String(credentials, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int separator = decoded.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        String username = decoded.substring(0, separator);
        String providedPassword = decoded.substring(separator + 1);
        try {
            String expectedPassword = users.get(username);
            return MessageDigest.isEqual(
                providedPassword.getBytes(StandardCharsets.UTF_8),
                expectedPassword.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchElementException expected) {
            return false;
        }
    }

    boolean isAvailable() {
        return users.isAvailable();
    }

    @Override
    public void close() {
        users.close();
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }
}
