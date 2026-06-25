package com.danil.app.server.HTTP.homework;

import com.danil.app.domain.Product;
import com.danil.app.server.databases.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProductHandler implements HttpHandler {
    private final Db db;
    private final ObjectMapper mapper = new ObjectMapper(); // Замість Gson

    public ProductHandler(Db db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String responseBody = "";
        int statusCode = 200;

        try {
            if ("GET".equals(method)) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                Product p = db.getById(id).orElse(null);
                
                if (p != null) {
                    responseBody = mapper.writeValueAsString(p);
                }
                else {
                    statusCode = 404;
                    responseBody = "{\"error\": \"Product not found\"}";
                }

            }
            else if ("PUT".equals(method)) {
                Product p = mapper.readValue(exchange.getRequestBody(), Product.class);
                int newId = db.create(p);
                responseBody = "{\"message\": \"Created\", \"id\": " + newId + "}";

            }
            else if ("POST".equals(method)) {
                Product p = mapper.readValue(exchange.getRequestBody(), Product.class);
                db.edit(p);
                responseBody = "{\"message\": \"Updated\"}";

            }
            else if ("DELETE".equals(method)) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                db.delete(id);
                responseBody = "{\"message\": \"Deleted\"}";

            }
            else {
                statusCode = 405;
                responseBody = "{\"error\": \"Method not allowed\"}";
            }
        }
        catch (Exception e) {
            statusCode = 500;
            responseBody = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}