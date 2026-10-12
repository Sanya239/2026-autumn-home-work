package company.vk.edu.distrib.compute.allpandasarecute.urlshortener;

import java.security.SecureRandom;

final class Ids {
    private static final int LENGTH = 10;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    static boolean isValid(String id) {
        if (id.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            if (ALPHABET.indexOf(id.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    static String generate() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
