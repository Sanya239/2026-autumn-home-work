package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.security.SecureRandom;

final class LinkIds {
    private static final int LENGTH = 10;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkIds() {
    }

    static String randomId() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    static boolean isValid(String id) {
        if (id.length() != LENGTH) {
            return false;
        }
        for (int index = 0; index < id.length(); index++) {
            if (ALPHABET.indexOf(id.charAt(index)) < 0) {
                return false;
            }
        }
        return true;
    }
}
