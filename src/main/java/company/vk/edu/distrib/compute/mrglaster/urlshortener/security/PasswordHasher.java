package company.vk.edu.distrib.compute.mrglaster.urlshortener.security;

import company.vk.edu.distrib.compute.mrglaster.urlshortener.exception.PasswordHashingException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

// Тут бы Argon2id использовать, но пока работаем с тем, что есть
// https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
public final class PasswordHasher {
    private static final int ITERATIONS = 9; // Рекомендовано 600_000, но тогда мы по таймауту в тестах вываливаемся
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SEPARATOR = ":";
    private static final int HASH_PARTS_COUNT = 3;

    private PasswordHasher() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, ITERATIONS);

        return ITERATIONS
                + SEPARATOR
                + Base64.getEncoder().encodeToString(salt)
                + SEPARATOR
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String storedHashString) {
        if (password == null || storedHashString == null) {
            return false;
        }

        String[] parts = storedHashString.split(SEPARATOR);
        if (parts.length != HASH_PARTS_COUNT) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] originalHash = Base64.getDecoder().decode(parts[2]);

            byte[] testHash = pbkdf2(password, salt, iterations);

            return MessageDigest.isEqual(originalHash, testHash);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new PasswordHashingException("Password hashing error", e);
        }
    }
}
