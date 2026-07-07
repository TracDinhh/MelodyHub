package com.melodyHub.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.melodyHub.config.AppConfig;
import com.melodyHub.entity.User;
import java.time.Instant;

public final class JwtUtil {
    private static final String JWT_SECRET = "jwt.secret";
    private static final String JWT_EXPIRES_MINUTES = "jwt.expires-minutes";
    private static final String ISSUER = "melodyhub";
    private static final int DEFAULT_EXPIRES_MINUTES = 1440;

    private JwtUtil() {
    }

    public static String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(getExpiresInSeconds());

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(user.getId()))
                .withClaim("userId", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(getAlgorithm());
    }

    public static DecodedJWT verifyToken(String token) {
        return getVerifier().verify(token);
    }

    public static int getUserIdFromToken(String token) {
        return verifyToken(token).getClaim("userId").asInt();
    }

    public static long getExpiresInSeconds() {
        return AppConfig.getInt(JWT_EXPIRES_MINUTES, DEFAULT_EXPIRES_MINUTES) * 60L;
    }

    private static JWTVerifier getVerifier() {
        return JWT.require(getAlgorithm())
                .withIssuer(ISSUER)
                .build();
    }

    private static Algorithm getAlgorithm() {
        return Algorithm.HMAC256(AppConfig.getRequired(JWT_SECRET));
    }
}
