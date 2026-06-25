package com.danil.app.server.HTTP.homework;

import com.danil.app.server.databases.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CategoryHandler implements HttpHandler {
    private final TCPCommandGateway gateway = new TCPCommandGateway();
    private final ObjectMapper mapper = new ObjectMapper();

    public CategoryHandler(Db db) {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        int statusCode = 200;
        Object responseBody;

        try {
            if ("GET".equals(method)) {
                String[] parts = path.split("/");
                if (parts.length >= 4 && "count".equals(parts[3])) {
                    String name = decode(parts[2]);
                    responseBody = Map.of("name", name, "productCount", gateway.categoryProductCount(name));
                }
                else if (parts.length >= 4 && "average-price".equals(parts[3])) {
                    String name = decode(parts[2]);
                    responseBody = Map.of("name", name, "averagePrice", gateway.categoryAveragePrice(name));
                }
                else {
                    responseBody = gateway.listCategories();
                }
            }
            else if ("PUT".equals(method)) {
                requireAdmin(exchange);
                CategoryRequest request = mapper.readValue(exchange.getRequestBody(), CategoryRequest.class);
                gateway.createCategory(request.name());
                responseBody = Map.of("message", "Created");
            }
            else if ("POST".equals(method)) {
                requireAdmin(exchange);
                RenameCategoryRequest request = mapper.readValue(exchange.getRequestBody(), RenameCategoryRequest.class);
                gateway.renameCategory(request.oldName(), request.newName());
                responseBody = Map.of("message", "Updated");
            }
            else if ("DELETE".equals(method)) {
                requireAdmin(exchange);
                String name = categoryNameFromPath(path);
                if (name == null) {
                    statusCode = 400;
                    responseBody = Map.of("error", "Category name is required");
                }
                else {
                    gateway.deleteCategory(name);
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

    private String categoryNameFromPath(String path) {
        if (path == null || "/categories".equals(path) || "/categories/".equals(path)) {
            return null;
        }
        String[] parts = path.split("/");
        return parts.length >= 3 ? decode(parts[2]) : null;
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record CategoryRequest(String name) {
    }

    private record RenameCategoryRequest(String oldName, String newName) {
    }
}
