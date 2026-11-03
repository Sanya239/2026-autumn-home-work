package company.vk.edu.distrib.compute.aartchik.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class BasicAuthentication {
    public static final String CHALLENGE = "Basic realm=\"url-shortener\", charset=\"UTF-8\"";

    private static final String BASIC_SCHEME = "Basic";

    private final Dao<String> users;

    public BasicAuthentication(Dao<String> users) {
        this.users = users;
    }

    public boolean authenticate(Map<String, List<String>> headers) throws IOException {
        List<String> values = headers.get("Authorization");
        if (values == null || values.isEmpty()) {
            return false;
        }
        String authorization = values.getFirst();
        int separator = authorization.indexOf(' ');
        if (separator <= 0 || !BASIC_SCHEME.equalsIgnoreCase(authorization.substring(0, separator))) {
            return false;
        }
        String token = authorization.substring(separator + 1).trim();
        if (token.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(token);
            String credentials = decodeUtf8(decoded);
            Credentials parsed = parse(credentials);
            String storedPassword = users.get(parsed.username());
            return MessageDigest.isEqual(
                    storedPassword.getBytes(StandardCharsets.UTF_8),
                    parsed.password().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | CharacterCodingException | NoSuchElementException invalidCredentials) {
            return false;
        }
    }

    public void register(String credentials) throws IOException {
        Credentials parsed = parse(credentials);
        users.upsert(parsed.username(), parsed.password());
    }

    private static Credentials parse(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("Credentials must have a non-empty username and a colon");
        }
        validateSingleLine(value);
        return new Credentials(value.substring(0, separator), value.substring(separator + 1));
    }

    private static void validateSingleLine(String value) {
        if (value.codePoints().anyMatch(BasicAuthentication::isInvalidCharacter)) {
            throw new IllegalArgumentException("Credentials must contain exactly one line");
        }
    }

    private static boolean isInvalidCharacter(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isISOControl(codePoint)
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }

    private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private record Credentials(String username, String password) {
    }
}
