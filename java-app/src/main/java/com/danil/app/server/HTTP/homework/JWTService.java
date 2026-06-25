package com.danil.app.server.HTTP.homework;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.util.Date;

public class JWTService {
    private static final String SECRET = "my-super-secret-key";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    public String createJwt(String username) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .withClaim("processId", 64)
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
}