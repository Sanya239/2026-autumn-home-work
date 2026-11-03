package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

record LinkId(String value) {
    static final int LENGTH = 10;
    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    LinkId {
        if (value.length() != LENGTH || !value.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            throw new IllegalArgumentException("Invalid link ID: " + value);
        }
    }
}
