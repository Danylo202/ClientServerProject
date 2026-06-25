package com.danil.app.server.databases;

import com.danil.app.domain.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteDb implements Db {
    private final Connection connection;

    public SQLiteDb(String dbName) {
        try {
            this.connection = DriverManager.getConnection(dbName);
            init();
        } catch (SQLException e) {
            throw new RuntimeException("Не вдалося підключитися до SQLite", e);
        }
    }

    private void init() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");

            // окрема таблиця для груп
            statement.execute("CREATE TABLE IF NOT EXISTS groups (name TEXT PRIMARY KEY)");

            statement.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    price REAL,
                    quantity INTEGER,
                    FOREIGN KEY (category) REFERENCES groups(name) 
                    ON UPDATE CASCADE ON DELETE RESTRICT
                )
            """);
        }
        catch (SQLException e) {
            throw new RuntimeException("Exception while DB init", e);
        }
    }

    @Override
    public boolean withdrawProduct(String name, int amount) {
    String sql = """
        UPDATE products SET quantity = quantity - ?
        WHERE name = ? AND quantity >= ?
    """;
    
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, amount);
        ps.setString(2, name);
        ps.setInt(3, amount);

        int rows = ps.executeUpdate();

        return (rows > 0);
    }
    catch (SQLException e) {
        throw new RuntimeException("Помилка при списанні товару '" + name + "'", e);
    }
}

    @Override
    public void addProductQuantity(String name, int amount) {
        String sql = """
            UPDATE products SET quantity = quantity + ?
            WHERE name = ?
        """;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, name);
            
            int rows = ps.executeUpdate();
            
            if (rows == 0) {
                System.err.println("[DB] Помилка: товар '" + name + "' не знайдено.");
            }
            else {
                System.out.println("[DB] Додано " + amount + " до товару " + name);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні кількості", e);
        }
}

    @Override
    public void setPrice(String name, double price) {
        String sql = """
            UPDATE products SET price = ?
            WHERE name = ?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setString(2, name);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося змінити ціну товару '" + name + "'", e);
        }
    }

    @Override
    public void addGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new RuntimeException("Назва категорії не може бути порожньою");
        }
        String sql = "INSERT OR IGNORE INTO groups (name) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName.trim());
            ps.executeUpdate();
            System.out.println("[DB] Група створена: " + groupName);
        }
        catch (SQLException e) {
            throw new RuntimeException("Помилка створення групи", e);
        }
    }

    @Override
    public void addProductToGroup(String productName, String groupName) {
        ensureGroupExists(groupName);
        // запит спрацює тільки якщо groupName існує в таблиці groups
        String sql = "UPDATE products SET category = ? WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ps.setString(2, productName);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.err.println("[DB] Товар не знайдено: " + productName);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося додати товар до групи '" + groupName + "'", e);
        }
    }
    
    @Override
    public int create(Product product) {
        ensureGroupExists(product.getCategory());
        String sql = "INSERT OR IGNORE INTO products (name, category, price, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getQuantity());

            int i = ps.executeUpdate();
            if (i < 1) {
                throw new RuntimeException("Не вдалося додати товар" + product.getName());
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                else {
                    throw new SQLException("Помилка: база не повернула ID!");
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося додати товар: " + product.getName(), e);
        }
    }

    @Override
    public Optional<Product> getById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Product(rs.getInt("id"), rs.getString("name"), rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity")));
                }
            }

            return Optional.empty();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося знайти товар з id " + id, e);
        }
    }

    @Override
    public Optional<Product> get(String name) {
        String sql = """
            SELECT * FROM products
            WHERE LOWER(name) LIKE LOWER(?)
            ORDER BY (CASE WHEN LOWER(name) = LOWER(?) THEN 0 ELSE 1 END), id ASC
            LIMIT 1
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name == null ? "%" : "%" + name + "%");
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Product(rs.getInt("id"), rs.getString("name"), rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity")));
                }
            }

            return Optional.empty();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося знайти товар '" + name + "'", e);
        }
    }

    @Override
    public void edit(Product product) {
        ensureGroupExists(product.getCategory());
        String sql = "UPDATE products SET name=?, category=?, price=?, quantity=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getQuantity());
            ps.setInt(5, product.getId());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося відредагувати товар з id " + product.getId(), e);
        }
    }

    @Override
    public int getCount() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM products")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося порахувати товари", e);
        }
    }

    @Override
    public int getCount(String name) {
        try (PreparedStatement ps = connection.prepareStatement("""
            SELECT quantity FROM products
            WHERE LOWER(name) LIKE LOWER(?)
            """))
        {
            ps.setString(1, name == null ? "%" : "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity"); 
                }
            }
            return 0;
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося порахувати товари", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Не вдалося видалити товар з id " + id, e);
        }
    }

    @Override
    public void delete(String name) {
        String sql = """
            DELETE FROM products
            WHERE name = ?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Не вдалося видалити товар '" + name + "'", e);
        }
    }

    // пошук (динамічні фільтри + пагінація)
    @Override
    public List<Product> search(String name, String category, Double minP, Double maxP, int page, int pageSize) {
        List<Product> list = new ArrayList<>();
        // ESCAPE потрібен, щоб символи '_' і '%' у введенні шукались як звичайний текст.
        String sql = """
                    SELECT * FROM products WHERE
                    LOWER(name) LIKE LOWER(?) ESCAPE '!' AND
                    LOWER(COALESCE(category, '')) LIKE LOWER(?) ESCAPE '!' AND
                    price >= ? AND
                    price <= ?
                    ORDER BY id ASC
                    LIMIT ? OFFSET ?
                """;
                     
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, likePattern(name));
            ps.setString(2, likePattern(category));
            ps.setDouble(3, minP == null ? 0 : minP);
            ps.setDouble(4, maxP == null ? Double.MAX_VALUE : maxP);
            ps.setInt(5, pageSize); // LIMIT - скільки рядків вивести
            ps.setInt(6, (page - 1) * pageSize); // OFFSET - скільки рядків відступити

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Product(rs.getInt("id"), rs.getString("name"), 
                         rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity")));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося знайти такий товар", e);
        }
        return list;
    }

    @Override
    public List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        String sql = "SELECT name FROM groups ORDER BY name ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(rs.getString("name"));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося отримати список категорій", e);
        }
        return groups;
    }

    @Override
    public void deleteGroup(String groupName) {
        ensureGroupExists(groupName);
        int productCount = getCategoryProductCount(groupName);
        if (productCount > 0) {
            throw new RuntimeException("Категорію '" + groupName + "' не можна видалити: у ній є товари (" + productCount + ")");
        }

        String sql = "DELETE FROM groups WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося видалити категорію '" + groupName + "'", e);
        }
    }

    @Override
    public void renameGroup(String oldName, String newName) {
        ensureGroupExists(oldName);
        if (newName == null || newName.isBlank()) {
            throw new RuntimeException("Нова назва категорії не може бути порожньою");
        }

        String sql = "UPDATE groups SET name = ? WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setString(2, oldName);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося перейменувати категорію '" + oldName + "'", e);
        }
    }

    @Override
    public int getCategoryProductCount(String groupName) {
        String sql = "SELECT COUNT(*) FROM products WHERE category = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося порахувати товари категорії '" + groupName + "'", e);
        }
    }

    @Override
    public double getCategoryAveragePrice(String groupName) {
        String sql = "SELECT COALESCE(AVG(price), 0) FROM products WHERE category = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося порахувати середню ціну категорії '" + groupName + "'", e);
        }
    }

    private static String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return "%";
        }
        return "%" + escapeLike(value.trim()) + "%";
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private void ensureGroupExists(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new RuntimeException("Категорія товару не може бути порожньою");
        }

        String sql = "SELECT 1 FROM groups WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, groupName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Категорії '" + groupName + "' не існує. Спочатку створіть її у таблиці категорій.");
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося перевірити категорію '" + groupName + "'", e);
        }
    }
}