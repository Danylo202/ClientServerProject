package com.danil.app.server.databases;

import com.danil.app.domain.ProductInfo;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Store {
    // зберігання товарів: назва => ціна й кількість
    private final Map<String, ProductInfo> inventory = new ConcurrentHashMap<>();
    
    // група товарів => список їх назв
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();

    public synchronized int getCount(String name) {
        ProductInfo info = inventory.get(name);
        if (info == null) {
            return 0;
        }
        else {
            return info.getQuantity();
        }
    }

    public synchronized void addProductQuantity(String name, int amount) {
        ProductInfo info = inventory.get(name);
        if (info == null) {
            inventory.put(name, new ProductInfo(0.0, amount));
        }
        else {
            info.setQuantity(info.getQuantity() + amount);
        }
    }

    public synchronized boolean withdrawProduct(String name, int amount) {
        ProductInfo info = inventory.get(name);
        if (info != null && info.getQuantity() >= amount) {
            info.setQuantity(info.getQuantity() - amount);
            return true;
        }
        return false;
    }

    public synchronized void addGroup(String groupName) {
        groups.putIfAbsent(groupName, new HashSet<>());
    }

    public synchronized void addProductToGroup(String groupName, String productName) {
        Set<String> productSet = groups.get(groupName);
        if (productSet == null) {
            productSet = new HashSet<>();
            groups.put(groupName, productSet);
        }
        productSet.add(productName);

        inventory.putIfAbsent(productName, new ProductInfo(0.0, 0));
    }

    public synchronized void setPrice(String name, double price) {
        ProductInfo info = inventory.get(name);
        if (info == null) {
            inventory.put(name, new ProductInfo(price, 0));
        }
        else {
            info.setPrice(price);
        }
    }
}