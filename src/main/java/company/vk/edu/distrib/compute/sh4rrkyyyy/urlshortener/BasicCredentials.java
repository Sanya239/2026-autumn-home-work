package company.vk.edu.distrib.compute.sh4rrkyyyy.urlshortener;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicCredentials {
    private final String username;
    private final String password;
    private static final String BASIC_PREF = "Basic ";

    public BasicCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public static BasicCredentials parse(String str) {
        int del = str.indexOf(':');
        if (del < 0) {
            return null;
        }
        return new BasicCredentials(str.substring(0, del), str.substring(del + 1));
    }

    public static BasicCredentials fromAuthHeader(String header) {
        if (header == null || !header.startsWith(BASIC_PREF)) {
            return null;
        }
        String encoded = header.substring(BASIC_PREF.length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        return parse(decoded);
    }
}
