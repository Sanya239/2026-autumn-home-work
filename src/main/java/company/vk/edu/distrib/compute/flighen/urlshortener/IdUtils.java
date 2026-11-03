package company.vk.edu.distrib.compute.flighen.urlshortener;

import java.security.SecureRandom;

public final class IdUtils {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom GENERATOR = new SecureRandom();

    private IdUtils() {
    }

    public static String getId(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int nextCharInd = GENERATOR.nextInt(0, ALPHABET.length());

            result.append(ALPHABET.charAt(nextCharInd));
        }

        return result.toString();
    }

    public static boolean isValidId(String id) {
        if (id.length() == 0) {
            return false;
        }

        for (int i = 0; i < id.length(); i++) {
            if (ALPHABET.indexOf(id.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }
}
