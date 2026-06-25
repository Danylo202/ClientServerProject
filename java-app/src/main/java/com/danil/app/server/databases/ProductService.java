package com.danil.app.server.databases;

import com.danil.app.domain.Product;
import com.danil.app.domain.ProductInfo;
import com.danil.app.domain.Page;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ProductService {
    private final Map<Integer, Product> productList = new ConcurrentHashMap<>();
    private final Set<String> activeGroups = Collections.synchronizedSet(new HashSet<>());
    private int idCounter = 1;

    // щоб не переписувати код з перших практичних, тут реалізовані старі методи і за ім'ям, і за id
    public synchronized int getCount(String name) {
        Product product = productList.values().stream()
        .filter(p -> p.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);

        if (product == null) {
            return 0;
        }
        else {
            return product.getQuantity();
        }
    }

    public synchronized int getCount(int id) {
        Product product = productList.get(id);
        if (product == null) {
            return 0;
        }
        else {
            return product.getQuantity();
        }
    }

    public synchronized boolean addProductQuantity(String name, int amount) {
        Product product = productList.values().stream()
        .filter(p -> p.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);

        if (product == null) {
            System.err.println("[Service] Помилка: товар '" + name + "' не знайдено. Спочатку створіть його.");
            return false;
        }
        else {
            product.setQuantity(product.getQuantity() + amount);
            return true;
        }
    }

    public synchronized boolean addProductQuantity(int id, int amount) {
        Product product = productList.get(id);
        if (product == null) {
            System.err.println("[Service] Помилка: товар з id " + id + " не знайдено.");
            return false;
        }
        else {
            product.setQuantity(product.getQuantity() + amount);
            return true;
        }
    }

    public synchronized boolean withdrawProduct(String name, int amount) {
        Product product = productList.values().stream()
        .filter(p -> p.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);

        if (product == null) {
            System.err.println("[Service] Помилка: товар '" + name + "' не знайдено. Спочатку створіть його.");
            return false;
        }
        else if (product.getQuantity() < amount) {
            System.err.println("[Service] Помилка: товару '" + name + "' замало, щоб прибрати " + amount + " одиниць.");
            return false;
        }
        else {
            product.setQuantity(product.getQuantity() - amount);
            return true;
        }
    }

    public synchronized boolean withdrawProduct(int id, int amount) {
        Product product = productList.get(id);
        if (product == null) {
            System.err.println("[Service] Помилка: товар " + id + " не знайдено. Спочатку створіть його.");
            return false;
        }
        else if (product.getQuantity() < amount) {
            System.err.println("[Service] Помилка: товару " + id + " замало, щоб прибрати " + amount + " одиниць.");
            return false;
        }
        else {
            product.setQuantity(product.getQuantity() - amount);
            return true;
        }
    }

    public synchronized void addGroup(String groupName) {
        activeGroups.add(groupName);
        System.out.println("[Service] Створено групу '" + groupName + "'");
    }

    public synchronized void addProductToGroup(String groupName, String productName) {
        Product product = productList.values().stream()
        .filter(p -> p.getName().equalsIgnoreCase(productName))
        .findFirst()
        .orElse(null);

        if (product != null) {
            product.setCategory(groupName);
            System.out.println("Товар " + productName + " тепер у групі " + groupName);
        }
    }

    public synchronized boolean setPrice(String name, double price) {
        Product product = productList.values().stream()
        .filter(p -> p.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);

        if (product == null) {
            System.err.println("[Service] Помилка: товар '" + name + "' не знайдено. Спочатку створіть його.");
            return false;
        }
        else {
            product.setPrice(price);
            return true;
        }
    }

    public synchronized boolean setPrice(int id, double price) {
        Product product = productList.get(id);
        if (product == null) {
            System.err.println("[Service] Помилка: товар " + id + " не знайдено.");
            return false;
        }
        else {
            product.setPrice(price);
            return true;
        }
    }

    // нові методи (4 практична)
    public synchronized Product create(String name, String category, double price, int quantity) {
        if (!activeGroups.contains(category)) {
            System.err.println("[Error] Неможливо створити товар: група '" + category + "' не існує!");
            return null;
        }

        int id = idCounter++;
        Product p = new Product(id, name, category, price, quantity);
        productList.put(id, p);
        return p;
    }

    public synchronized Product getById(int id) {
        return productList.get(id);
    }

    public synchronized boolean edit(int id, double price, int qty) {
        Product p = productList.get(id);
        if(p == null) {
            return false;
        }
        p.setPrice(price);
        p.setQuantity(qty);
        return true;
    }

    public synchronized boolean delete(int id) {
        return productList.remove(id) != null;
    }

    public Page<Product> search(String name, String category, 
                                Double minPrice, Double maxPrice, 
                                Integer minQty, Integer maxQty,
                                int page, int pageSize) {
        List<Product> filtered = productList.values().stream()
            // динамічні фільтри
            .filter(p -> name == null || p.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(p -> category == null || p.getCategory().equalsIgnoreCase(category))
            .filter(p -> minPrice == null || p.getPrice() >= minPrice)
            .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
            .filter(p -> minQty == null || p.getQuantity() >= minQty)
            .filter(p -> maxQty == null || p.getQuantity() <= maxQty)
            .toList();

        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        
        List<Product> items = filtered.stream()
        // пагінація
        .skip((long) (page - 1) * pageSize)
        .limit(pageSize)
        .toList();

        return new Page<>(items, totalItems, totalPages, page);
}
}
