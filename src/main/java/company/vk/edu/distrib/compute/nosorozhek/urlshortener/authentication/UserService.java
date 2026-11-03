package company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.NoSuchElementException;

public final class UserService {

    private final Dao<String> userDao;

    public UserService(Dao<String> userDao) {
        this.userDao = userDao;
    }

    public void authenticate(Credentials credentials) throws IOException {
        String expectedPassword;
        try {
            expectedPassword = userDao.get(credentials.username());
        } catch (NoSuchElementException e) {
            throw new UnauthorizedException(e);
        }

        if (!expectedPassword.equals(credentials.password())) {
            throw new UnauthorizedException();
        }
    }

    public void register(Credentials credentials) throws IOException {
        userDao.upsert(credentials.username(), credentials.password());
    }
}
