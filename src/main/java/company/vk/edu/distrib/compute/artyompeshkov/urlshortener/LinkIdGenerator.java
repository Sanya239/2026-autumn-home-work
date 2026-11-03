package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.security.SecureRandom;

class LinkIdGenerator {
    private final SecureRandom random = new SecureRandom();

    LinkId generate() {
        StringBuilder id = new StringBuilder(LinkId.LENGTH);
        for (int i = 0; i < LinkId.LENGTH; i++) {
            id.append(LinkId.ALPHABET.charAt(random.nextInt(LinkId.ALPHABET.length())));
        }
        return new LinkId(id.toString());
    }
}
