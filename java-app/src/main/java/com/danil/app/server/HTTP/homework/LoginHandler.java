package com.danil.app.server.HTTP.homework;

import com.danil.app.domain.*;
import com.danil.app.server.databases.SQLiteUserDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import java.io.*;

public class LoginHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JWTService jwtService = new JWTService();
    private final SQLiteUserDao userDao;

    public LoginHandler() {
        this(new SQLiteUserDao("jdbc:sqlite:mystore.db"));
    }

    public LoginHandler(SQLiteUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        LoginDTO login = mapper.readValue(exchange.getRequestBody(), LoginDTO.class);

        var account = userDao.authenticate(login.getUsername(), login.getPassword());
        if (account.isPresent()) {
            String token = jwtService.createJwt(account.get().username(), account.get().role());
            byte[] response = mapper.writeValueAsBytes(
                    new TokenResponse(token, account.get().username(), account.get().role())
            );

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
        }
        else {
            exchange.sendResponseHeaders(401, -1);
        }
    }
}
