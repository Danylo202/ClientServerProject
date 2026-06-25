package com.danil.app.server.HTTP.homework;

import com.danil.app.domain.Product;
import com.danil.app.server.databases.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProductHandler implements HttpHandler {
    private final TCPCommandGateway gateway = new TCPCommandGateway();
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductHandler(Db db) {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        int statusCode = 200;
        Object responseBody;

        try {
            if ("GET".equals(method)) {
                Integer id = pathId(path);
                if (id == null) {
                    Map<String, String> query = queryParams(exchange.getRequestURI().getRawQuery());
                    responseBody = gateway.searchProducts(
                            emptyToNull(query.get("name")),
                            emptyToNull(query.get("category")),
                            parseDoubleOrNull(query.get("minPrice")),
                            parseDoubleOrNull(query.get("maxPrice")),
                            parseIntOrDefault(query.get("page"), 1),
                            parseIntOrDefault(query.get("size"), 500)
                    );
                }
                else {
                    Product p = gateway.getProductById(id);

                    if (p != null) {
                        responseBody = p;
                    }
                    else {
                        statusCode = 404;
                        responseBody = Map.of("error", "Product not found");
                    }
                }
            }
            else if ("PUT".equals(method)) {
                requireAdmin(exchange);
                Product p = mapper.readValue(exchange.getRequestBody(), Product.class);
                int newId = gateway.createProduct(p);
                responseBody = Map.of("message", "Created", "id", newId);

            }
            else if ("POST".equals(method)) {
                requireAdmin(exchange);
                Product p = mapper.readValue(exchange.getRequestBody(), Product.class);
                Integer id = pathId(path);
                if (id != null) {
                    p.setId(id);
                }
                gateway.editProduct(p);
                responseBody = Map.of("message", "Updated");

            }
            else if ("DELETE".equals(method)) {
                requireAdmin(exchange);
                Integer id = pathId(path);
                if (id == null) {
                    statusCode = 400;
                    responseBody = Map.of("error", "Product ID is required");
                }
                else {
                    gateway.deleteProduct(id);
                    responseBody = Map.of("message", "Deleted");
                }

            }
            else {
                statusCode = 405;
                responseBody = Map.of("error", "Method not allowed");
            }
        }
        catch (SecurityException e) {
            statusCode = 403;
            responseBody = Map.of("error", e.getMessage());
        }
        catch (Exception e) {
            statusCode = 500;
            responseBody = Map.of("error", e.getMessage());
        }

        byte[] responseBytes = mapper.writeValueAsString(responseBody).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void requireAdmin(HttpExchange exchange) {
        String role = exchange.getPrincipal() == null ? "" : exchange.getPrincipal().getRealm();
        if (!"admin".equals(role)) {
            throw new SecurityException("Недостатньо прав: ця дія доступна тільки адміністратору");
        }
    }

    private Integer pathId(String path) {
        if (path == null || "/products".equals(path) || "/products/".equals(path)) {
            return null;
        }
        String value = path.substring(path.lastIndexOf("/") + 1);
        return Integer.parseInt(value);
    }

    private Map<String, String> queryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Double.parseDouble(s);
    }

    private static int parseIntOrDefault(String s, int defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(s);
    }
}