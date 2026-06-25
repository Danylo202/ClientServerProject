package com.danil.app.server.databases;

import com.danil.app.domain.Product;
import java.util.List;
import java.util.Optional;

public interface Db {
    boolean withdrawProduct(String name, int amount);
    void addProductQuantity(String name, int amount);
    void setPrice(String name, double price);
    void addGroup(String groupName);
    void addProductToGroup(String productName, String groupName);
    int create(Product p);
    Optional<Product> getById(int id);
    Optional<Product> get(String name);
    void edit(Product p);
    int getCount();
    int getCount(String name);
    void delete(int id);
    void delete(String name);
    List<Product> search(String name, String cat, Double minP, Double maxP, int page, int size);
}
