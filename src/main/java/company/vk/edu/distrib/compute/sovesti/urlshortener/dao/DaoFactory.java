package company.vk.edu.distrib.compute.sovesti.urlshortener.dao;

import java.io.IOException;

import company.vk.edu.distrib.compute.Dao;

@FunctionalInterface
public interface DaoFactory<T> {

    Dao<T> createDao(String key) throws IOException;
}
