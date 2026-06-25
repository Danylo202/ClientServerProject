package com.danil.app.server.HTTP.homework;

import com.danil.app.server.NetworkConnection.StoreServerTCP;
import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;
import com.danil.app.server.databases.SQLiteUserDao;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class Server {
    public static final int PORT = 8181;
    private static final String DB_NAME = "jdbc:sqlite:mystore.db";

    public static HttpServer create(Db db, SQLiteUserDao userDao) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new LoginPageHandler());
        server.createContext("/app/login", new LoginPageHandler());
        server.createContext("/app", new LoginPageHandler());
        server.createContext("/login", new LoginHandler(userDao));
        HttpContext productContext = server.createContext("/products", new ProductHandler(db));
        productContext.setAuthenticator(new JWTAuthentificator());
        HttpContext categoryContext = server.createContext("/categories", new CategoryHandler(db));
        categoryContext.setAuthenticator(new JWTAuthentificator());
        return server;
    }

    public static void main(String[] args) throws IOException {
        Db db = new SQLiteDb(DB_NAME);
        SQLiteUserDao userDao = new SQLiteUserDao(DB_NAME);
        try {
            StoreServerTCP.startInBackground(db);
        } catch (BindException e) {
            System.err.println("[TCP Server] Порт " + StoreServerTCP.PORT + " вже зайнятий. Закрийте старий TCP-сервер.");
            return;
        }

        HttpServer server;
        try {
            server = create(db, userDao);
        } catch (BindException e) {
            String message = "Порт " + PORT + " вже зайнятий. Закрийте попередньо запущений HTTP-сервер.";
            System.err.println("[HTTP Server] " + message);
            return;
        }
        server.start();
        System.out.println("[HTTP Server] Запущено на порту " + PORT);
        System.out.println("[TCP Server] Запущено на порту " + StoreServerTCP.PORT + " (gateway для HTTP UI)");
        System.out.println("[HTTP Server] Відкрийте http://localhost:" + PORT + "/app/login у браузері");
    }
}
