package com.danil.app.server.HTTP.homework;

import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;

import com.sun.net.httpserver.HttpContext;
import java.net.*;
import java.io.IOException;
import com.sun.net.httpserver.HttpServer;

public class Server {
    public static void main(String[] args) throws IOException {
        Db db = new SQLiteDb("jdbc:sqlite:mystore.db");
        HttpServer server = HttpServer.create(new InetSocketAddress(8181), 0);

        server.createContext("/login", new LoginHandler());
        HttpContext productContext = server.createContext("/products", new ProductHandler(db));
        productContext.setAuthenticator(new JWTAuthentificator());

        server.start();
        System.out.println("[HTTP Server] Запущено на порту 8181");
    }
}
