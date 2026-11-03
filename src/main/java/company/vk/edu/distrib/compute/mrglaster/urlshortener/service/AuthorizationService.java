package company.vk.edu.distrib.compute.mrglaster.urlshortener.service;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.security.PasswordHasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AuthorizationService {
    private final Dao<String> userDao;
    private static final String SEPARATOR = ":";
    private static final int HEADER_PARTS_COUNT = 2;

    public AuthorizationService(Dao<String> userDao) {
        this.userDao = userDao;
    }

    public boolean checkBasicAuth(String authHeader) throws IOException {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

            String[] parts = credentials.split(SEPARATOR, 2);
            if (parts.length != HEADER_PARTS_COUNT) {
                return false;
            }

            String username = parts[0];
            String password = parts[1];

            return verify(username, password);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean verify(String login, String password) {
        try {
            String storedHash = userDao.get(login);
            return PasswordHasher.verify(password, storedHash);
        } catch (IOException e) {
            return false;
        }
    }
}
