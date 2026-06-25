package com.danil.app.server.HTTP.homework;

import com.danil.app.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import java.io.*;

public class LoginHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JWTService jwtService = new JWTService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        LoginDTO login = mapper.readValue(exchange.getRequestBody(), LoginDTO.class);

        if ("user".equals(login.getUsername()) && "123".equals(login.getPassword())) {
            String token = jwtService.createJwt(login.getUsername());
            byte[] response = mapper.writeValueAsBytes(new TokenResponse(token));

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
        }
        else {
            exchange.sendResponseHeaders(401, -1);
        }
    }
}
