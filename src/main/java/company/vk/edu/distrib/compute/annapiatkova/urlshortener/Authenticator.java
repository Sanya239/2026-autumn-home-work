package company.vk.edu.distrib.compute.annapiatkova.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;

class Authenticator {
    private final Dao<String> users;

    Authenticator() {
        users = new DaoImpl();
    }

    void addUser(String username, String password) throws IOException {
        users.upsert(username, password);
    }

    boolean checkCredentials(String username, String password) throws IOException {
        String expectedPassword = users.get(username);
        return expectedPassword != null && expectedPassword.equals(password);
    }
}
