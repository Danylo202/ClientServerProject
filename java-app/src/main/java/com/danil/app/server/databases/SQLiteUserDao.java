package com.danil.app.server.databases;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.Optional;

public class SQLiteUserDao {
    private final Connection connection;

    public SQLiteUserDao(String dbName) {
        try {
            this.connection = DriverManager.getConnection(dbName);
            init();
            seedDefaults();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося підключитися до SQLite users", e);
        }
    }

    private void init() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('admin', 'employee'))
                )
            """);
        }
    }

    private void seedDefaults() {
        addIfMissing("admin", "admin123", "admin");
        addIfMissing("manager", "manager123", "admin");
        addIfMissing("user", "123", "admin");
        addIfMissing("employee", "emp123", "employee");
        addIfMissing("worker", "worker123", "employee");
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        String sql = "SELECT username, password_hash, role FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String expectedHash = rs.getString("password_hash");
                if (!expectedHash.equals(hashPassword(password))) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(rs.getString("username"), rs.getString("role")));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося перевірити користувача", e);
        }
    }

    public void addIfMissing(String username, String password, String role) {
        String sql = "INSERT OR IGNORE INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ps.setString(3, role);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException("Не вдалося додати користувача '" + username + "'", e);
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (Exception e) {
            throw new RuntimeException("Не вдалося захешувати пароль", e);
        }
    }

    public record UserAccount(String username, String role) {
    }
}
