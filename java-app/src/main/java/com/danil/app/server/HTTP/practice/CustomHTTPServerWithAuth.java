package com.danil.app.server.HTTP.practice;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;


public class CustomHTTPServerWithAuth {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8282), 0);

        // /users/**
        HttpContext basicContext = server.createContext("/basic/", exchange -> {
            String responseBody = """
                {
                  "id": 1,
                  "firstName": "John",
                  "lastName": "Smith"
                }
                """;

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBody.getBytes());
            }
        });
        basicContext.setAuthenticator(new BasicAuthenticator());

        server.start();
    }

    private static class BasicAuthenticator extends Authenticator {
        @Override
        public Result authenticate(HttpExchange exch) {
            List<String> values = exch.getRequestHeaders().get("Authorization");
            // header doesn't exists
            if (values == null || values.isEmpty()) {
                return new Failure(401);
            }

            // wrong auth type
            String[] credentialParts = values.getFirst().split(" ");
            if (credentialParts.length != 2 || !credentialParts[0].equals("Basic")) {
                return new Failure(401);
            }

            String credential = new String(Base64.getDecoder().decode(credentialParts[1]));
            String[] userNameAndPassword = credential.split(":");

            // incorrect basic auth format
            if (userNameAndPassword.length != 2) {
                return new Failure(401);
            }

            if (userNameAndPassword[0].equals("username") && userNameAndPassword[1].equals("password")) {
                return new Success(new HttpPrincipal(userNameAndPassword[0], "ROLE_ADMIN"));
            }

            // correct format, but wrong user for this endpoint
            return new Failure(403);
        }
    }
}
