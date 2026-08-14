package com.melodyHub.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shared helpers for opaque security tokens (refresh tokens, password-reset
 * tokens): SHA-256 hex hashing for at-rest storage and URL-safe random token
 * generation. Centralized so callers never re-implement the digest loop.
 */
public final class TokenHashUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtil() {
    }

    /** Returns the lowercase SHA-256 hex digest of {@code token}. */
    public static String sha256Hex(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** Generates a URL-safe, unpadded Base64 token from {@code byteLength} random bytes. */
    public static String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
