package com.danil.app;

import com.danil.app.domain.Product;
import com.danil.app.server.databases.SQLiteDb;
import com.danil.app.server.HTTP.homework.LoginHandler;
import com.danil.app.server.HTTP.homework.ProductHandler;
import com.danil.app.server.HTTP.homework.JWTAuthentificator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Lab5Test {
    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private String token;
    private final String URL = "http://localhost:8282";

    @BeforeAll
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(8282), 0);
        SQLiteDb db = new SQLiteDb("jdbc:sqlite:test_rest.db");
        db.addGroup("Electronics");

        server.createContext("/login", new LoginHandler());
        var productContext = server.createContext("/products", new ProductHandler(db));
        productContext.setAuthenticator(new JWTAuthentificator());

        server.start();
        System.out.println("Тестовий сервер запущено.");
    }

    @AfterAll
    void stopServer() {
        server.stop(0);
        new java.io.File("test_rest.db").delete();
        System.out.println("Тестовий сервер зупинено.");
    }

    @Test
    @Order(1)
    @DisplayName("1. POST /login")
    void testLogin() throws Exception {
        String json = "{\"username\":\"user\", \"password\":\"123\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, String> res = mapper.readValue(response.body(), Map.class);
        token = res.get("token");
        assertThat(token).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. PUT /products")
    void testCreate() throws Exception {
        Product p = new Product(0, "Phone", "Electronics", 500.0, 10);
        String json = mapper.writeValueAsString(p);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Created");
    }

    @Test
    @Order(3)
    @DisplayName("3. GET /products/{id}")
    void testRead() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products/1"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Phone");
    }

    @Test
    @Order(4)
    @DisplayName("4. POST /products/{id}")
    void testUpdate() throws Exception {
        // змінюємо ціну на 600
        Product p = new Product(1, "Phone", "Electronics", 600.0, 10);
        String json = mapper.writeValueAsString(p);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products/1"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        
        String checkBody = client.send(HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products/1"))
                .header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        assertThat(checkBody).contains("600.0");
    }

    @Test
    @Order(5)
    @DisplayName("5. DELETE /products/{id}")
    void testDelete() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products/1"))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        HttpResponse<String> check = client.send(HttpRequest.newBuilder()
                .uri(URI.create(URL + "/products/1"))
                .header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(check.statusCode()).isEqualTo(404);
    }
}
