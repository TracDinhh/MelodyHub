package com.melodyHub.service.auth;

import com.melodyHub.config.AppConfig;
import com.melodyHub.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public class RefreshTokenService {
    private static final String REFRESH_TOKEN_EXPIRES_DAYS = "refresh-token.expires-days";
    private static final int DEFAULT_EXPIRES_DAYS = 7;
    private static final int TOKEN_BYTES = 64;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService() {
        this(new RefreshTokenRepository());
    }

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = Objects.requireNonNull(
                refreshTokenRepository,
                "refreshTokenRepository must not be null"
        );
    }

    public IssuedRefreshToken issue(int userId) throws SQLException {
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(getExpiresInDays());
        refreshTokenRepository.create(userId, hash(token), expiresAt);
        return new IssuedRefreshToken(token, getExpiresInSeconds());
    }

    public Optional<Integer> findActiveUserId(String token) throws SQLException {
        String normalizedToken = normalize(token);
        if (normalizedToken == null) {
            return Optional.empty();
        }

        return refreshTokenRepository.findActiveUserIdByTokenHash(hash(normalizedToken), LocalDateTime.now());
    }

    public void revoke(String token) throws SQLException {
        String normalizedToken = normalize(token);
        if (normalizedToken == null) {
            return;
        }

        refreshTokenRepository.revokeByTokenHash(hash(normalizedToken), LocalDateTime.now());
    }

    public long getExpiresInSeconds() {
        return getExpiresInDays() * 24L * 60L * 60L;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hash.append(String.format("%02x", value));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private int getExpiresInDays() {
        return AppConfig.getInt(REFRESH_TOKEN_EXPIRES_DAYS, DEFAULT_EXPIRES_DAYS);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record IssuedRefreshToken(String token, long expiresInSeconds) {
    }
}
