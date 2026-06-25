package com.danil.app.server.HTTP.practice;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class CustomHTTPServer {
    private final HttpServer server;

    public CustomHTTPServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        createEndpoints();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void createEndpoints() {
        // /users/**
        HttpContext context = server.createContext("/users/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.printf("Request to %s %s%n", exchange.getRequestMethod(), exchange.getRequestURI());
                System.out.println("Headers:");
                exchange.getRequestHeaders().forEach((header, value) -> System.out.printf("  %s: %s%n", header, value));

                String userId = exchange.getRequestURI().toString().split("\\?")[0]
                    .replace("/users/", "");

                String responseBody = """
                    {
                      "id": %s,
                      "firstName": "John",
                      "lastName": "Smith"
                    }
                    """.formatted(userId);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody.getBytes());
                }
            }
        });

        // all other endpoints including handling for 'not found' path
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(404, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    // write nothing
                }
            }
        });
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CustomHTTPServer server = new CustomHTTPServer(8181);
        server.start();

        Thread.sleep(5_000);

        server.stop();
    }
}
