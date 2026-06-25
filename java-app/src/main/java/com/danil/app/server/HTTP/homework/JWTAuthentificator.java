package com.danil.app.server.HTTP.homework;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class JWTAuthentificator extends Authenticator {
    private final JWTService jwtService = new JWTService();

    @Override
    public Result authenticate(HttpExchange exch) {
        String authHeader = exch.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new Authenticator.Retry(401);
        }

        String token = authHeader.substring(7);
        String username = jwtService.decodeJwt(token);
        String role = jwtService.decodeRole(token);

        if (username == null || role == null) {
            return new Authenticator.Failure(403); 
        }

        return new Authenticator.Success(new HttpPrincipal(username, role));
    }
}
