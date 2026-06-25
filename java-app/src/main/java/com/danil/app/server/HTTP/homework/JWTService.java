package com.danil.app.server.HTTP.homework;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;

public class JWTService {
    private static final String SECRET = "my-super-secret-key";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    public String createJwt(String username) {
        return createJwt(username, "employee");
    }

    public String createJwt(String username, String role) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .withClaim("processId", 64)
                .withClaim("role", role)
                .sign(algorithm);
    }

    public String decodeJwt(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withClaim("processId", 64)
                    .build();
            return verifier.verify(token).getSubject();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String decodeRole(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withClaim("processId", 64)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            String role = jwt.getClaim("role").asString();
            return role == null ? "employee" : role;
        }
        catch (Exception e) {
            return null;
        }
    }
}