package company.vk.edu.distrib.compute.mrglaster.urlshortener.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShortLinksGeneratorService {
    private final URI baseUrl;
    private static final int MIN_HEX_LENGTH = 1;

    public ShortLinksGeneratorService(String baseUrl) {
        this.baseUrl = URI.create(baseUrl);
    }

    public String generateLinkID(String longUrl) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(longUrl.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == MIN_HEX_LENGTH) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.substring(0, 10);
    }

    public String generateShortURL(String linkId) throws NoSuchAlgorithmException {
        return baseUrl.resolve(linkId).toString();
    }
}
